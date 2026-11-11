package pe.edu.upeu.pharmamobile.domain.model

sealed interface ResultadoEnvio {
    data class Exito(val producto: Producto) : ResultadoEnvio
    data class Transitorio(val motivo: String, val reintentoEnMillis: Long) : ResultadoEnvio
    data class Permanente(val motivo: String, val codigoHttp: Int? = null) : ResultadoEnvio
}

