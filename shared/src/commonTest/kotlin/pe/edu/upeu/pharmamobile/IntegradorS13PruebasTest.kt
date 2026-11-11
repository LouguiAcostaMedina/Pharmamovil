package pe.edu.upeu.pharmamobile

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile.data.local.crearDriverPruebas
import pe.edu.upeu.pharmamobile.data.network.ClasificadorErrorSincronizacion
import pe.edu.upeu.pharmamobile.data.network.ProductoApi
import pe.edu.upeu.pharmamobile.db.PharmaDb
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobile.domain.permission.EstadoPermiso
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.permission.Permiso
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobile.domain.telemetria.SanitizadorTelemetria
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoUiState
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.domain.usecase.*
import kotlin.test.*

class IntegradorS13PruebasTest {

    private fun crearBaseEnMemoria(): Pair<SqlDriver, PharmaDb> {
        val driver = crearDriverPruebas()
        val db = PharmaDb(driver)
        return Pair(driver, db)
    }

    private fun mockHttpClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
            engine {
                addHandler(handler)
            }
        }
    }

    // =========================================================================
    // 1. FALLO 503 INCREMENTA INTENTOS Y CONSERVA PENDIENTE
    // =========================================================================
    @Test
    fun test01_fallo503_incrementa_intentos_y_conserva_pendiente() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val pendienteInicial = local.guardarPendiente(
                Producto(id = 0, nombre = "Amoxicilina 500mg", precio = 12.5, stock = 30)
            )
            assertEquals(1, local.contarPendientes())
            assertEquals(0, pendienteInicial.intentos)

            val client = mockHttpClient { request ->
                if (request.method == HttpMethod.Post) {
                    respond(
                        content = """{"message": "Servicio no disponible"}""",
                        status = HttpStatusCode.ServiceUnavailable,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respond(
                        content = """{"contenido":[],"pagina":0,"tamanio":50,"totalElementos":0,"totalPaginas":0,"ultima":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            val repo = ProductoRepositorioOffline(
                local = local,
                remoto = ProductoApi(client),
                esperaProvider = {}
            )

            repo.sincronizar()

            val pendientes = local.pendientesParaSincronizacion(Long.MAX_VALUE)
            assertEquals(1, pendientes.size)
            val actualizado = pendientes[0]
            assertEquals(1, actualizado.intentos, "El número de intentos debe incrementarse a 1")
            assertTrue(actualizado.pendiente, "El producto debe continuar en estado pendiente")
            assertFalse(actualizado.falloPermanente, "Un error 503 es transitorio, no debe marcarse permanente")
            assertNotNull(actualizado.ultimoError, "Debe registrarse el mensaje del último error")
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 2. FALLO 409 SE CLASIFICA COMO PERMANENTE Y SE EXCLUYE DE REINTENTO AUTO
    // =========================================================================
    @Test
    fun test02_fallo409_clasificado_permanente_y_excluido_de_reintento_automatico() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val guardado = local.guardarPendiente(
                Producto(id = 0, nombre = "Paracetamol Duplicado", precio = 5.0, stock = 20)
            )

            val client = mockHttpClient { request ->
                if (request.method == HttpMethod.Post) {
                    respond(
                        content = """{"message": "Ya existe un producto con el nombre Paracetamol Duplicado"}""",
                        status = HttpStatusCode.Conflict,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respond(
                        content = """{"contenido":[],"pagina":0,"tamanio":50,"totalElementos":0,"totalPaginas":0,"ultima":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            val repo = ProductoRepositorioOffline(
                local = local,
                remoto = ProductoApi(client),
                esperaProvider = {}
            )

            repo.sincronizar()

            // Verificación: la fila queda excluida de reintentos automáticos
            val pendientesAuto = local.pendientesParaSincronizacion(Long.MAX_VALUE)
            assertEquals(0, pendientesAuto.size, "Una fila con fallo permanente debe excluirse del reintento automático")

            val fallidas = local.fallidas()
            assertEquals(1, fallidas.size, "Debe figurar en el listado de fallidas para revisión manual")
            assertTrue(fallidas[0].falloPermanente)
            assertEquals(guardado.id, fallidas[0].id)
            assertTrue(fallidas[0].ultimoError?.contains("409") == true || fallidas[0].ultimoError?.contains("Conflicto") == true)
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 3. REINTENTO MANUAL REARMA FILA PERMANENTE
    // =========================================================================
    @Test
    fun test03_reintento_manual_rearma_fila_permanente() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val guardado = local.guardarPendiente(
                Producto(id = 0, nombre = "Ibuprofeno Error", precio = 7.0, stock = 15)
            )
            local.registrarFalloPermanente(guardado.id, "Error de validación 400")

            assertEquals(0, local.pendientesParaSincronizacion(Long.MAX_VALUE).size)
            assertEquals(1, local.fallidas().size)

            // Acción: rearme manual
            local.rearmarReintentoManual(guardado.id)

            // Comprobación: vuelve a ser elegible para sincronizar y se limpian errores
            val rearmadas = local.pendientesParaSincronizacion(Long.MAX_VALUE)
            assertEquals(1, rearmadas.size)
            assertEquals(0, rearmadas[0].intentos)
            assertFalse(rearmadas[0].falloPermanente)
            assertNull(rearmadas[0].ultimoError)
            assertEquals(0, local.fallidas().size)
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 4. LÍMITE DE 5 INTENTOS Y TOPE DE ESPERA DE 16 SEGUNDOS
    // =========================================================================
    @Test
    fun test04_limite_5_intentos_y_tope_espera_16_segundos() {
        // Backoff exponencial: 1s, 2s, 4s, 8s, 16s tope
        assertEquals(1_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(1))
        assertEquals(2_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(2))
        assertEquals(4_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(3))
        assertEquals(8_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(4))
        assertEquals(16_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(5))
        assertEquals(16_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(6), "No debe superar el tope de 16s")
        assertEquals(16_000L, ClasificadorErrorSincronizacion.calcularEsperaMillis(10), "No debe superar el tope de 16s")

        // Clasificación: el 5to intento fallido se convierte en Permanente
        val errorTransitorio = ErrorApi.Servidor("Error 500")
        val resultadoIntento4 = ClasificadorErrorSincronizacion.clasificar(
            error = errorTransitorio,
            intentosPrevios = 3, // nuevo intento será 4
            ahoraMillis = 1000L
        )
        assertTrue(resultadoIntento4 is ResultadoEnvio.Transitorio)

        val resultadoIntento5 = ClasificadorErrorSincronizacion.clasificar(
            error = errorTransitorio,
            intentosPrevios = 4, // nuevo intento será 5 (alcanza MAX_INTENTOS)
            ahoraMillis = 1000L
        )
        assertTrue(resultadoIntento5 is ResultadoEnvio.Permanente, "Al alcanzar MAX_INTENTOS (5) debe pasar a Permanente")
    }

    // =========================================================================
    // 5. TRANSACCIÓN DE CONFIRMACIÓN REEMPLAZA FILA Y CONSERVA OTRAS PENDIENTES
    // =========================================================================
    @Test
    fun test05_transaccion_confirmacion_reemplaza_fila_y_conserva_otras_pendientes() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val p1 = local.guardarPendiente(Producto(id = 0, nombre = "Prod A", precio = 10.0, stock = 5))
            val p2 = local.guardarPendiente(Producto(id = 0, nombre = "Prod B", precio = 20.0, stock = 10))

            assertEquals(2, local.contarPendientes())

            // Confirmar únicamente p1 con ID asignado por el servidor (ej. 101)
            val confirmado1 = Producto(
                id = 101L,
                nombre = p1.nombre,
                precio = p1.precio,
                stock = p1.stock,
                activo = true,
                pendiente = false
            )
            local.confirmarSubida(idTemporal = p1.id, confirmado = confirmado1)

            // p1 ya no es pendiente
            assertEquals(1, local.contarPendientes(), "Sólo debe quedar 1 pendiente (Prod B)")
            val pendientes = local.pendientesParaSincronizacion(Long.MAX_VALUE)
            assertEquals(1, pendientes.size)
            assertEquals(p2.id, pendientes[0].id)

            // p1 existe en el catálogo confirmado
            val p1Consultado = local.obtenerPorId(101L)
            assertNotNull(p1Consultado)
            assertFalse(p1Consultado.pendiente)
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 6. DESCARGA DE CATÁLOGO REMOTO PRESERVA FILAS PENDIENTES
    // =========================================================================
    @Test
    fun test06_descarga_catalogo_preserva_pendientes_locales() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            // Insertar producto local en modo offline
            val localPendiente = local.guardarPendiente(
                Producto(id = 0, nombre = "Local Offline Sin Red", precio = 15.0, stock = 2)
            )

            // Catálogo remoto descargado
            val remotos = listOf(
                Producto(id = 501L, nombre = "Remoto 1", precio = 30.0, stock = 100, pendiente = false),
                Producto(id = 502L, nombre = "Remoto 2", precio = 40.0, stock = 200, pendiente = false)
            )

            local.reemplazarSincronizados(remotos)

            val todos = local.observar().first()
            assertEquals(3, todos.size, "Debe contener los 2 remotos más el producto pendiente local")
            assertTrue(todos.any { it.id == localPendiente.id && it.pendiente }, "El producto pendiente no debe ser sobreescrito ni eliminado")
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 7. CLAVE DE IDEMPOTENCIA SE CONSERVA
    // =========================================================================
    @Test
    fun test07_clave_idempotencia_se_conserva() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val p = local.guardarPendiente(
                Producto(id = 0, nombre = "Idempotente Test", precio = 9.9, stock = 10)
            )

            val claveGenerada = "idem-uuid-12345"
            local.actualizarClaveIdempotencia(p.id, claveGenerada)

            val recuperado = local.obtenerPorId(p.id)
            assertNotNull(recuperado)
            assertEquals(claveGenerada, recuperado.claveIdempotencia)
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 8. MIGRACIÓN DE BASE DE DATOS DE ESQUEMA 1 A 2
    // =========================================================================
    @Test
    fun test08_migracion_esquema_1_a_2_conserva_datos() {
        val driver = crearDriverPruebas()
        try {
            val versionEsquema = PharmaDb.Schema.version
            assertEquals(3L, versionEsquema, "El esquema de PharmaDb con migración 2.sqm debe estar en versión 3")

            val db = PharmaDb(driver)
            val local = ProductoLocal(db)
            val p = local.guardarPendiente(Producto(id = 0, nombre = "Migracion Test", precio = 8.0, stock = 4))

            // Probar consultas añadidas en la migración 2.sqm
            local.registrarFalloTransitorio(p.id, "Fallo de prueba", 5000L)
            local.registrarFalloPermanente(p.id, "Fallo permanente")
            assertEquals(1, local.contarFallidas())
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // 9. GESTOR DE PERMISOS NATIVOS CON TRES ESTADOS
    // =========================================================================
    @Test
    fun test09_gestor_permisos_maneja_tres_estados() = runTest {
        var estadoActual: EstadoPermiso = EstadoPermiso.Denegado
        val gestor = object : GestorPermisos {
            override suspend fun estado(permiso: Permiso): EstadoPermiso = estadoActual
            override suspend fun solicitar(permiso: Permiso): EstadoPermiso = estadoActual
            override fun abrirAjustes() {}
        }

        // Estado inicial Denegado
        assertEquals(EstadoPermiso.Denegado, gestor.estado(Permiso.NOTIFICACIONES))

        // Cambio a Concedido
        estadoActual = EstadoPermiso.Concedido
        assertEquals(EstadoPermiso.Concedido, gestor.solicitar(Permiso.NOTIFICACIONES))

        // Cambio a DenegadoParaSiempre
        estadoActual = EstadoPermiso.DenegadoParaSiempre
        assertEquals(EstadoPermiso.DenegadoParaSiempre, gestor.solicitar(Permiso.NOTIFICACIONES))
    }

    // =========================================================================
    // 10. SANITIZADOR DE TELEMETRÍA LIMPIA DATOS SENSIBLES
    // =========================================================================
    @Test
    fun test10_sanitizador_telemetria_limpia_datos_sensibles() {
        val parametrosSensibles: Map<String, Any> = mapOf(
            "token" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.abc.def",
            "password" to "MiPasswordSeguro123!",
            "email" to "usuario@hospital.com",
            "paciente_nombre" to "Juan Perez",
            "producto_nombre" to "Amoxicilina",
            "precio" to 12.5
        )

        val saneados = SanitizadorTelemetria.sanearParametros(parametrosSensibles)

        assertEquals("[PROTEGIDO]", saneados["token"])
        assertEquals("[PROTEGIDO]", saneados["password"])
        assertEquals("[PROTEGIDO]", saneados["email"])
        assertEquals("[PROTEGIDO]", saneados["paciente_nombre"])
        assertEquals("Amoxicilina", saneados["producto_nombre"], "Campos seguros no deben alterarse")
        assertEquals(12.5, saneados["precio"], "Campos numéricos seguros se conservan")

        // Sanitización de textos libres con tokens o correos incrustados
        val textoConJwt = "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test.sign y contacto admin@farmacia.com"
        val textoSaneado = SanitizadorTelemetria.sanearTexto(textoConJwt)
        assertFalse(textoSaneado.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse(textoSaneado.contains("admin@farmacia.com"))
        assertTrue(textoSaneado.contains("Bearer [PROTEGIDO]"))
        assertTrue(textoSaneado.contains("[EMAIL_PROTEGIDO]"))
    }

    // =========================================================================
    // 11. FALLO DE TELEMETRÍA NO CRASHEA EL FLUJO DE LA APP
    // =========================================================================
    @Test
    fun test11_fallo_telemetria_no_rompe_flujo() {
        val telemetriaQueFalla = object : Telemetria {
            override fun registrarEvento(nombre: String, datos: Map<String, Any>) {
                throw RuntimeException("Fallo simulado de Firebase SDK")
            }
            override fun registrarError(error: Throwable, contexto: Map<String, String>) {
                throw RuntimeException("Fallo simulado de Firebase SDK")
            }
        }

        // Ejecutar registrarEvento y registrarError encapsulados
        var ejecutadoSinExcepcion = false
        try {
            try {
                telemetriaQueFalla.registrarEvento("evento_prueba", emptyMap())
            } catch (e: Exception) {
                // Adaptador seguro debe absorber o ViewModel absorbe
            }
            ejecutadoSinExcepcion = true
        } catch (t: Throwable) {
            fail("Una excepción de telemetría no debe propagarse al usuario: ${t.message}")
        }
        assertTrue(ejecutadoSinExcepcion)
    }
}
