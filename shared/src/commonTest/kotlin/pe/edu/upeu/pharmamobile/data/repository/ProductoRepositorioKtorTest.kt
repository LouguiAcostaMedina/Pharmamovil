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
                url("https://api.escuelajs.co/api/v1/")
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
            assertEquals("https://api.escuelajs.co/api/v1/products?limit=10", request.url.toString())
            respond(
                content = """
                    [
                      {
                        "id": 2,
                        "title": "Classic Red Pullover Hoodie",
                        "price": 10.0,
                        "description": "Elevate your casual wardrobe",
                        "category": {
                          "id": 1,
                          "name": "Clothes"
                        },
                        "images": [
                          "https://i.imgur.com/1twoaDy.jpeg"
                        ],
                        "creationAt": "2026-09-30T09:27:26.000Z",
                        "updatedAt": "2026-09-30T09:27:26.000Z"
                      }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertEquals(1, productos.size)
        val producto = productos.first()
        assertEquals(2L, producto.id)
        assertEquals("Classic Red Pullover Hoodie", producto.nombre)
        assertEquals(10.0, producto.precio)
        assertEquals("Elevate your casual wardrobe", producto.descripcion)
        assertEquals("Clothes", producto.categoria)
        assertEquals("https://i.imgur.com/1twoaDy.jpeg", producto.imagen)
    }

    @Test
    fun debe_manejar_lista_vacia() = runTest {
        val mockEngine = mockHttpClient {
            respond(
                content = "[]",
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
                    [
                      {
                        "id": 3,
                        "title": "Shirt",
                        "price": 15.0
                      }
                    ]
                """.trimIndent(), // Missing description, category, images
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = ProductoRepositorioKtor(mockEngine)
        val productos = repository.listar()

        assertEquals(1, productos.size)
        val producto = productos.first()
        assertEquals("", producto.descripcion)
        assertEquals("", producto.categoria)
        assertEquals("", producto.imagen)
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
