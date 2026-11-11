package pe.edu.upeu.pharmamobile.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile.data.model.ProductoRequestDto
import pe.edu.upeu.pharmamobile.data.network.ClasificadorErrorSincronizacion
import pe.edu.upeu.pharmamobile.data.network.ProductoApi
import pe.edu.upeu.pharmamobile.data.network.safeApiCall
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.tiempoActualEpochMillis
import pe.edu.upeu.pharmamobile.tiempoActualEpochSegundos

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoApi,
    private val tiempoMillisProvider: () -> Long = { tiempoActualEpochMillis() },
    private val tiempoSegundosProvider: () -> Long = { tiempoActualEpochSegundos() },
    private val esperaProvider: suspend (Long) -> Unit = { delay(it) },
    private val onSesionExpirada: (() -> Unit)? = null
) : ProductoRepository {

    private val mutexSincronizacion = Mutex()

    override fun observar(): Flow<List<Producto>> = local.observar()

    override fun observarContadores(): Flow<Pair<Long, Long>> = local.observarContadores()

    override suspend fun registrar(p: Producto): Producto {
        val guardado = local.guardarPendiente(p)
        intentar { sincronizar() }
        return guardado
    }

    override suspend fun sincronizar() {
        mutexSincronizacion.withLock {
            subirPendientes()
            descargarYActualizarCatalogo()
            local.guardarUltimaSincronizacion(tiempoSegundosProvider())
        }
    }

    override fun obtenerUltimaSincronizacion(): Long? = local.obtenerUltimaSincronizacion()

    override suspend fun descargarCatalogo() {
        mutexSincronizacion.withLock {
            descargarYActualizarCatalogo()
        }
    }

    override suspend fun reintentarFila(id: Long) {
        local.rearmarReintentoManual(id)
        intentar { sincronizar() }
    }

    override suspend fun reintentarTodasLasFallidas() {
        val fallidas = local.fallidas()
        fallidas.forEach { local.rearmarReintentoManual(it.id) }
        intentar { sincronizar() }
    }

    private suspend fun subirPendientes() {
        val ahora = tiempoMillisProvider()
        val pendientes = local.pendientesParaSincronizacion(ahora)

        for (pendiente in pendientes) {
            val request = ProductoRequestDto(
                nombre = pendiente.nombre,
                precio = pendiente.precio,
                stock = pendiente.stock,
                estado = pendiente.activo,
                categoriaId = pendiente.categoriaId ?: 1L
            )

            try {
                val creado = safeApiCall { remoto.crear(request) }
                val confirmado = Producto(
                    id = creado.id,
                    nombre = creado.nombre,
                    precio = creado.precio,
                    stock = creado.stock,
                    activo = creado.estado,
                    descripcion = pendiente.descripcion,
                    imagen = pendiente.imagen,
                    categoria = creado.categoriaNombre ?: pendiente.categoria,
                    categoriaId = creado.categoriaId ?: pendiente.categoriaId,
                    pendiente = false,
                    intentos = 0,
                    ultimoError = null,
                    falloPermanente = false,
                    proximoReintentoEpochMillis = 0L,
                    claveIdempotencia = null
                )
                local.confirmarSubida(pendiente.id, confirmado)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                val errorApi = if (e is ErrorApi) e else ErrorApi.Desconocido(e.message ?: "Error desconocido")
                val resultado = ClasificadorErrorSincronizacion.clasificar(
                    error = errorApi,
                    intentosPrevios = pendiente.intentos,
                    ahoraMillis = tiempoMillisProvider()
                )

                when (resultado) {
                    is ResultadoEnvio.Permanente -> {
                        local.registrarFalloPermanente(pendiente.id, resultado.motivo)
                        if (resultado.codigoHttp == 401) {
                            onSesionExpirada?.invoke()
                            return // Detiene subida ante 401 para resolver sesión sin reintentos ciegos
                        }
                    }
                    is ResultadoEnvio.Transitorio -> {
                        local.registrarFalloTransitorio(
                            id = pendiente.id,
                            error = resultado.motivo,
                            proximoReintentoEpochMillis = resultado.reintentoEnMillis
                        )
                        val delayFila = ClasificadorErrorSincronizacion.calcularEsperaMillis(pendiente.intentos + 1)
                        esperaProvider(delayFila)
                    }
                    is ResultadoEnvio.Exito -> {
                        // Inalcanzable en rama de captura de error
                    }
                }
            }
        }
    }

    private suspend fun descargarYActualizarCatalogo() {
        val todosLosProductosRemotos = mutableListOf<Producto>()
        var pagina = 0
        var esUltima = false

        while (!esUltima) {
            val respuesta = safeApiCall { remoto.listar(pagina = pagina, tamanio = 50) }
            val productosPagina = respuesta.contenido.filter { it.estado }.map { dto ->
                Producto(
                    id = dto.id,
                    nombre = dto.nombre,
                    precio = dto.precio,
                    stock = dto.stock,
                    activo = dto.estado,
                    descripcion = "",
                    imagen = "",
                    categoria = dto.categoriaNombre ?: "",
                    categoriaId = dto.categoriaId ?: 1L,
                    pendiente = false,
                    intentos = 0,
                    ultimoError = null,
                    falloPermanente = false,
                    proximoReintentoEpochMillis = 0L,
                    claveIdempotencia = null
                )
            }
            todosLosProductosRemotos.addAll(productosPagina)
            esUltima = respuesta.ultima || respuesta.pagina >= (respuesta.totalPaginas - 1).coerceAtLeast(0)
            pagina++
        }

        local.reemplazarSincronizados(todosLosProductosRemotos)
    }

    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorApi.SinConexion) {
            // Sin conexión: queda pendiente en local y se subirá al reconectar
        } catch (e: ErrorApi.TiempoAgotado) {
            // Timeout de red: queda pendiente
        } catch (e: ErrorApi.Servidor) {
            // Error de servidor: queda pendiente con incremento de intentos
        }
    }

    override suspend fun listar(): List<Producto> {
        return local.observar().first()
    }

    override suspend fun obtener(id: Long): Producto {
        return local.obtenerPorId(id) ?: throw ErrorApi.NoEncontrado("Producto con ID $id no encontrado")
    }

    override suspend fun actualizar(p: Producto): Producto {
        if (p.id < 0) {
            local.actualizarLocal(p)
            return p
        }
        val request = ProductoRequestDto(
            nombre = p.nombre,
            precio = p.precio,
            stock = p.stock,
            estado = p.activo,
            categoriaId = p.categoriaId ?: 1L
        )
        val dto = safeApiCall { remoto.actualizar(p.id, request) }
        val actualizado = p.copy(
            id = dto.id,
            nombre = dto.nombre,
            precio = dto.precio,
            stock = dto.stock,
            activo = dto.estado,
            categoria = dto.categoriaNombre ?: p.categoria,
            categoriaId = dto.categoriaId ?: p.categoriaId,
            pendiente = false,
            intentos = 0,
            ultimoError = null,
            falloPermanente = false,
            proximoReintentoEpochMillis = 0L,
            claveIdempotencia = null
        )
        local.actualizarLocal(actualizado)
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        if (id > 0) {
            safeApiCall { remoto.eliminar(id) }
        }
        local.eliminarLocal(id)
    }
}
