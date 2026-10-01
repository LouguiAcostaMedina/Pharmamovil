package pe.edu.upeu.pharmamobile.domain.usecase

import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class ListarProductosUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(): List<Producto> {
        return repository.listar()
    }
}
