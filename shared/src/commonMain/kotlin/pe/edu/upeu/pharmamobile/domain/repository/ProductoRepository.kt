package pe.edu.upeu.pharmamobile.domain.repository

import pe.edu.upeu.pharmamobile.domain.model.Producto

interface ProductoRepository {
    suspend fun registrar(p: Producto): Producto
    suspend fun listar(): List<Producto>
}
