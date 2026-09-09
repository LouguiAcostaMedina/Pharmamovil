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
import pe.edu.upeu.pharmamobile.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val repository: ProductoRepository
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
                val lista = repository.listar()
                if (lista.isEmpty()) {
                    _uiState.update { it.copy(fase = ProductoUiState.Fase.SinProductos) }
                } else {
                    _uiState.update { it.copy(fase = ProductoUiState.Fase.ConProductos(lista)) }
                }
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
        _uiState.update { 
            it.copy(nombreError = null, precioError = null, stockError = null, mensaje = "", esExito = false)
        }
        val currentState = _uiState.value

        viewModelScope.launch {
            val p = Producto(
                nombre = currentState.nombre,
                precio = currentState.precio.toDoubleOrNull() ?: 0.0,
                stock = currentState.stock.toIntOrNull() ?: 0
            )

            val result = registrarProductoUseCase(p)
            result.fold(
                onSuccess = { producto ->
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
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            mensaje = error.message ?: "Error al registrar",
                            esExito = false
                        )
                    }
                }
            )
        }
    }
}
