package pe.edu.upeu.pharmamobile.data.repository

import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductRepository

class ProductRepositoryImpl : ProductRepository {
    private val products = mutableListOf(
        Producto(id = 1, nombre = "Paracetamol", precio = 5.0, stock = 100, activo = true),
        Producto(id = 2, nombre = "Ibuprofeno", precio = 8.50, stock = 50, activo = true),
        Producto(id = 3, nombre = "Amoxicilina", precio = 12.00, stock = 5, activo = true),
        Producto(id = 4, nombre = "Loratadina", precio = 4.00, stock = 0, activo = false),
        Producto(id = 5, nombre = "Diclofenaco", precio = 6.00, stock = 3, activo = true)
    )

    override fun getProducts(): List<Producto> {
        return products.toList()
    }

    override fun addProduct(producto: Producto): Boolean {
        val nextId = (products.maxOfOrNull { it.id } ?: 0) + 1
        val nuevoProducto = producto.copy(id = nextId)
        return products.add(nuevoProducto)
    }
}
