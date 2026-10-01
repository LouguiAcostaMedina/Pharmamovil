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
            val response: pe.edu.upeu.pharmamobile.data.model.PaginaResponse<ProductoDto> = client.get("$baseUrl/productos") {
                url {
                    parameters.append("pagina", "0")
                    parameters.append("tamanio", "20")
                    parameters.append("ordenarPor", "id")
                    parameters.append("direccion", "asc")
                }
            }.body()

            response.contenido.map { dto ->
                Producto(
                    id = dto.id,
                    nombre = dto.nombre,
                    precio = dto.precio,
                    descripcion = "", // Not available in PharmaSoft currently
                    imagen = "", // Not available
                    categoria = dto.categoriaNombre ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
