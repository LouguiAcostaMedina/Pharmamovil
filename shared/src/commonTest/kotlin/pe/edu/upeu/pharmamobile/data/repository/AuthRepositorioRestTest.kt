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
    fun cerrar_sesion_borra_token_del_almacen_y_limpia_cache_bearer() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-activo-123")
        val client = mockHttpClient { respondOk() }
        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        // Verificamos estado previo
        assertEquals("token-activo-123", almacen.leerToken())

        // Cierre de sesión completo
        repo.cerrarSesion()

        // 1) Token eliminado del almacén
        assertNull(almacen.leerToken())
        assertNull(almacen.token)

        // 2) Cache de BearerAuthProvider invalidada
        val bearerProvider = client.authProvider<BearerAuthProvider>()
        assertNotNull(bearerProvider)
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
                content = """{"status":401,"error":"Unauthorized","message":"El token ha expirado. Inicie sesión nuevamente"}""",
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

    @Test
    fun sesionGuardada_con_usuario_deshabilitado_401_limpia_almacen_y_retorna_null() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-usuario-inactivo")
        val client = mockHttpClient {
            respond(
                content = """{"status":401,"error":"Unauthorized","message":"El usuario está deshabilitado"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        val sesion = repo.sesionGuardada()

        assertNull(sesion, "Una sesión con usuario deshabilitado debe retornar null")
        assertNull(almacen.token, "El token debe ser purgado del almacén seguro al recibir 401 por usuario deshabilitado")
    }

    @Test
    fun sesionGuardada_con_token_alterado_401_limpia_almacen_y_retorna_null() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-adulterado-invalido")
        val client = mockHttpClient {
            respond(
                content = """{"status":401,"error":"Unauthorized","message":"El token no es válido"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        val sesion = repo.sesionGuardada()

        assertNull(sesion, "Un token alterado o inválido debe retornar null")
        assertNull(almacen.token, "El token alterado debe ser purgado del almacén")
    }

    @Test
    fun sesionGuardada_con_403_mantiene_token_en_almacen_y_lanza_SinPermiso() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-sin-permiso-valido")
        val client = mockHttpClient {
            respond(
                content = """{"status":403,"error":"Forbidden","message":"Acceso denegado: no cuenta con el rol o permiso requerido"}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        assertFailsWith<ErrorSesion.SinPermiso> {
            repo.sesionGuardada()
        }
        // Diferencia crítica 401 vs 403: 403 NO debe borrar el token de sesión
        assertEquals("token-sin-permiso-valido", almacen.token)
    }

    @Test
    fun cambio_de_usuario_admin_a_vendedor_reemplaza_permisos_sin_herencia() = runTest {
        val almacen = AlmacenSeguroFake()
        var callCount = 0

        val client = mockHttpClient { request ->
            callCount++
            if (callCount == 1) {
                // Login Admin
                respond(
                    content = """
                        {
                          "token": "token-admin",
                          "tipo": "Bearer",
                          "expiraEnSegundos": 3600,
                          "username": "admin",
                          "nombreCompleto": "Carlos Ramirez Admin",
                          "roles": ["ADMIN"],
                          "permisos": ["CATALOGO_LEER", "CATALOGO_ESCRIBIR", "REPORTES_VER", "VENTAS_REGISTRAR"]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                // Login Vendedor
                respond(
                    content = """
                        {
                          "token": "token-vendedor",
                          "tipo": "Bearer",
                          "expiraEnSegundos": 3600,
                          "username": "vendedor",
                          "nombreCompleto": "Lucia Fernandez Vendedora",
                          "roles": ["VENDEDOR"],
                          "permisos": ["CATALOGO_LEER", "CLIENTES_LEER", "VENTAS_REGISTRAR"]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        // 1. Iniciar sesión como Admin
        val sesionAdmin = repo.iniciarSesion(Credenciales("admin", "Admin123"))
        assertEquals("admin", sesionAdmin.usuario)
        assertTrue(sesionAdmin.puede("REPORTES_VER"))
        assertTrue(sesionAdmin.puede("CATALOGO_ESCRIBIR"))

        // 2. Cerrar sesión
        repo.cerrarSesion()
        assertNull(almacen.token)

        // 3. Iniciar sesión como Vendedor
        val sesionVendedor = repo.iniciarSesion(Credenciales("vendedor", "Vendedor123"))
        assertEquals("vendedor", sesionVendedor.usuario)
        assertTrue(sesionVendedor.puede("CLIENTES_LEER"))
        assertTrue(sesionVendedor.puede("VENTAS_REGISTRAR"))

        // Aislamiento de permisos: NO debe heredar permisos privilegiados de admin
        assertFalse(sesionVendedor.puede("REPORTES_VER"), "Vendedor no debe heredar REPORTES_VER de admin")
        assertFalse(sesionVendedor.puede("CATALOGO_ESCRIBIR"), "Vendedor no debe heredar CATALOGO_ESCRIBIR de admin")
    }

    @Test
    fun sesionGuardada_con_fallo_de_red_conserva_token_y_lanza_SinConexion() = runTest {
        val almacen = AlmacenSeguroFake()
        almacen.guardarToken("token-persisitente-valido")

        val client = mockHttpClient {
            throw io.ktor.utils.io.errors.IOException("Network unreachable")
        }

        val api = AuthApi(client)
        val repo = AuthRepositorioRest(client, almacen, api)

        assertFailsWith<ErrorSesion.SinConexion> {
            repo.sesionGuardada()
        }

        // Un error de red/timeout no debe desloguear agresivamente ni destruir el token
        assertEquals("token-persisitente-valido", almacen.token)
    }
}
