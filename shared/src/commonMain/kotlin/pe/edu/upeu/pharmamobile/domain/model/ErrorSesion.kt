package pe.edu.upeu.pharmamobile.domain.model

sealed class ErrorSesion(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    data object CredencialesInvalidas : ErrorSesion("Credenciales inválidas")
    data object SesionExpirada : ErrorSesion("La sesión ha expirado")
    data object SinPermiso : ErrorSesion("No tiene permisos para realizar esta operación")
    data object SinConexion : ErrorSesion("Sin conexión al servidor")
    data class Desconocido(val mensaje: String) : ErrorSesion(mensaje)
}

