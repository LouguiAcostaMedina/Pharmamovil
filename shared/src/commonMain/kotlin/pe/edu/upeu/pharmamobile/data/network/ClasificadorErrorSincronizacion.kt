package pe.edu.upeu.pharmamobile.data.network

import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import pe.edu.upeu.pharmamobile.domain.model.ResultadoEnvio

object ClasificadorErrorSincronizacion {

    const val MAX_INTENTOS = 5
    const val TOPE_ESPERA_MILLIS = 16_000L

    fun calcularEsperaMillis(intentos: Int): Long {
        val exponente = (intentos.coerceAtLeast(1) - 1).coerceAtMost(4)
        val espera = (1L shl exponente) * 1_000L
        return minOf(espera, TOPE_ESPERA_MILLIS)
    }

    fun clasificar(error: Throwable, intentosPrevios: Int, ahoraMillis: Long): ResultadoEnvio {
        val nuevosIntentos = intentosPrevios + 1
        if (nuevosIntentos >= MAX_INTENTOS) {
            return ResultadoEnvio.Permanente(
                motivo = "L?mite m?ximo de reintentos alcanzado ($MAX_INTENTOS intentos). ?ltimo error: ${error.message ?: "Fallo de conexi?n"}",
                codigoHttp = if (error is ErrorApi.Servidor) 500 else null
            )
        }
        return when (error) {
            is ErrorApi.SinConexion -> {
                val delay = calcularEsperaMillis(nuevosIntentos)
                ResultadoEnvio.Transitorio(
                    motivo = error.message.ifBlank { "Sin conexión con el servidor" },
                    reintentoEnMillis = ahoraMillis + delay
                )
            }
            is ErrorApi.TiempoAgotado -> {
                val delay = calcularEsperaMillis(nuevosIntentos)
                ResultadoEnvio.Transitorio(
                    motivo = error.message.ifBlank { "Tiempo de espera agotado" },
                    reintentoEnMillis = ahoraMillis + delay
                )
            }
            is ErrorApi.Servidor -> {
                val delay = calcularEsperaMillis(nuevosIntentos)
                ResultadoEnvio.Transitorio(
                    motivo = error.message.ifBlank { "Error interno del servidor (HTTP 5xx)" },
                    reintentoEnMillis = ahoraMillis + delay
                )
            }
            is ErrorApi.Validacion -> {
                val detalle = if (error.validationErrors.isNotEmpty()) {
                    ": " + error.validationErrors.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                } else ""
                ResultadoEnvio.Permanente(
                    motivo = "Validación rechazada (HTTP 400)$detalle",
                    codigoHttp = 400
                )
            }
            is ErrorApi.NoEncontrado -> {
                ResultadoEnvio.Permanente(
                    motivo = error.message.ifBlank { "Recurso no encontrado (HTTP 404)" },
                    codigoHttp = 404
                )
            }
            is ErrorApi.Conflicto -> {
                ResultadoEnvio.Permanente(
                    motivo = "Conflicto en el servidor (HTTP 409): ${error.message.ifBlank { "Recurso duplicado" }}",
                    codigoHttp = 409
                )
            }
            is ErrorApi.Prohibido -> {
                ResultadoEnvio.Permanente(
                    motivo = "Sin privilegios para registrar productos (HTTP 403)",
                    codigoHttp = 403
                )
            }
            is ErrorApi.NoAutorizado -> {
                ResultadoEnvio.Permanente(
                    motivo = "Sesión expirada o no autorizada (HTTP 401)",
                    codigoHttp = 401
                )
            }
            else -> {
                val delay = calcularEsperaMillis(nuevosIntentos)
                ResultadoEnvio.Transitorio(
                    motivo = error.message ?: "Fallo transitorio inesperado",
                    reintentoEnMillis = ahoraMillis + delay
                )
            }
        }
    }
}

