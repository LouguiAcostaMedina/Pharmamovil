package pe.edu.upeu.pharmamobile.domain.model

data class Producto(
    val id: Long = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int,
) {
    val requiereReposicion: Boolean get() = stock <= STOCK_MINIMO

    companion object {
        const val STOCK_MINIMO = 5
    }
}
