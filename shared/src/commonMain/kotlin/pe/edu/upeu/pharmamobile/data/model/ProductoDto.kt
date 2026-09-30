package pe.edu.upeu.pharmamobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductoDto(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String = "",
    val images: List<String> = emptyList(),
    val category: CategoriaDto? = null
)

@Serializable
data class CategoriaDto(
    val id: Int,
    val name: String
)
