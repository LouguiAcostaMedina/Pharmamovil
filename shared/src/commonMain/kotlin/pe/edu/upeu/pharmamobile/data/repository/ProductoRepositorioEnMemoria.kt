package pe.edu.upeu.pharmamobile.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class ProductoRepositorioEnMemoria : ProductoRepository {
    private val productos = mutableListOf<Producto>()
    private var idCounter = 1L

    override suspend fun registrar(p: Producto): Producto {
        delay((300..800).random().toLong())
        val nuevoProducto = p.copy(id = idCounter++)
        productos.add(nuevoProducto)
        return nuevoProducto
    }

    override suspend fun listar(): List<Producto> {
        delay((300..800).random().toLong())
        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        return productos.find { it.id == id } ?: throw Exception("Not found")
    }

    override suspend fun actualizar(p: Producto): Producto {
        val index = productos.indexOfFirst { it.id == p.id }
        if (index != -1) {
            productos[index] = p
            return p
        }
        throw Exception("Not found")
    }

    override suspend fun eliminar(id: Long) {
        productos.removeAll { it.id == id }
    }
}
