package pe.edu.upeu.pharmamobile.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile.data.model.ProductoRequestDto
import pe.edu.upeu.pharmamobile.data.network.ProductoApi
import pe.edu.upeu.pharmamobile.data.network.safeApiCall
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoApi
) : ProductoRepository {

    private val mutexSincronizacion = Mutex()

    override fun observar(): Flow<List<Producto>> = local.observar()

    override suspend fun registrar(p: Producto): Producto {
        val guardado = local.guardarPendiente(p)
        intentar { sincronizar() }
        return guardado
    }

    override suspend fun sincronizar() {
        mutexSincronizacion.withLock {
            subirPendientes()
            descargarYActualizarCatalogo()
            local.guardarUltimaSincronizacion(pe.edu.upeu.pharmamobile.tiempoActualEpochSegundos())
        }
    }

    override fun obtenerUltimaSincronizacion(): Long? = local.obtenerUltimaSincronizacion()

    override suspend fun descargarCatalogo() {
        mutexSincronizacion.withLock {
            descargarYActualizarCatalogo()
        }
    }

    private suspend fun subirPendientes() {
        val pendientes = local.pendientes()
        for (pendiente in pendientes) {
            val request = ProductoRequestDto(
                nombre = pendiente.nombre,
                precio = pendiente.precio,
                stock = pendiente.stock,
                estado = pendiente.activo,
                categoriaId = pendiente.categoriaId ?: 1L
            )
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
                pendiente = false
            )
            local.confirmarSubida(pendiente.id, confirmado)
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
                    pendiente = false
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
            pendiente = false
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
