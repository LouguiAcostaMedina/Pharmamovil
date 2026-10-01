package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductoRepositorioKtorTest {

    private fun mockHttpClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
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
    fun debe_mapear_correctamente_lista_de_DTO_a_Dominio_con_HTTP_200() = runTest {
        val mockEngine = mockHttpClient { request ->
            assertTrue(request.url.toString().contains("productos?pagina=0&tamanio=20"))
            respond(
                content = """
                    {
                      "contenido": [
                        {
                          "id": 1,
                          "nombre": "Paracetamol 500mg",
                          "precio": 5.5,
                          "stock": 100,
                          "estado": true,
                          "categoriaId": 1,
                          "categoriaNombre": "Analgésicos"
                        }
                      ],
                      "pagina": 0,
                      "tamanio": 20,
                      "totalElementos": 1,
                      "totalPaginas": 1,
                      "ultima": true
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertEquals(1, productos.size)
        val producto = productos.first()
        assertEquals(1L, producto.id)
        assertEquals("Paracetamol 500mg", producto.nombre)
        assertEquals(5.5, producto.precio)
        assertEquals("", producto.descripcion) // Not provided by PharmaSoft
        assertEquals("Analgésicos", producto.categoria)
    }

    @Test
    fun debe_manejar_lista_vacia() = runTest {
        val mockEngine = mockHttpClient {
            respond(
                content = """
                    {
                      "contenido": [],
                      "pagina": 0,
                      "tamanio": 20,
                      "totalElementos": 0,
                      "totalPaginas": 0,
                      "ultima": true
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertTrue(productos.isEmpty())
    }

    @Test
    fun debe_retornar_vacio_y_manejar_campos_opcionales() = runTest {
        val mockEngine = mockHttpClient {
            respond(
                content = """
                    {
                      "contenido": [
                        {
                          "id": 3,
                          "nombre": "Ibuprofeno",
                          "precio": 15.0,
                          "stock": 50,
                          "estado": true
                        }
                      ],
                      "pagina": 0,
                      "tamanio": 20,
                      "totalElementos": 1,
                      "totalPaginas": 1,
                      "ultima": true
                    }
                """.trimIndent(), // Missing category fields
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertEquals(1, productos.size)
        val producto = productos.first()
        assertEquals("", producto.categoria)
    }

    @Test
    fun debe_retornar_lista_vacia_en_error_400_o_500() = runTest {
        val mockEngine = mockHttpClient {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError
            )
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertTrue(productos.isEmpty())
    }

    @Test
    fun debe_retornar_lista_vacia_en_error_de_conexion() = runTest {
        val mockEngine = mockHttpClient {
            throw Exception("Connection Error")
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertTrue(productos.isEmpty())
    }
}
