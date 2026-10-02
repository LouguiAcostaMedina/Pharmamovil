package pe.edu.upeu.pharmamobile.domain.model

sealed class ErrorApi(override val message: String) : Exception(message) {
    data class Validacion(
        override val message: String,
        val validationErrors: Map<String, String> = emptyMap()
    ) : ErrorApi(message)

    data class NoEncontrado(override val message: String = "Recurso no encontrado") : ErrorApi(message)
    data class Conflicto(override val message: String = "Conflicto en la operación") : ErrorApi(message)
    data class Servidor(override val message: String = "Error interno del servidor") : ErrorApi(message)
    data class SinConexion(override val message: String = "No hay conexión a internet") : ErrorApi(message)
    data class TiempoAgotado(override val message: String = "Tiempo de espera agotado") : ErrorApi(message)
    data class Desconocido(override val message: String = "Error desconocido") : ErrorApi(message)
}
