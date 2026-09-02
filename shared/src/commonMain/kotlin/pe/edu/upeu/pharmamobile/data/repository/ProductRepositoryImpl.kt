package pe.edu.upeu.pharmamobile.data.repository

import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductRepository

class ProductRepositoryImpl : ProductRepository {
    private val products = mutableListOf<Producto>()

    override fun getProducts(): List<Producto> {
        return products.toList()
    }

    override fun addProduct(producto: Producto): Boolean {
        return products.add(producto)
    }
}
