package pe.edu.upeu.pharmamobile.data.model.auth

import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobile.domain.model.Sesion

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long = 3600,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList()
) {
    fun aSesion(): Sesion = Sesion(
        token = token,
        usuario = username,
        nombreCompleto = nombreCompleto,
        permisos = permisos.toSet()
    )
}

@Serializable
data class UsuarioResponseDto(
    val id: Long? = null,
    val username: String,
    val empleadoId: Long? = null,
    val empleadoNombre: String? = null,
    val cargo: String? = null,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList(),
    val estado: Boolean? = true,
    val fechaCreacion: String? = null
) {
    fun aSesion(token: String): Sesion = Sesion(
        token = token,
        usuario = username,
        nombreCompleto = empleadoNombre ?: username,
        permisos = permisos.toSet()
    )
}
