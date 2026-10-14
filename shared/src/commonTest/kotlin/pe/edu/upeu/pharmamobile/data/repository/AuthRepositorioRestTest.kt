package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.network.AuthApi
import pe.edu.upeu.pharmamobile.domain.model.Credenciales
import pe.edu.upeu.pharmamobile.domain.model.ErrorSesion
import kotlin.test.*

class AuthRepositorioRestTest {

    private class AlmacenSeguroFake : AlmacenSeguro {
        var token: String? = null
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken(): String? = token
        override fun borrarToken() { token = null }
    }

    private fun mockHttpClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(DefaultRequest) {
                contentType(ContentType.Application.Json)
            }
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens("test-token", "") }
                }
            }
            engine {
                addHandler(handler)
            }
        }
    }

    @Test
    fun login_exitoso_guarda_token_y_retorna_sesion() = runTest {
        val almacen = AlmacenSeguroFake()
        val client = mockHttpClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = """
                    {
                      "token": "jwt-token-12345",
                      "tipo": "Bearer",
                      "expiraEnSegundos": 3600,
                      "username": "admin",
                      "nombreCompleto": "Carlos Ramirez",
                      "roles": ["ADMIN"],
                      "permisos": ["CATALOGO_LEER", "CATALOGO_ESCRIBIR"]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        val sesion = repo.iniciarSesion(Credenciales("admin", "Admin123"))

        assertEquals("admin", sesion.usuario)
        assertEquals("Carlos Ramirez", sesion.nombreCompleto)
        assertEquals("jwt-token-12345", sesion.token)
        assertEquals("jwt-token-12345", almacen.token)
        assertTrue(sesion.puede("CATALOGO_LEER"))
        assertTrue(sesion.puede("CATALOGO_ESCRIBIR"))
        assertFalse(sesion.puede("CLIENTES_LEER"))
    }

    @Test
    fun login_con_credenciales_invalidas_lanza_CredencialesInvalidas() = runTest {
        val almacen = AlmacenSeguroFake()
        val client = mockHttpClient {
            respond(
                content = """{"status":401,"error":"Unauthorized","message":"Usuario o contraseña incorrectos"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        assertFailsWith<ErrorSesion.CredencialesInvalidas> {
            repo.iniciarSesion(Credenciales("admin", "clave-errada"))
        }
        assertNull(almacen.token)
    }

    @Test
    fun cerrar_sesion_borra_token_del_almacen() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-existente")
        val client = mockHttpClient { respondOk() }
        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        repo.cerrarSesion()

        assertNull(almacen.token)
    }

    @Test
    fun sesionGuardada_reconstruye_sesion_con_auth_me() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-guardado-abc")
        val client = mockHttpClient { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(
                content = """
                    {
                      "id": 1,
                      "username": "vendedor",
                      "empleadoNombre": "Lucia Fernandez",
                      "cargo": "Vendedora",
                      "roles": ["VENDEDOR"],
                      "permisos": ["CATALOGO_LEER", "CLIENTES_LEER"]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        val sesion = repo.sesionGuardada()

        assertNotNull(sesion)
        assertEquals("vendedor", sesion.usuario)
        assertEquals("Lucia Fernandez", sesion.nombreCompleto)
        assertEquals("token-guardado-abc", sesion.token)
        assertTrue(sesion.puede("CLIENTES_LEER"))
        assertFalse(sesion.puede("CATALOGO_ESCRIBIR"))
    }

    @Test
    fun sesionGuardada_con_token_expirado_401_limpia_almacen_y_retorna_null() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-expirado")
        val client = mockHttpClient {
            respond(
                content = """{"status":401,"error":"Unauthorized","message":"El token ha expirado"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        val sesion = repo.sesionGuardada()

        assertNull(sesion)
        assertNull(almacen.token)
    }
}
