package pe.edu.upeu.pharmamobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PaginaResponse<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)
