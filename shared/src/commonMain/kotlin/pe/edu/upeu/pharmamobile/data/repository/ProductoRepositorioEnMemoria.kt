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
}
