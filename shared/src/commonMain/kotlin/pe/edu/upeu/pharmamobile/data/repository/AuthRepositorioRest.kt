package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.errors.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.model.auth.LoginRequestDto
import pe.edu.upeu.pharmamobile.data.network.AuthApi
import pe.edu.upeu.pharmamobile.domain.model.Credenciales
import pe.edu.upeu.pharmamobile.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobile.domain.model.Sesion
import pe.edu.upeu.pharmamobile.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile.tiempoActualEpochSegundos

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
            almacen.guardarDatosSesion(
                usuario = responseDto.username,
                nombreCompleto = responseDto.nombreCompleto,
                permisos = responseDto.permisos.toSet()
            )

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
            // Revalida con /auth/me en el servidor cuando hay red
            val usuarioDto = authApi.me()
            val sesion = usuarioDto.aSesion(token)
            almacen.guardarDatosSesion(
                usuario = sesion.usuario,
                nombreCompleto = sesion.nombreCompleto,
                permisos = sesion.permisos
            )
            sesion
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // Token revocado o expirado en el servidor: invalidar sesión y limpiar almacén
                cerrarSesion()
                null
            } else if (e.response.status == HttpStatusCode.Forbidden) {
                throw ErrorSesion.SinPermiso
            } else {
                throw ErrorSesion.Desconocido(e.message)
            }
        } catch (e: Exception) {
            // Distingue falta de conexión: recuperar sesión previamente autenticada guardada
            if (e is HttpRequestTimeoutException || e is IOException || e is ErrorSesion.SinConexion || e.message?.contains("connect", ignoreCase = true) == true) {
                val datos = almacen.leerDatosSesion()
                if (datos != null && datos.usuario.isNotBlank()) {
                    val exp = extraerExpiracion(token)
                    val expirado = exp != null && exp <= tiempoActualEpochSegundos()
                    if (expirado) {
                        // Política B05: Sesión caducada sin red
                        // Se preserva el acceso de solo lectura al catálogo local guardado,
                        // restringiendo a permisos de lectura y marcando requiereRenovacion = true.
                        // El token y pendientes se preservan intactos sin forzar logout sin red.
                        val permisosLectura = datos.permisos.filter { it.endsWith("_LEER") }.toSet()
                        Sesion(
                            token = token,
                            usuario = datos.usuario,
                            nombreCompleto = "${datos.nombreCompleto} (Sesión caducada)",
                            permisos = permisosLectura,
                            requiereRenovacion = true
                        )
                    } else {
                        Sesion(
                            token = token,
                            usuario = datos.usuario,
                            nombreCompleto = datos.nombreCompleto,
                            permisos = datos.permisos,
                            requiereRenovacion = false
                        )
                    }
                } else {
                    throw ErrorSesion.SinConexion
                }
            } else {
                if (e is ErrorSesion) throw e
                null
            }
        }
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private fun extraerExpiracion(token: String): Long? {
        return try {
            val partes = token.split(".")
            if (partes.size < 2) return null
            var b64 = partes[1].replace('-', '+').replace('_', '/')
            while (b64.length % 4 != 0) b64 += "="
            val jsonStr = b64.decodeBase64String()
            val json = Json.parseToJsonElement(jsonStr).jsonObject
            json["exp"]?.jsonPrimitive?.longOrNull
        } catch (_: Exception) {
            null
        }
    }
}
