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

    override suspend fun listar(): List<Producto> {
        return try {
            val response: List<ProductoDto> = client.get("products") {
                url {
                    parameters.append("limit", "10")
                }
            }.body()

            response.map { dto ->
                Producto(
                    id = dto.id.toLong(),
                    nombre = dto.title,
                    precio = dto.price,
                    descripcion = dto.description,
                    imagen = dto.images.firstOrNull() ?: "",
                    categoria = dto.category?.name ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
