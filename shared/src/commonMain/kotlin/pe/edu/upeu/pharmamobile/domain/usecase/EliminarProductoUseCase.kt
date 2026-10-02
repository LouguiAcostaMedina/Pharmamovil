package pe.edu.upeu.pharmamobile.domain.usecase

import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class EliminarProductoUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(id: Long) {
        repository.eliminar(id)
    }
}
