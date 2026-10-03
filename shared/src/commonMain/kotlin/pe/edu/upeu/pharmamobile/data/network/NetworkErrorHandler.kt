package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile.data.model.ErrorResponseDto
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi

suspend fun <T> safeApiCall(apiCall: suspend () -> T): T {
    return try {
        apiCall()
    } catch (e: Exception) {
        throw mapExceptionToErrorApi(e)
    }
}

private suspend fun mapExceptionToErrorApi(e: Exception): ErrorApi {
    return when (e) {
        is CancellationException -> throw e
        is ClientRequestException -> { // 4xx errors
            val response = e.response
            val errorDto = try {
                response.body<ErrorResponseDto>()
            } catch (serializationException: Exception) {
                null
            }
            val message = errorDto?.message ?: e.message ?: "Error en la solicitud"

            when (response.status.value) {
                400 -> ErrorApi.Validacion(message, errorDto?.validationErrors ?: emptyMap())
                404 -> ErrorApi.NoEncontrado("Producto no encontrado")
                409 -> ErrorApi.Conflicto(message)
                else -> ErrorApi.Desconocido(message)
            }
        }
        is ServerResponseException -> { // 5xx errors
            ErrorApi.Servidor("Error interno del servidor")
        }
        is IOException -> ErrorApi.SinConexion("No se pudo conectar con el servidor")
        is HttpRequestTimeoutException -> ErrorApi.TiempoAgotado("La solicitud tardó demasiado")
        else -> ErrorApi.Desconocido(e.message ?: "Error desconocido")
    }
}
