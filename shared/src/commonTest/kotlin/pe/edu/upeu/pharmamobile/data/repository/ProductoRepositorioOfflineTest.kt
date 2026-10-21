package pe.edu.upeu.pharmamobile.data.repository

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile.data.local.crearDriverPruebas
import pe.edu.upeu.pharmamobile.data.network.ProductoApi
import pe.edu.upeu.pharmamobile.db.PharmaDb
import pe.edu.upeu.pharmamobile.domain.model.ErrorApi
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.presentation.producto.AntiguedadSincronizacion
import kotlin.test.*

class ProductoRepositorioOfflineTest {

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
    // PRODUCTO 1: SEIS PRUEBAS OBLIGATORIAS (ACTIVIDAD AUTÓNOMA S11)
    // =========================================================================

    /**
     * Caso 1. Sincronización con red:
     * Tras sincronizar, la base contiene los productos del servidor y el Flow emite la lista completa.
     */
    @Test
    fun caso1_sincronizacion_con_red_escribe_en_base_y_flow_emite_lista_completa() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val client = mockHttpClient { request ->
                assertTrue(request.url.toString().contains("productos"))
                assertEquals(HttpMethod.Get, request.method)
                respond(
                    content = """
                        {
                          "contenido": [
                            {
                              "id": 1,
                              "nombre": "Paracetamol 500mg",
                              "precio": 2.50,
                              "stock": 100,
                              "estado": true,
                              "categoriaId": 1,
                              "categoriaNombre": "Analgésicos"
                            },
                            {
                              "id": 2,
                              "nombre": "Ibuprofeno 400mg",
                              "precio": 3.00,
                              "stock": 50,
                              "estado": true,
                              "categoriaId": 1,
                              "categoriaNombre": "Analgésicos"
                            }
                          ],
                          "pagina": 0,
                          "tamanio": 50,
                          "totalElementos": 2,
                          "totalPaginas": 1,
                          "ultima": true
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // Inicialmente vacío
            val inicial = repo.observar().first()
            assertEquals(0, inicial.size)

            // Sincronizar descarga de la red y persiste en SQLDelight
            repo.sincronizar()

            // El Flow emite automáticamente los datos persistidos
            val productos = repo.observar().first()
            assertEquals(2, productos.size)
            assertEquals("Ibuprofeno 400mg", productos[0].nombre) // Orden alfabetico por nombre
            assertEquals("Paracetamol 500mg", productos[1].nombre)
            assertEquals(1L, productos[1].id)
            assertFalse(productos[0].pendiente)
            assertFalse(productos[1].pendiente)
        } finally {
            driver.close()
        }
    }

    /**
     * Caso 2. Registro sin red:
     * Al fallar el envío por desconexión, el producto permanece guardado con ID negativo y marca de pendiente,
     * y el Flow lo emite igualmente.
     */
    @Test
    fun caso2_registro_sin_red_conserva_producto_con_id_negativo_y_marca_pendiente() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val client = mockHttpClient {
                throw IOException("Sin conexión a la red simulada")
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            val nuevo = Producto(
                id = 0,
                nombre = "Amoxicilina 500mg",
                precio = 5.0,
                stock = 30,
                activo = true,
                categoria = "Antibióticos",
                categoriaId = 2L
            )

            // Registrar en modo sin red
            val guardado = repo.registrar(nuevo)

            // Verifica que el ID sea negativo y esté marcado como pendiente
            assertTrue(guardado.id < 0L, "El ID local debe ser negativo, fue: ${guardado.id}")
            assertTrue(guardado.pendiente, "El producto debe quedar marcado como pendiente")

            // Visible inmediatamente desde el Flow
            val lista = repo.observar().first()
            assertEquals(1, lista.size)
            assertEquals(guardado.id, lista[0].id)
            assertEquals("Amoxicilina 500mg", lista[0].nombre)
            assertTrue(lista[0].pendiente)
            assertEquals(1L, local.contarPendientes())
        } finally {
            driver.close()
        }
    }

    /**
     * Caso 3. Envío al reconectar:
     * El pendiente se envía mediante POST antes de descargar. La respuesta confirma un ID positivo,
     * elimina la marca de pendiente y deja una única representación local.
     */
    @Test
    fun caso3_envio_al_reconectar_sube_pendiente_mediante_post_antes_de_descarga_asigna_id_positivo_y_elimina_pendiente() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)

            // 1. Crear producto sin red con ID negativo
            val offline = local.guardarPendiente(
                Producto(
                    id = 0,
                    nombre = "Cetirizina 10mg",
                    precio = 4.20,
                    stock = 40,
                    activo = true,
                    categoria = "Antialérgicos",
                    categoriaId = 1L
                )
            )
            assertTrue(offline.id < 0L)
            assertTrue(offline.pendiente)

            // 2. MockEngine que registra la secuencia estricta de llamadas HTTP
            val llamadasHttp = mutableListOf<String>()
            val client = mockHttpClient { request ->
                if (request.method == HttpMethod.Post && request.url.toString().contains("productos")) {
                    llamadasHttp.add("POST")
                    respond(
                        content = """
                            {
                              "id": 99,
                              "nombre": "Cetirizina 10mg",
                              "precio": 4.20,
                              "stock": 40,
                              "estado": true,
                              "categoriaId": 1,
                              "categoriaNombre": "Antialérgicos",
                              "fechaCreacion": "2026-10-21T12:00:00"
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else if (request.method == HttpMethod.Get && request.url.toString().contains("productos")) {
                    llamadasHttp.add("GET")
                    respond(
                        content = """
                            {
                              "contenido": [
                                {
                                  "id": 99,
                                  "nombre": "Cetirizina 10mg",
                                  "precio": 4.20,
                                  "stock": 40,
                                  "estado": true,
                                  "categoriaId": 1,
                                  "categoriaNombre": "Antialérgicos"
                                }
                              ],
                              "pagina": 0,
                              "tamanio": 50,
                              "totalElementos": 1,
                              "totalPaginas": 1,
                              "ultima": true
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // 3. Sincronizar reconectado
            repo.sincronizar()

            // 4. Verificaciones de secuencia: Primero POST, luego GET
            assertEquals(listOf("POST", "GET"), llamadasHttp, "El orden estricto debe ser subir pendientes (POST) y luego descargar (GET)")

            // 5. Verificación de estado local resultante
            val lista = repo.observar().first()
            assertEquals(1, lista.size, "Debe haber una sola representación local del producto")
            val p = lista.first()
            assertEquals(99L, p.id, "El ID negativo debe haber sido sustituido por el ID real positivo")
            assertFalse(p.pendiente, "El producto ya no debe estar marcado como pendiente")
            assertEquals(0L, local.contarPendientes(), "No deben quedar pendientes en la base")
        } finally {
            driver.close()
        }
    }

    /**
     * Caso 4. La descarga no borra pendientes:
     * Ejecuta realmente una descarga (GET) y la actualización local. Comprueba que un pendiente local
     * sobrevive Y que los productos descargados del servidor se incorporan correctamente a SQLite.
     */
    @Test
    fun caso4_descarga_no_borra_pendientes_locales_e_incorpora_productos_del_servidor() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)

            // 1. Guardar un producto creado offline (ID negativo, pendiente = true)
            val pendienteLocal = local.guardarPendiente(
                Producto(
                    id = 0,
                    nombre = "Producto Offline Pendiente",
                    precio = 18.50,
                    stock = 12,
                    activo = true,
                    categoria = "Inyectables",
                    categoriaId = 3L
                )
            )
            assertTrue(pendienteLocal.id < 0L)
            assertTrue(pendienteLocal.pendiente)

            // 2. MockEngine que responde exitosamente al GET con productos del servidor
            var getEjecutado = false
            val client = mockHttpClient { request ->
                assertEquals(HttpMethod.Get, request.method)
                getEjecutado = true
                respond(
                    content = """
                        {
                          "contenido": [
                            {
                              "id": 10,
                              "nombre": "Amoxicilina Remota",
                              "precio": 8.00,
                              "stock": 45,
                              "estado": true,
                              "categoriaId": 1,
                              "categoriaNombre": "Antibióticos"
                            },
                            {
                              "id": 20,
                              "nombre": "Omeprazol Remoto",
                              "precio": 4.50,
                              "stock": 60,
                              "estado": true,
                              "categoriaId": 2,
                              "categoriaNombre": "Antiácidos"
                            }
                          ],
                          "pagina": 0,
                          "tamanio": 50,
                          "totalElementos": 2,
                          "totalPaginas": 1,
                          "ultima": true
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // 3. Ejecutar descarga directa del catálogo
            repo.descargarCatalogo()
            assertTrue(getEjecutado, "Se debió ejecutar la petición GET de descarga")

            // 4. Comprobar que en SQLite existen 3 productos: el pendiente local + los 2 descargados
            val productos = repo.observar().first()
            assertEquals(3, productos.size, "Debe contener el producto pendiente y los 2 descargados")

            // 5. Verificar que el producto pendiente local SOBREVIVIÓ intacto
            val encontradoPendiente = productos.firstOrNull { it.id == pendienteLocal.id }
            assertNotNull(encontradoPendiente, "El producto pendiente offline debe sobrevivir en SQLite")
            assertTrue(encontradoPendiente.pendiente)
            assertEquals("Producto Offline Pendiente", encontradoPendiente.nombre)

            // 6. Verificar que los productos descargados se incorporaron con ID positivo y pendiente = false
            val remotos = productos.filter { it.id > 0L }
            assertEquals(2, remotos.size)
            assertTrue(remotos.all { !it.pendiente })
            assertTrue(remotos.any { it.id == 10L && it.nombre == "Amoxicilina Remota" })
            assertTrue(remotos.any { it.id == 20L && it.nombre == "Omeprazol Remoto" })
            assertEquals(1L, local.contarPendientes(), "La cantidad de pendientes en la base sigue siendo 1")
        } finally {
            driver.close()
        }
    }

    /**
     * Caso 5. Identificadores sin colisión:
     * Comprueba que un producto local con ID negativo y uno del servidor con ID positivo
     * conviven sin sobrescribirse. Verifica cantidad, IDs y contenido de ambos.
     */
    @Test
    fun caso5_identificadores_sin_colision_entre_productos_locales_negativos_y_remotos_positivos() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val client = mockHttpClient { respondError(HttpStatusCode.NotFound) }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // 1. Guardar producto remoto con ID positivo en la base local (sincronizado)
            local.reemplazarSincronizados(
                listOf(
                    Producto(
                        id = 1L,
                        nombre = "Aspirina 100mg (Remoto)",
                        precio = 1.50,
                        stock = 200,
                        activo = true,
                        categoria = "Cardiología",
                        categoriaId = 4L,
                        pendiente = false
                    )
                )
            )

            // 2. Guardar producto local con ID negativo (pendiente)
            val localPendiente = local.guardarPendiente(
                Producto(
                    id = 0,
                    nombre = "Salbutamol Inhalador (Local)",
                    precio = 22.00,
                    stock = 15,
                    activo = true,
                    categoria = "Respiratorio",
                    categoriaId = 5L
                )
            )

            // 3. Verificar que el ID local es estrictamente negativo (-1)
            assertTrue(localPendiente.id < 0L, "El ID local debe ser negativo: ${localPendiente.id}")

            // 4. Observar la base de datos completa: ambos deben convivir sin colisión
            val todos = repo.observar().first()
            assertEquals(2, todos.size, "Deben existir exactamente 2 productos en la base")

            // 5. Verificar contenido íntegro de cada producto
            val prodRemoto = todos.first { it.id == 1L }
            assertEquals("Aspirina 100mg (Remoto)", prodRemoto.nombre)
            assertEquals(1.50, prodRemoto.precio)
            assertEquals(200, prodRemoto.stock)
            assertFalse(prodRemoto.pendiente)

            val prodLocal = todos.first { it.id == localPendiente.id }
            assertEquals("Salbutamol Inhalador (Local)", prodLocal.nombre)
            assertEquals(22.00, prodLocal.precio)
            assertEquals(15, prodLocal.stock)
            assertTrue(prodLocal.pendiente)

            // 6. Verificar que consultar por ID retorna la instancia exacta correspondiente
            val recuperadoRemoto = repo.obtener(1L)
            assertEquals(1L, recuperadoRemoto.id)
            val recuperadoLocal = repo.obtener(localPendiente.id)
            assertEquals(localPendiente.id, recuperadoLocal.id)
        } finally {
            driver.close()
        }
    }

    /**
     * Caso 6. Sincronizar dos veces no duplica:
     * Ejecutar dos sincronizaciones completas sucesivas deja exactamente la misma cantidad de filas
     * y los mismos IDs finales. Además, una creación ya confirmada no repite su POST.
     */
    @Test
    fun caso6_sincronizar_dos_veces_no_duplica_registros_ni_repite_post_de_creacion_confirmada() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            local.guardarPendiente(
                Producto(
                    id = 0,
                    nombre = "Naproxeno 550mg",
                    precio = 6.0,
                    stock = 15,
                    categoriaId = 1L
                )
            )

            var postCount = 0
            var getCount = 0

            val client = mockHttpClient { request ->
                if (request.method == HttpMethod.Post) {
                    postCount++
                    respond(
                        content = """{"id": 201, "nombre": "Naproxeno 550mg", "precio": 6.0, "stock": 15, "estado": true, "categoriaId": 1}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    getCount++
                    respond(
                        content = """{"contenido": [{"id": 201, "nombre": "Naproxeno 550mg", "precio": 6.0, "stock": 15, "estado": true, "categoriaId": 1}], "pagina": 0, "tamanio": 50, "totalElementos": 1, "totalPaginas": 1, "ultima": true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // Primera sincronización completa
            repo.sincronizar()
            assertEquals(1, postCount, "El primer POST debe ejecutarse")
            assertEquals(1, getCount, "El primer GET debe ejecutarse")

            val listaPrimeraSync = repo.observar().first()
            assertEquals(1, listaPrimeraSync.size)
            val idsPrimeraSync = listaPrimeraSync.map { it.id }

            // Segunda sincronización inmediata
            repo.sincronizar()
            assertEquals(1, postCount, "La segunda sincronización NO debe repetir el POST de creaciones ya confirmadas")
            assertEquals(2, getCount, "El segundo GET se ejecuta normalmente para refrescar")

            val listaSegundaSync = repo.observar().first()
            assertEquals(1, listaSegundaSync.size, "La cantidad de registros NO debe duplicarse")
            assertEquals(idsPrimeraSync, listaSegundaSync.map { it.id }, "Los IDs finales deben permanecer idénticos")
            assertFalse(listaSegundaSync[0].pendiente)
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // PRODUCTO 3: PRUEBAS DE ANTIGÜEDAD Y METADATOS DE SINCRONIZACIÓN
    // =========================================================================

    /**
     * Producto 3.1: Caso sin sincronización previa retorna "Nunca sincronizado".
     */
    @Test
    fun producto3_nunca_sincronizado_muestra_etiqueta_correspondiente() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val client = mockHttpClient { respondError(HttpStatusCode.NotFound) }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            val timestamp = repo.obtenerUltimaSincronizacion()
            assertNull(timestamp, "En una base nueva el timestamp de sincronización debe ser null")

            val texto = AntiguedadSincronizacion.formatear(timestamp)
            assertEquals("Nunca sincronizado", texto)
        } finally {
            driver.close()
        }
    }

    /**
     * Producto 3.2: Sincronización exitosa guarda la marca de tiempo persistentemente.
     */
    @Test
    fun producto3_sincronizacion_exitosa_guarda_marca_de_tiempo_persistentemente() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val client = mockHttpClient {
                respond(
                    content = """{"contenido": [], "pagina": 0, "tamanio": 50, "totalElementos": 0, "totalPaginas": 0, "ultima": true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            assertNull(repo.obtenerUltimaSincronizacion())

            // Sincronizar exitosamente
            repo.sincronizar()

            val timestamp = repo.obtenerUltimaSincronizacion()
            assertNotNull(timestamp)
            assertTrue(timestamp > 0L, "El timestamp debe ser un valor positivo en segundos epoch")

            // Formatear justo después de sincronizar
            val texto = AntiguedadSincronizacion.formatear(timestamp, ahoraEpochSegundos = timestamp + 10)
            assertEquals("Actualizado hace unos instantes", texto)
        } finally {
            driver.close()
        }
    }

    /**
     * Producto 3.3: Un fallo en la sincronización NO actualiza la marca anterior.
     */
    @Test
    fun producto3_fallo_en_sincronizacion_no_actualiza_marca_anterior() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            val marcaPrevia = 1700000000L
            local.guardarUltimaSincronizacion(marcaPrevia)

            // Mock que falla en la descarga
            val client = mockHttpClient {
                throw IOException("Fallo de red simulado")
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            assertFailsWith<ErrorApi.SinConexion> {
                repo.sincronizar()
            }

            // La marca debe conservarse exactamente igual a la previa
            assertEquals(marcaPrevia, repo.obtenerUltimaSincronizacion(), "Un fallo debe preservar la marca previa sin actualizarla")
        } finally {
            driver.close()
        }
    }

    /**
     * Producto 3.4: Formato de antigüedad calcula minutos, horas y días con reloj inyectable.
     */
    @Test
    fun producto3_formato_antiguedad_calcula_minutos_horas_y_dias_con_reloj_inyectable() {
        val base = 1700000000L

        // Casos nulos o no positivos
        assertEquals("Nunca sincronizado", AntiguedadSincronizacion.formatear(null, ahoraEpochSegundos = base))
        assertEquals("Nunca sincronizado", AntiguedadSincronizacion.formatear(0L, ahoraEpochSegundos = base))
        assertEquals("Nunca sincronizado", AntiguedadSincronizacion.formatear(-10L, ahoraEpochSegundos = base))

        // Menos de 60 segundos
        assertEquals("Actualizado hace unos instantes", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 30))
        assertEquals("Actualizado hace unos instantes", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 59))

        // Minutos (singular y plural)
        assertEquals("Actualizado hace 1 minuto", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 60))
        assertEquals("Actualizado hace 1 minuto", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 119))
        assertEquals("Actualizado hace 2 minutos", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 120))
        assertEquals("Actualizado hace 45 minutos", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + (45 * 60)))

        // Horas (singular y plural)
        assertEquals("Actualizado hace 1 hora", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 3600))
        assertEquals("Actualizado hace 1 hora", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 7199))
        assertEquals("Actualizado hace 2 horas", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 7200))
        assertEquals("Actualizado hace 18 horas", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + (18 * 3600)))

        // Días (singular y plural)
        assertEquals("Actualizado hace 1 día", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + 86400))
        assertEquals("Actualizado hace 1 día", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + (86400 + 3600)))
        assertEquals("Actualizado hace 2 días", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + (2 * 86400)))
        assertEquals("Actualizado hace 7 días", AntiguedadSincronizacion.formatear(base, ahoraEpochSegundos = base + (7 * 86400)))
    }

    /**
     * Producto 3.5: Persistencia de la marca de tiempo en SQLite al reabrir la base.
     */
    @Test
    fun producto3_persistencia_de_marca_de_tiempo_al_reabrir_base_de_datos() {
        val driver = crearDriverPruebas()
        try {
            val db1 = PharmaDb(driver)
            val local1 = ProductoLocal(db1)
            val timestampGuardado = 1729526400L // 21 Octubre 2026

            local1.guardarUltimaSincronizacion(timestampGuardado)
            assertEquals(timestampGuardado, local1.obtenerUltimaSincronizacion())

            // Simular nueva instancia de PharmaDb sobre el mismo driver
            val db2 = PharmaDb(driver)
            val local2 = ProductoLocal(db2)
            assertEquals(timestampGuardado, local2.obtenerUltimaSincronizacion(), "El timestamp debe persistir en la tabla metadata_sincronizacion")
        } finally {
            driver.close()
        }
    }

    // =========================================================================
    // REGRESIONES CRÍTICAS PRESERVADAS (S10 Y S11)
    // =========================================================================

    @Test
    fun regresion_POST_correcto_seguido_de_fallo_de_GET_conserva_la_confirmacion() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            local.guardarPendiente(Producto(id = 0, nombre = "Loratadina", precio = 3.5, stock = 25))

            var postLlamado = false
            val client = mockHttpClient { request ->
                if (request.method == HttpMethod.Post) {
                    postLlamado = true
                    respond(
                        content = """{"id": 55, "nombre": "Loratadina", "precio": 3.5, "stock": 25, "estado": true, "categoriaId": 1}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    // Falla la descarga tras confirmar el POST
                    throw IOException("Caída de conexión al descargar catálogo")
                }
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // Falla la sincronización debido al GET
            assertFailsWith<ErrorApi.SinConexion> {
                repo.sincronizar()
            }

            assertTrue(postLlamado)
            // La confirmación local DEBE haberse persistido: ID 55, pendiente = false
            val productos = repo.observar().first()
            assertEquals(1, productos.size)
            assertEquals(55L, productos[0].id)
            assertFalse(productos[0].pendiente)
            assertEquals(0L, local.contarPendientes(), "No debe volver a marcarse como pendiente")
        } finally {
            driver.close()
        }
    }

    @Test
    fun regresion_catalogo_paginado_descarga_todas_las_paginas() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)

            val client = mockHttpClient { request ->
                val url = request.url.toString()
                if (url.contains("pagina=0")) {
                    respond(
                        content = """
                            {
                              "contenido": [
                                {"id": 10, "nombre": "Prod P0-1", "precio": 5.0, "stock": 10, "estado": true, "categoriaId": 1},
                                {"id": 11, "nombre": "Prod P0-2", "precio": 6.0, "stock": 10, "estado": true, "categoriaId": 1}
                              ],
                              "pagina": 0, "tamanio": 2, "totalElementos": 4, "totalPaginas": 2, "ultima": false
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respond(
                        content = """
                            {
                              "contenido": [
                                {"id": 12, "nombre": "Prod P1-1", "precio": 7.0, "stock": 10, "estado": true, "categoriaId": 1},
                                {"id": 13, "nombre": "Prod P1-2", "precio": 8.0, "stock": 10, "estado": true, "categoriaId": 1}
                              ],
                              "pagina": 1, "tamanio": 2, "totalElementos": 4, "totalPaginas": 2, "ultima": true
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            repo.sincronizar()

            val productos = repo.observar().first()
            assertEquals(4, productos.size, "Debe contener los productos de ambas páginas")
        } finally {
            driver.close()
        }
    }

    @Test
    fun regresion_fallos_401_403_no_se_silencian_como_conectividad() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            local.guardarPendiente(Producto(id = 0, nombre = "Test 401", precio = 1.0, stock = 1))

            val client401 = mockHttpClient {
                respond(
                    content = """{"message": "Full authentication is required"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val repo401 = ProductoRepositorioOffline(local, ProductoApi(client401))

            val err401 = assertFailsWith<ErrorApi.NoAutorizado> {
                repo401.sincronizar()
            }
            assertTrue(err401.message.contains("authentication", ignoreCase = true) || err401.message.isNotBlank())

            val client403 = mockHttpClient {
                respond(
                    content = """{"message": "Access Denied"}""",
                    status = HttpStatusCode.Forbidden,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val repo403 = ProductoRepositorioOffline(local, ProductoApi(client403))

            val err403 = assertFailsWith<ErrorApi.Prohibido> {
                repo403.sincronizar()
            }
            assertTrue(err403.message.contains("Denied", ignoreCase = true) || err403.message.isNotBlank())
        } finally {
            driver.close()
        }
    }
}
