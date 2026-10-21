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
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
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
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    private var observacionIniciada = false
    private var tickerJob: Job? = null

    init {
        iniciarObservacion()
        actualizarMarcaSincronizacion()
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

    fun cargarProductos() {
        iniciarObservacion()
        if (_uiState.value.productos.isEmpty()) {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
        }
        sincronizar()
    }

    fun sincronizar() {
        viewModelScope.launch {
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
                        avisoRed = null,
                        fase = fase,
                        ultimaSincronizacionTimestamp = nuevoTimestamp,
                        ultimaSincronizacion = AntiguedadSincronizacion.formatear(nuevoTimestamp)
                    )
                }
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
                        avisoRed = "Sin conexión: mostrando datos guardados localmente.",
                        fase = fase
                    )
                }
            } catch (e: ErrorApi.TiempoAgotado) {
                _uiState.update { state ->
                    val fase = if (state.productos.isEmpty()) {
                        ProductoUiState.Fase.SinProductos
                    } else {
                        ProductoUiState.Fase.ConProductos(state.productos)
                    }
                    state.copy(
                        sincronizando = false,
                        avisoRed = "Tiempo de espera agotado: mostrando datos locales.",
                        fase = fase
                    )
                }
            } catch (e: ErrorApi.NoAutorizado) {
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        fase = ProductoUiState.Fase.Error(e.message),
                        mensaje = e.message
                    )
                }
            } catch (e: ErrorApi.Prohibido) {
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        mensaje = "Permiso denegado: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state.productos.isEmpty()) {
                        state.copy(
                            sincronizando = false,
                            fase = ProductoUiState.Fase.Error(e.message ?: "Error al sincronizar"),
                            mensaje = e.message ?: "Error al sincronizar"
                        )
                    } else {
                        state.copy(
                            sincronizando = false,
                            avisoRed = "No se pudo sincronizar: ${e.message ?: "Servidor no disponible"}"
                        )
                    }
                }
            }
        }
    }

    fun buscarPorId(idStr: String) {
        val id = idStr.toLongOrNull()
        if (id == null) {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Error("ID inválido")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
            try {
                val p = obtenerProductoUseCase(id)
                _uiState.update { it.copy(fase = ProductoUiState.Fase.ConProductos(listOf(p))) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorApi) {
                _uiState.update { it.copy(fase = ProductoUiState.Fase.Error(e.message)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(fase = ProductoUiState.Fase.Error(e.message ?: "Error desconocido")) }
            }
        }
    }

    fun onNombreChange(nuevoNombre: String) {
        _uiState.update { it.copy(nombre = nuevoNombre, nombreError = null) }
    }

    fun onPrecioChange(nuevoPrecio: String) {
        _uiState.update { it.copy(precio = nuevoPrecio, precioError = null) }
    }

    fun onStockChange(nuevoStock: String) {
        _uiState.update { it.copy(stock = nuevoStock, stockError = null) }
    }

    fun registrarProducto() {
        val operacion = _uiState.value.operacion
        if (operacion is ProductoUiState.Operacion.EnCurso) return

        val id = _uiState.value.productoEditandoId
        if (id != null) {
            actualizarProducto(id)
            return
        }

        ejecutarOperacion(ProductoUiState.Operacion.Tipo.Crear) {
            val currentState = _uiState.value
            val p = Producto(
                id = 0,
                nombre = currentState.nombre,
                precio = currentState.precio.toDoubleOrNull() ?: 0.0,
                stock = currentState.stock.toIntOrNull() ?: 0,
                activo = true,
                descripcion = "",
                imagen = "",
                categoria = "",
                categoriaId = 1L
            )
            val guardado = registrarProductoUseCase(p)
            val msg = if (guardado.pendiente) {
                "Producto guardado localmente (pendiente de sincronización)."
            } else {
                "Producto registrado y sincronizado exitosamente."
            }
            _uiState.update {
                it.copy(
                    nombre = "",
                    precio = "",
                    stock = "",
                    mensaje = msg,
                    esExito = true
                )
            }
            // NO se realiza recarga manual: el Flow de SQLDelight actualiza la UI automáticamente
        }
    }

    fun actualizarProducto(id: Long) {
        ejecutarOperacion(ProductoUiState.Operacion.Tipo.Actualizar) {
            val currentState = _uiState.value
            val p = Producto(
                id = id,
                nombre = currentState.nombre,
                precio = currentState.precio.toDoubleOrNull() ?: 0.0,
                stock = currentState.stock.toIntOrNull() ?: 0,
                activo = true,
                descripcion = "",
                imagen = "",
                categoria = "",
                categoriaId = 1L
            )
            actualizarProductoUseCase(p)
            _uiState.update {
                it.copy(
                    productoEditandoId = null,
                    nombre = "",
                    precio = "",
                    stock = "",
                    mensaje = "Producto actualizado exitosamente.",
                    esExito = true
                )
            }
            // NO se realiza recarga manual: el Flow de SQLDelight actualiza la UI automáticamente
        }
    }

    fun eliminarProducto(id: Long) {
        ejecutarOperacion(ProductoUiState.Operacion.Tipo.Eliminar) {
            eliminarProductoUseCase(id)
            _uiState.update {
                it.copy(
                    mensaje = "Producto eliminado exitosamente.",
                    esExito = true
                )
            }
            // NO se realiza recarga manual: el Flow de SQLDelight actualiza la UI automáticamente
        }
    }

    private fun ejecutarOperacion(
        tipo: ProductoUiState.Operacion.Tipo,
        block: suspend () -> Unit
    ) {
        _uiState.update {
            it.copy(
                nombreError = null, precioError = null, stockError = null,
                mensaje = "", esExito = false,
                operacion = ProductoUiState.Operacion.EnCurso(tipo)
            )
        }
        viewModelScope.launch {
            try {
                block()
                _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Inactiva) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorApi.Validacion) {
                _uiState.update {
                    it.copy(
                        nombreError = e.validationErrors["nombre"],
                        precioError = e.validationErrors["precio"],
                        stockError = e.validationErrors["stock"],
                        operacion = ProductoUiState.Operacion.Fallida(tipo, e.message)
                    )
                }
            } catch (e: ErrorApi.NoAutorizado) {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(tipo, e.message),
                        fase = ProductoUiState.Fase.Error(e.message),
                        mensaje = e.message
                    )
                }
            } catch (e: ErrorApi) {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(tipo, e.message),
                        mensaje = e.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(tipo, e.message ?: "Error desconocido"),
                        mensaje = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun limpiarEstado() {
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
