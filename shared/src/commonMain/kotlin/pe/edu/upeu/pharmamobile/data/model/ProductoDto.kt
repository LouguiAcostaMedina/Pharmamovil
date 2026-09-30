package pe.edu.upeu.pharmamobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductoDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean
)
