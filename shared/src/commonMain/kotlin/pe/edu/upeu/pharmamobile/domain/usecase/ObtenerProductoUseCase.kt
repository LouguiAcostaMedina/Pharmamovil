package pe.edu.upeu.pharmamobile.domain.usecase

import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class ObtenerProductoUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(id: Long): Producto {
        return repository.obtener(id)
    }
}
