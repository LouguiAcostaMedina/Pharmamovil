package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pe.edu.upeu.pharmamobile.data.model.PaginatedResponse
import pe.edu.upeu.pharmamobile.data.model.ProductoDto
import pe.edu.upeu.pharmamobile.data.network.ktorHttpClient
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.getPlatform

class ProductoRepositorioKtor : ProductoRepository {
    private val baseUrl = if (getPlatform().name.contains("Android")) {
        "http://10.0.2.2:8080"
    } else {
        "http://localhost:8080"
    }

    override suspend fun registrar(p: Producto): Producto {
        // Implementar POST luego, por ahora mock
        return p.copy(id = 999)
    }

    override suspend fun listar(): List<Producto> {
        return try {
            val response: PaginatedResponse<ProductoDto> = ktorHttpClient.get("$baseUrl/api/v1/productos") {
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
                    stock = dto.stock,
                    activo = dto.estado
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
