package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.model.auth.LoginRequestDto
import pe.edu.upeu.pharmamobile.data.network.AuthApi
import pe.edu.upeu.pharmamobile.domain.model.Credenciales
import pe.edu.upeu.pharmamobile.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobile.domain.model.Sesion
import pe.edu.upeu.pharmamobile.domain.repository.AuthRepository

inline fun <reified T : AuthProvider> HttpClient.authProvider(): T? {
    return pluginOrNull(Auth)?.providers?.filterIsInstance<T>()?.firstOrNull()
}

class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro,
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
        return try {
            val responseDto = authApi.login(
                LoginRequestDto(
                    username = credenciales.usuario,
                    password = credenciales.clave
                )
            )

            // Guardar token cifrado en el almacén de la plataforma
            almacen.guardarToken(responseDto.token)

            // Invalidar la caché de BearerAuthProvider para que la siguiente llamada lea el nuevo token
            cliente.authProvider<BearerAuthProvider>()?.clearToken()

            responseDto.aSesion()
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw ErrorSesion.CredencialesInvalidas
                HttpStatusCode.Forbidden -> throw ErrorSesion.SinPermiso
                else -> throw ErrorSesion.Desconocido(e.message)
            }
        } catch (e: HttpRequestTimeoutException) {
            throw ErrorSesion.SinConexion
        } catch (e: IOException) {
            throw ErrorSesion.SinConexion
        } catch (e: Exception) {
            if (e is ErrorSesion) throw e
            throw ErrorSesion.Desconocido(e.message ?: "Error al iniciar sesión")
        }
    }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken()
        if (token.isNullOrBlank()) {
            return null
        }

        return try {
            // Reconstruye usuario y permisos consultando /auth/me con el token almacenado
            val usuarioDto = authApi.me()
            usuarioDto.aSesion(token)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // Token expirado o revocado en el servidor: limpiar almacén y caché
                cerrarSesion()
                null
            } else if (e.response.status == HttpStatusCode.Forbidden) {
                throw ErrorSesion.SinPermiso
            } else {
                throw ErrorSesion.Desconocido(e.message)
            }
        } catch (e: HttpRequestTimeoutException) {
            // Distingue fallo de red de sesión inválida: no borra el token
            throw ErrorSesion.SinConexion
        } catch (e: IOException) {
            // Distingue falta de conexión: no borra el token
            throw ErrorSesion.SinConexion
        } catch (e: Exception) {
            if (e is ErrorSesion) throw e
            null
        }
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
    }
}
