package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.call.*
import io.ktor.client.request.*
import pe.edu.upeu.pharmamobile.data.model.ProductoDto
import io.ktor.client.*
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class ProductoRepositorioKtor(private val client: HttpClient) : ProductoRepository {
    override suspend fun registrar(p: Producto): Producto {
        return p.copy(id = 999)
    }

    private val baseUrl = if (pe.edu.upeu.pharmamobile.getPlatform().name.contains("Android")) {
        "http://10.0.2.2:8080/api/v1"
    } else {
        "http://localhost:8080/api/v1"
    }

    override suspend fun listar(): List<Producto> {
        return try {
            val url = "$baseUrl/productos?pagina=0&tamanio=20&ordenarPor=id&direccion=asc"
            println("=== LOG: URL solicitada: $url ===")
            
            val httpResponse = client.get("$baseUrl/productos") {
                url {
                    parameters.append("pagina", "0")
                    parameters.append("tamanio", "20")
                    parameters.append("ordenarPor", "id")
                    parameters.append("direccion", "asc")
                }
            }
            println("=== LOG: status HTTP: ${httpResponse.status} ===")
            
            val response: pe.edu.upeu.pharmamobile.data.model.PaginaResponse<ProductoDto> = httpResponse.body()
            println("=== LOG: cantidad de elementos recibidos: ${response.contenido.size} ===")

            val dominioList = response.contenido.map { dto ->
                Producto(
                    id = dto.id,
                    nombre = dto.nombre,
                    precio = dto.precio,
                    descripcion = "", // Not available in PharmaSoft currently
                    imagen = "", // Not available
                    categoria = dto.categoriaNombre ?: ""
                )
            }
            println("=== LOG: cantidad mapeada al dominio: ${dominioList.size} ===")
            dominioList
        } catch (e: Exception) {
            println("=== LOG: Exception capturada: ${e.message} ===")
            e.printStackTrace()
            emptyList()
        }
    }
}
