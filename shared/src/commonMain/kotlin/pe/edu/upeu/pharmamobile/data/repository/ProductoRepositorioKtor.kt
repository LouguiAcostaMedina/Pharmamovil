package pe.edu.upeu.pharmamobile.data.repository

import io.ktor.client.call.*
import io.ktor.client.request.*
import pe.edu.upeu.pharmamobile.data.model.ProductoResponseDto
import io.ktor.client.*
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

import pe.edu.upeu.pharmamobile.data.network.ProductoApi
import pe.edu.upeu.pharmamobile.data.network.safeApiCall
import pe.edu.upeu.pharmamobile.data.model.ProductoRequestDto

class ProductoRepositorioKtor(private val api: ProductoApi) : ProductoRepository {
    
    override suspend fun listar(): List<Producto> {
        return safeApiCall {
            val response = api.listar()
            response.contenido.map { dto ->
                Producto(
                    id = dto.id,
                    nombre = dto.nombre,
                    precio = dto.precio,
                    descripcion = "",
                    imagen = "",
                    categoria = dto.categoriaNombre ?: ""
                )
            }
        }
    }

    override suspend fun obtener(id: Long): Producto {
        return safeApiCall {
            val dto = api.obtener(id)
            Producto(
                id = dto.id,
                nombre = dto.nombre,
                precio = dto.precio,
                descripcion = "",
                imagen = "",
                categoria = dto.categoriaNombre ?: ""
            )
        }
    }

    override suspend fun registrar(p: Producto): Producto {
        return safeApiCall {
            val request = ProductoRequestDto(
                nombre = p.nombre,
                precio = p.precio,
                stock = p.stock,
                estado = true, // Default
                categoriaId = 1 // Default for now
            )
            val dto = api.crear(request)
            p.copy(id = dto.id)
        }
    }

    override suspend fun actualizar(p: Producto): Producto {
        return safeApiCall {
            val request = ProductoRequestDto(
                nombre = p.nombre,
                precio = p.precio,
                stock = p.stock,
                estado = true,
                categoriaId = 1
            )
            val dto = api.actualizar(p.id, request)
            p.copy(id = dto.id)
        }
    }

    override suspend fun eliminar(id: Long) {
        safeApiCall {
            api.eliminar(id)
        }
    }
}
