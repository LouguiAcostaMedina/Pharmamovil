package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

fun crearHttpClient(
    almacen: AlmacenSeguro,
    esDepuracion: Boolean = true
): HttpClient = httpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(DefaultRequest) {
        url(obtenerUrlBase())
        contentType(ContentType.Application.Json)
    }

    install(Auth) {
        bearer {
            loadTokens {
                almacen.leerToken()?.let { BearerTokens(it, "") }
            }
            sendWithoutRequest { peticion ->
                !peticion.url.encodedPath.endsWith("/auth/login")
            }
        }
    }

    install(Logging) {
        level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { nombre ->
            nombre.equals(HttpHeaders.Authorization, ignoreCase = true)
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15000L
        connectTimeoutMillis = 15000L
        socketTimeoutMillis = 15000L
    }
}

/**
 * Cliente de diagnóstico exclusivo para pruebas S10 de laboratorio.
 * No instala el plugin Auth para permitir enviar peticiones sin cabecera (E04)
 * o con prefijos de cabecera alterados manualmente (E05) sin intercepción ni reintentos.
 */
fun crearClienteDiagnostico(): HttpClient = httpClient {
    expectSuccess = false

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(DefaultRequest) {
        url(obtenerUrlBase())
        contentType(ContentType.Application.Json)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15000L
        connectTimeoutMillis = 15000L
        socketTimeoutMillis = 15000L
    }
}

