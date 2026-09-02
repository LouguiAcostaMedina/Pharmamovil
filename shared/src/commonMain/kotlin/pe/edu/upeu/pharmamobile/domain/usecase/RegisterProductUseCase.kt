package pe.edu.upeu.pharmamobile.domain.usecase

import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.domain.repository.ProductRepository

class RegisterProductUseCase(private val repository: ProductRepository) {
    operator fun invoke(nombre: String, precioStr: String, stockStr: String): Result<Producto> {
        if (nombre.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingrese nombre"))
        }
        
        val precio = precioStr.toDoubleOrNull()
        if (precio == null || precio <= 0.0) {
            return Result.failure(IllegalArgumentException("Ingrese precio válido"))
        }
        
        val stock = stockStr.toIntOrNull()
        if (stock == null || stock < 0) {
            return Result.failure(IllegalArgumentException("Stock no puede ser negativo"))
        }
        
        val producto = Producto(
            id = System.currentTimeMillis(),
            nombre = nombre.trim(),
            precio = precio,
            stock = stock
        )
        
        val added = repository.addProduct(producto)
        return if (added) {
            Result.success(producto)
        } else {
            Result.failure(RuntimeException("Error al registrar producto"))
        }
    }
}
