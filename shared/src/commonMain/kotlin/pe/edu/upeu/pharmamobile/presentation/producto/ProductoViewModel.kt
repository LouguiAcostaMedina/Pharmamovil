package pe.edu.upeu.pharmamobile.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.permission.EstadoPermiso
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.permission.Permiso
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.domain.platform.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria
import pe.edu.upeu.pharmamobile.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile.platform.formatearSoles

class ProductoViewModel(
    private val repositorio: ProductoRepository,
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase,
    private val eliminarProductoUseCase: EliminarProductoUseCase,
    private val obtenerProductoUseCase: ObtenerProductoUseCase,
    private val compartidor: Compartidor,
    private val notificador: NotificadorSincronizacion = object : NotificadorSincronizacion {
        override fun notificarResultado(titulo: String, mensaje: String) {}
    },
    private val telemetria: Telemetria = object : Telemetria {
        override fun registrarEvento(nombre: String, datos: Map<String, Any>) {}
        override fun registrarError(error: Throwable, contexto: Map<String, String>) {}
    },
    private val gestorPermisos: GestorPermisos = object : GestorPermisos {
        override suspend fun estado(permiso: Permiso): EstadoPermiso = EstadoPermiso.Concedido
        override suspend fun solicitar(permiso: Permiso): EstadoPermiso = EstadoPermiso.Concedido
        override fun abrirAjustes() {}
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    private var observacionIniciada = false
    private var tickerJob: Job? = null

    init {
        iniciarObservacion()
        iniciarObservacionContadores()
        actualizarMarcaSincronizacion()
        verificarPermisoNotificaciones()
    }

    fun verificarPermisoNotificaciones() {
        viewModelScope.launch {
            val estado = gestorPermisos.estado(Permiso.NOTIFICACIONES)
            _uiState.update { it.copy(estadoPermisoNotificaciones = estado) }
        }
    }

    fun solicitarPermisoNotificaciones(onResultado: (EstadoPermiso) -> Unit = {}) {
        viewModelScope.launch {
            val estado = gestorPermisos.solicitar(Permiso.NOTIFICACIONES)
            _uiState.update { it.copy(estadoPermisoNotificaciones = estado) }
            telemetria.registrarEvento(
                nombre = "permission_result",
                datos = mapOf(
                    "permiso" to Permiso.NOTIFICACIONES.name,
                    "resultado" to when (estado) {
                        is EstadoPermiso.Concedido -> "concedido"
                        is EstadoPermiso.Denegado -> "denegado"
                        is EstadoPermiso.DenegadoParaSiempre -> "denegado_permanente"
                    }
                )
            )
            onResultado(estado)
        }
    }

    fun abrirAjustesPermiso() {
        gestorPermisos.abrirAjustes()
    }

    fun actualizarMarcaSincronizacion() {
        val ts = repositorio.obtenerUltimaSincronizacion()
        _uiState.update {
            it.copy(
                ultimaSincronizacionTimestamp = ts,
                ultimaSincronizacion = AntiguedadSincronizacion.formatear(ts)
            )
        }
    }

    fun iniciarTickerAntiguedad() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                actualizarMarcaSincronizacion()
            }
        }
    }

    fun iniciarObservacion() {
        if (observacionIniciada) return
        observacionIniciada = true

        viewModelScope.launch {
            repositorio.observar().collect { productos ->
                _uiState.update { state ->
                    val fase = if (productos.isNotEmpty()) {
                        ProductoUiState.Fase.ConProductos(productos)
                    } else {
                        if (state.fase is ProductoUiState.Fase.Cargando || state.fase is ProductoUiState.Fase.Error) {
                            state.fase
                        } else {
                            ProductoUiState.Fase.SinProductos
                        }
                    }
                    state.copy(
                        productos = productos,
                        fase = fase
                    )
                }
            }
        }
    }

    private fun iniciarObservacionContadores() {
        viewModelScope.launch {
            repositorio.observarContadores().collect { (pendientes, fallidas) ->
                _uiState.update { state ->
                    state.copy(
                        cantidadPendientes = pendientes,
                        cantidadFallidas = fallidas
                    )
                }
            }
        }
    }

    fun cargarProductos() {
        iniciarObservacion()
        if (_uiState.value.productos.isEmpty()) {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
        }
        sincronizar()
    }

    fun sincronizar(modo: String = "manual") {
        viewModelScope.launch {
            val inicioMs = pe.edu.upeu.pharmamobile.tiempoActualEpochMillis()
            telemetria.registrarEvento("sync_start", mapOf("modo" to modo))
            _uiState.update { it.copy(sincronizando = true, avisoRed = null) }
            try {
                repositorio.sincronizar()
                val nuevoTimestamp = repositorio.obtenerUltimaSincronizacion()
                _uiState.update { state ->
                    val fase = if (state.productos.isEmpty()) {
                        ProductoUiState.Fase.SinProductos
                    } else {
                        ProductoUiState.Fase.ConProductos(state.productos)
                    }
                    state.copy(
                        sincronizando = false,
                        conectado = true,
                        avisoRed = null,
                        fase = fase,
                        ultimaSincronizacionTimestamp = nuevoTimestamp,
                        ultimaSincronizacion = AntiguedadSincronizacion.formatear(nuevoTimestamp)
                    )
                }
                notificador.notificarResultado(
                    titulo = "Catálogo Sincronizado",
                    mensaje = "Sincronización completada exitosamente con PharmaSoft."
                )
                val duracionMs = pe.edu.upeu.pharmamobile.tiempoActualEpochMillis() - inicioMs
                telemetria.registrarEvento(
                    nombre = "sync_success",
                    datos = mapOf(
                        "duracion_ms" to duracionMs,
                        "pendientes_restantes" to _uiState.value.cantidadPendientes,
                        "fallidas_restantes" to _uiState.value.cantidadFallidas
                    )
                )
            } catch (e: CancellationException) {
                _uiState.update { it.copy(sincronizando = false) }
                throw e
            } catch (e: ErrorApi.SinConexion) {
                _uiState.update { state ->
                    val fase = if (state.productos.isEmpty()) {
                        ProductoUiState.Fase.SinProductos
                    } else {
                        ProductoUiState.Fase.ConProductos(state.productos)
                    }
                    state.copy(
                        sincronizando = false,
                        conectado = false,
                        avisoRed = "Sin conexión: mostrando datos guardados localmente.",
                        fase = fase
                    )
                }
                notificador.notificarResultado(
                    titulo = "Modo Offline Activo",
                    mensaje = "Sin conexión a internet. Los datos se guardan localmente en SQLite."
                )
                telemetria.registrarEvento(
                    nombre = "sync_failure",
                    datos = mapOf(
                        "motivo" to "sin_conexion",
                        "transitorio" to true,
                        "intentos" to 1
                    )
                )
            } catch (e: ErrorApi.TiempoAgotado) {
                _uiState.update { state ->
                    val fase = if (state.productos.isEmpty()) {
                        ProductoUiState.Fase.SinProductos
                    } else {
                        ProductoUiState.Fase.ConProductos(state.productos)
                    }
                    state.copy(
                        sincronizando = false,
                        conectado = false,
                        avisoRed = "Tiempo de espera agotado: mostrando datos locales.",
                        fase = fase
                    )
                }
                telemetria.registrarEvento(
                    nombre = "sync_failure",
                    datos = mapOf(
                        "motivo" to "tiempo_agotado",
                        "transitorio" to true,
                        "intentos" to 1
                    )
                )
            } catch (e: ErrorApi.NoAutorizado) {
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        fase = ProductoUiState.Fase.Error(e.message),
                        mensaje = e.message
                    )
                }
                telemetria.registrarError(e, mapOf("accion" to "sincronizar", "error" to "401_no_autorizado"))
            } catch (e: ErrorApi.Prohibido) {
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        mensaje = "Permiso denegado: ${e.message}"
                    )
                }
                telemetria.registrarError(e, mapOf("accion" to "sincronizar", "error" to "403_prohibido"))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        fase = ProductoUiState.Fase.Error(e.message ?: "Error al sincronizar"),
                        mensaje = e.message ?: "Error al sincronizar"
                    )
                }
                telemetria.registrarError(e, mapOf("accion" to "sincronizar", "error" to "inesperado"))
            }
        }
    }

    fun reintentarFila(id: Long) {
        viewModelScope.launch {
            repositorio.reintentarFila(id)
            actualizarMarcaSincronizacion()
            telemetria.registrarEvento("sync_reintento_manual_fila", mapOf("producto_id" to id))
        }
    }

    fun reintentarTodasLasFallidas() {
        viewModelScope.launch {
            repositorio.reintentarTodasLasFallidas()
            actualizarMarcaSincronizacion()
            telemetria.registrarEvento("sync_reintento_manual_todas", emptyMap())
        }
    }

    fun buscarPorId(idStr: String) {
        val id = idStr.toLongOrNull()
        if (id == null) {
            _uiState.update { it.copy(mensaje = "Por favor ingrese un ID válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando, mensaje = "") }
            try {
                val producto = obtenerProductoUseCase(id)
                _uiState.update {
                    it.copy(fase = ProductoUiState.Fase.ConProductos(listOf(producto)))
                }
            } catch (e: ErrorApi.NoEncontrado) {
                _uiState.update {
                    it.copy(fase = ProductoUiState.Fase.Error(e.message), mensaje = e.message)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(fase = ProductoUiState.Fase.Error(e.message ?: "Error al buscar"), mensaje = e.message ?: "Error al buscar")
                }
            }
        }
    }

    fun onNombreChange(valor: String) {
        _uiState.update {
            it.copy(
                nombre = valor,
                nombreError = if (valor.isBlank()) "El nombre es obligatorio" else null
            )
        }
    }

    fun onPrecioChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El precio es obligatorio"
            valor.toDoubleOrNull() == null -> "Debe ser un número válido"
            (valor.toDoubleOrNull() ?: 0.0) <= 0.0 -> "El precio debe ser mayor a 0"
            else -> null
        }
        _uiState.update { it.copy(precio = valor, precioError = error) }
    }

    fun onStockChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El stock es obligatorio"
            valor.toIntOrNull() == null -> "Debe ser un número entero"
            (valor.toIntOrNull() ?: 0) < 0 -> "El stock no puede ser negativo"
            else -> null
        }
        _uiState.update { it.copy(stock = valor, stockError = error) }
    }

    fun registrarProducto() = guardarProducto()

    fun guardarProducto() {
        val state = _uiState.value
        val precioDouble = state.precio.toDoubleOrNull() ?: 0.0
        val stockInt = state.stock.toIntOrNull() ?: 0

        viewModelScope.launch {
            val esEdicion = state.productoEditandoId != null
            val tipoOperacion = if (esEdicion) {
                ProductoUiState.Operacion.Tipo.Actualizar
            } else {
                ProductoUiState.Operacion.Tipo.Crear
            }

            _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.EnCurso(tipoOperacion),
                    mensaje = ""
                )
            }

            try {
                if (esEdicion) {
                    val p = Producto(
                        id = state.productoEditandoId ?: 0L,
                        nombre = state.nombre.trim(),
                        precio = precioDouble,
                        stock = stockInt,
                        categoriaId = 1L
                    )
                    actualizarProductoUseCase(p)
                    _uiState.update {
                        it.copy(
                            operacion = ProductoUiState.Operacion.Inactiva,
                            mensaje = "Producto actualizado correctamente",
                            esExito = true,
                            productoEditandoId = null,
                            nombre = "",
                            precio = "",
                            stock = ""
                        )
                    }
                } else {
                    val p = Producto(
                        nombre = state.nombre.trim(),
                        precio = precioDouble,
                        stock = stockInt,
                        categoriaId = 1L
                    )
                    registrarProductoUseCase(p)
                    _uiState.update {
                        it.copy(
                            operacion = ProductoUiState.Operacion.Inactiva,
                            mensaje = "Producto registrado localmente (pendiente de sincronizar)",
                            esExito = true,
                            nombre = "",
                            precio = "",
                            stock = ""
                        )
                    }
                }
            } catch (e: ErrorApi.Validacion) {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            tipo = tipoOperacion,
                            error = e.message
                        ),
                        nombreError = e.validationErrors["nombre"],
                        precioError = e.validationErrors["precio"],
                        stockError = e.validationErrors["stock"],
                        mensaje = e.message,
                        esExito = false
                    )
                }
                telemetria.registrarError(e, mapOf("accion" to "guardar_producto", "error" to "validacion"))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            tipo = tipoOperacion,
                            error = e.message ?: "Error al guardar el producto"
                        ),
                        mensaje = e.message ?: "Error al guardar el producto",
                        esExito = false
                    )
                }
                telemetria.registrarError(e, mapOf("accion" to "guardar_producto"))
            }
        }
    }

    fun eliminarProducto(id: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Eliminar),
                    mensaje = ""
                )
            }
            try {
                eliminarProductoUseCase(id)
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        mensaje = "Producto eliminado correctamente",
                        esExito = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            tipo = ProductoUiState.Operacion.Tipo.Eliminar,
                            error = e.message ?: "Error al eliminar"
                        ),
                        mensaje = e.message ?: "Error al eliminar",
                        esExito = false
                    )
                }
                telemetria.registrarError(e, mapOf("accion" to "eliminar_producto", "id" to id.toString()))
            }
        }
    }

    fun limpiarEstado() {
        limpiarErrores()
    }

    fun limpiarErrores() {
        _uiState.update { state ->
            val fase = if (state.productos.isEmpty()) {
                ProductoUiState.Fase.SinProductos
            } else {
                ProductoUiState.Fase.ConProductos(state.productos)
            }
            state.copy(
                avisoRed = null,
                sincronizando = false,
                mensaje = "",
                fase = fase
            )
        }
    }

    fun onEditarProducto(producto: Producto) {
        _uiState.update {
            it.copy(
                productoEditandoId = producto.id,
                nombre = producto.nombre,
                precio = producto.precio.toString(),
                stock = producto.stock.toString(),
                nombreError = null,
                precioError = null,
                stockError = null,
                mensaje = ""
            )
        }
    }

    fun onCancelarEdicion() {
        _uiState.update {
            it.copy(
                productoEditandoId = null,
                nombre = "",
                precio = "",
                stock = "",
                nombreError = null,
                precioError = null,
                stockError = null,
                mensaje = ""
            )
        }
    }

    fun compartirProducto(producto: Producto) {
        val pendienteTexto = if (producto.pendiente) " [PENDIENTE]" else ""
        val texto = "${producto.nombre} — ${formatearSoles(producto.precio)} · Stock: ${producto.stock}$pendienteTexto"
        compartidor.compartir(texto)
    }
}
