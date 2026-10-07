package pe.edu.upeu.pharmamobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductoResponseDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null,
    val fechaCreacion: String? = null,
    val fechaModificacion: String? = null
)
