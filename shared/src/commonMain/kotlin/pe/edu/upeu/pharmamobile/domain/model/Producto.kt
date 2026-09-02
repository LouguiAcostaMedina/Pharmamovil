package pe.edu.upeu.pharmamobile.domain.model

data class Producto(
    val id: Long = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true
)
