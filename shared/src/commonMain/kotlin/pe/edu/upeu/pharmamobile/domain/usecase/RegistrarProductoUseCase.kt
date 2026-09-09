package pe.edu.upeu.pharmamobile.domain.usecase

import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository

class RegistrarProductoUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(producto: Producto): Result<Producto> {
        if (producto.nombre.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
        }
        if (producto.precio <= 0) {
            return Result.failure(IllegalArgumentException("El precio debe ser mayor a 0"))
        }
        if (producto.stock < 0) {
            return Result.failure(IllegalArgumentException("El stock no puede ser negativo"))
        }
        return try {
            val result = repository.registrar(producto)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
