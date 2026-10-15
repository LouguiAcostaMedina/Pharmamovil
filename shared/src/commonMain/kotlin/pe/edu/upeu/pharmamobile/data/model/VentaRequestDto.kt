package pe.edu.upeu.pharmamobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DetalleVentaRequestDto(
    val productoId: Long,
    val cantidad: Int
)

@Serializable
data class VentaRequestDto(
    val clienteId: Long,
    val detalles: List<DetalleVentaRequestDto>
)

@Serializable
data class ClienteResponseDto(
    val id: Long,
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val email: String,
    val telefono: String? = null,
    val direccion: String? = null,
    val estado: Boolean = true
)

@Serializable
data class VentaPorCategoriaDto(
    val categoriaId: Long,
    val categoriaNombre: String,
    val totalVendido: Double,
    val unidadesVendidas: Long
)
