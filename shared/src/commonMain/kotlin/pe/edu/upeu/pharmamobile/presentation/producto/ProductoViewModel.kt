package pe.edu.upeu.pharmamobile.presentation.producto

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.usecase.GetProductsUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.RegisterProductUseCase

data class ProductoUiState(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
    val mensaje: String = "",
    val esExito: Boolean = false,
    val productos: List<Producto> = emptyList()
)

class ProductoViewModel(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        val lista = getProductsUseCase()
        _uiState.update { it.copy(productos = lista) }
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
        // Clear previous state errors and status message
        _uiState.update { 
            it.copy(
                nombreError = null,
                precioError = null,
                stockError = null,
                mensaje = "",
                esExito = false
            )
        }

        val currentState = _uiState.value

        // 1. Validate Nombre (no vacío / isNotBlank)
        if (currentState.nombre.isBlank()) {
            _uiState.update {
                it.copy(
                    nombreError = "El nombre no puede estar vacío",
                    mensaje = "Error de validación",
                    esExito = false
                )
            }
            return
        }

        // 2. Validate Precio (numérico y > 0)
        val precioDouble = currentState.precio.toDoubleOrNull()
        if (precioDouble == null || precioDouble <= 0.0) {
            _uiState.update {
                it.copy(
                    precioError = "Ingrese un precio válido y mayor a 0",
                    mensaje = "Error de validación",
                    esExito = false
                )
            }
            return
        }

        // 3. Validate Stock (entero y >= 0)
        val stockInt = currentState.stock.toIntOrNull()
        if (stockInt == null || stockInt < 0) {
            _uiState.update {
                it.copy(
                    stockError = "Ingrese un stock entero mayor o igual a 0",
                    mensaje = "Error de validación",
                    esExito = false
                )
            }
            return
        }

        // If validation passes, attempt registration
        val result = registerProductUseCase(
            nombre = currentState.nombre,
            precioStr = currentState.precio,
            stockStr = currentState.stock
        )
        
        result.fold(
            onSuccess = { producto ->
                _uiState.update {
                    it.copy(
                        nombre = "",
                        precio = "",
                        stock = "",
                        nombreError = null,
                        precioError = null,
                        stockError = null,
                        mensaje = "Producto registrado: ${producto.nombre} - S/${producto.precio} - Stock: ${producto.stock}",
                        esExito = true
                    )
                }
                cargarProductos()
            },
            onFailure = { throwable ->
                _uiState.update {
                    it.copy(
                        mensaje = throwable.message ?: "Error al registrar producto",
                        esExito = false
                    )
                }
            }
        )
    }
}
