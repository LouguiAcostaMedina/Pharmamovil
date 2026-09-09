package pe.edu.upeu.pharmamobile.presentation.producto

import pe.edu.upeu.pharmamobile.domain.model.Producto

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
    val mensaje: String = "",
    val esExito: Boolean = false
) {
    sealed interface Fase {
        data object Cargando : Fase
        data object SinProductos : Fase
        data class ConProductos(val lista: List<Producto>) : Fase
        data class Error(val mensaje: String) : Fase
    }
}
