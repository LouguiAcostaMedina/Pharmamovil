package pe.edu.upeu.pharmamobile.domain.repository

import pe.edu.upeu.pharmamobile.domain.model.Producto

interface ProductRepository {
    fun getProducts(): List<Producto>
    fun addProduct(producto: Producto): Boolean
}
