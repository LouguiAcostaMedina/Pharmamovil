package pe.edu.upeu.pharmamobile.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import kotlinx.coroutines.CancellationException

import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.platform.formatearSoles

class ProductoViewModel(
    private val listarProductosUseCase: ListarProductosUseCase,
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase,
    private val eliminarProductoUseCase: EliminarProductoUseCase,
    private val obtenerProductoUseCase: ObtenerProductoUseCase,
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
            try {
                val lista = listarProductosUseCase()
                if (lista.isEmpty()) {
                    _uiState.update { it.copy(fase = ProductoUiState.Fase.SinProductos) }
                } else {
                    _uiState.update { it.copy(fase = ProductoUiState.Fase.ConProductos(lista)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorApi) {
                _uiState.update { it.copy(fase = ProductoUiState.Fase.Error(e.message)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(fase = ProductoUiState.Fase.Error(e.message ?: "Error desconocido")) }
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
                descripcion = "",
                imagen = "",
                categoria = ""
            )
            registrarProductoUseCase(p)
            _uiState.update {
                it.copy(
                    nombre = "",
                    precio = "",
                    stock = "",
                    mensaje = "Producto registrado exitosamente.",
                    esExito = true
                )
            }
            cargarProductos()
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
                descripcion = "",
                imagen = "",
                categoria = ""
            )
            actualizarProductoUseCase(p)
            _uiState.update {
                it.copy(
                    nombre = "",
                    precio = "",
                    stock = "",
                    mensaje = "Producto actualizado exitosamente.",
                    esExito = true
                )
            }
            cargarProductos()
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
            cargarProductos()
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
        val texto = "${producto.nombre} — ${formatearSoles(producto.precio)} · Stock: ${producto.stock}"
        compartidor.compartir(texto)
    }
}
