package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun httpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin) {
    config(this)
    engine {
        configureRequest {
            setAllowsCellularAccess(true)
        }
    }
}
