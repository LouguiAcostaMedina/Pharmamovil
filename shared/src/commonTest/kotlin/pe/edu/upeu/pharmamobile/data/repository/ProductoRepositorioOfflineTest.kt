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

    @Test
    fun con_red_sincronizar_escribe_en_la_base_y_el_Flow_emite_datos() = runTest {
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
            assertEquals("Ibuprofeno 400mg", productos[0].nombre) // Orden alfabetico
            assertEquals("Paracetamol 500mg", productos[1].nombre)
            assertEquals(1L, productos[1].id)
            assertFalse(productos[0].pendiente)
            assertFalse(productos[1].pendiente)
        } finally {
            driver.close()
        }
    }

    @Test
    fun sin_red_registrar_conserva_un_producto_con_ID_negativo_y_pendiente_visible_desde_el_Flow() = runTest {
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

    @Test
    fun reconexion_primero_POST_despues_GET_ID_real_pendiente_eliminado_y_una_sola_representacion_local() = runTest {
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

            // 2. MockEngine que registra la secuencia de llamadas HTTP
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

    @Test
    fun regresion_repetir_sincronizacion_no_reenvia_lo_ya_confirmado() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)
            local.guardarPendiente(Producto(id = 0, nombre = "Naproxeno 550mg", precio = 6.0, stock = 15))

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

            // Primera sincronización
            repo.sincronizar()
            assertEquals(1, postCount)
            assertEquals(1, getCount)

            // Segunda sincronización inmediata: NO debe volver a enviar POST
            repo.sincronizar()
            assertEquals(1, postCount, "La segunda sincronización NO debe repetir el POST de creaciones ya confirmadas")
            assertEquals(2, getCount)
        } finally {
            driver.close()
        }
    }

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
    fun regresion_descarga_no_borra_pendientes() = runTest {
        val (driver, db) = crearBaseEnMemoria()
        try {
            val local = ProductoLocal(db)

            // Guardar producto sincronizado previo
            local.reemplazarSincronizados(listOf(Producto(id = 1, nombre = "Prod Servidor", precio = 10.0, stock = 5)))
            // Guardar producto pendiente
            val pendiente = local.guardarPendiente(Producto(id = 0, nombre = "Prod Pendiente Offline", precio = 20.0, stock = 2))

            // Mock de descarga con otro producto del servidor
            val client = mockHttpClient { request ->
                if (request.method == HttpMethod.Post) {
                    throw IOException("Sin red al intentar subir")
                } else {
                    respond(
                        content = """
                            {
                              "contenido": [
                                {"id": 1, "nombre": "Prod Servidor", "precio": 10.0, "stock": 5, "estado": true, "categoriaId": 1},
                                {"id": 2, "nombre": "Prod Nuevo Servidor", "precio": 15.0, "stock": 8, "estado": true, "categoriaId": 1}
                              ],
                              "pagina": 0, "tamanio": 50, "totalElementos": 2, "totalPaginas": 1, "ultima": true
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))

            // Si falla la subida, la sincronización se aborta y NO borra el pendiente
            assertFailsWith<ErrorApi.SinConexion> {
                repo.sincronizar()
            }

            val productos = repo.observar().first()
            val ids = productos.map { it.id }
            assertTrue(ids.contains(pendiente.id), "El producto pendiente offline debe preservarse intacto")
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
