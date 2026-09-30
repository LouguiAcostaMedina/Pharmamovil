package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

val ktorHttpClient = httpClient {
    expectSuccess = true
    
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
    
    install(HttpTimeout) {
        requestTimeoutMillis = 15000L
        connectTimeoutMillis = 15000L
        socketTimeoutMillis = 15000L
    }
    
    defaultRequest {
        url("https://api.escuelajs.co/api/v1/")
        contentType(ContentType.Application.Json)
    }
}
