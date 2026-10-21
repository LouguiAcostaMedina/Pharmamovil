package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pe.edu.upeu.pharmamobile.data.model.auth.LoginRequestDto
import pe.edu.upeu.pharmamobile.data.model.auth.LoginResponseDto
import pe.edu.upeu.pharmamobile.data.model.auth.UsuarioResponseDto

class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequestDto): LoginResponseDto {
        return client.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun me(): UsuarioResponseDto {
        return client.get("auth/me").body()
    }

    /**
     * Petición de prueba para evidenciar el error HTTP 403 (Forbidden)
     * en roles que no cuentan con el permiso CLIENTES_LEER (como ALMACENERO).
     */
    suspend fun probarAccesoClientes(): String {
        return client.get("clientes").body()
    }
}
