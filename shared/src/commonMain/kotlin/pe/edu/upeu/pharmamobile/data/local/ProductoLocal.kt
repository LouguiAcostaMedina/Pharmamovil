package pe.edu.upeu.pharmamobile.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobile.db.PharmaDb
import pe.edu.upeu.pharmamobile.db.Producto as ProductoEntity
import pe.edu.upeu.pharmamobile.domain.model.Producto

class ProductoLocal(
    base: PharmaDb,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val consultas = base.productoQueries
    private val consultasMetadata = base.sincronizacionMetadataQueries

    fun obtenerUltimaSincronizacion(): Long? {
        return try {
            consultasMetadata.obtenerMetadata("ultima_sincronizacion").executeAsOneOrNull()?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun guardarUltimaSincronizacion(timestampEpochSegundos: Long) {
        consultasMetadata.guardarMetadata("ultima_sincronizacion", timestampEpochSegundos.toString())
    }

    fun observar(): Flow<List<Producto>> =
        consultas.listarTodos()
            .asFlow()
            .mapToList(dispatcher)
            .map { filas -> filas.map { it.aDominio() } }

    fun pendientes(): List<Producto> =
        consultas.listarPendientes().executeAsList()
            .map { it.aDominio() }

    fun contarPendientes(): Long =
        consultas.contarPendientes().executeAsOne()

    fun obtenerPorId(id: Long): Producto? =
        consultas.obtenerPorId(id).executeAsOneOrNull()?.aDominio()

    fun guardarPendiente(producto: Producto): Producto {
        return consultas.transactionWithResult {
            val menor = consultas.siguienteIdLocal().executeAsOne()
            val nuevoId = minOf(menor, 0L) - 1L
            consultas.insertar(
                id = nuevoId,
                nombre = producto.nombre,
                precio = producto.precio,
                stock = producto.stock.toLong(),
                activo = if (producto.activo) 1L else 0L,
                descripcion = producto.descripcion,
                imagen = producto.imagen,
                categoria = producto.categoria,
                categoriaId = producto.categoriaId ?: 1L,
                pendiente = 1L
            )
            producto.copy(id = nuevoId, pendiente = true)
        }
    }

    fun reemplazarSincronizados(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach { p ->
                consultas.insertar(
                    id = p.id,
                    nombre = p.nombre,
                    precio = p.precio,
                    stock = p.stock.toLong(),
                    activo = if (p.activo) 1L else 0L,
                    descripcion = p.descripcion,
                    imagen = p.imagen,
                    categoria = p.categoria,
                    categoriaId = p.categoriaId ?: 1L,
                    pendiente = 0L
                )
            }
        }
    }

    fun confirmarSubida(idTemporal: Long, confirmado: Producto) {
        consultas.transaction {
            consultas.borrarPorId(idTemporal)
            consultas.insertar(
                id = confirmado.id,
                nombre = confirmado.nombre,
                precio = confirmado.precio,
                stock = confirmado.stock.toLong(),
                activo = if (confirmado.activo) 1L else 0L,
                descripcion = confirmado.descripcion,
                imagen = confirmado.imagen,
                categoria = confirmado.categoria,
                categoriaId = confirmado.categoriaId ?: 1L,
                pendiente = 0L
            )
        }
    }

    fun actualizarLocal(producto: Producto) {
        consultas.actualizar(
            nombre = producto.nombre,
            precio = producto.precio,
            stock = producto.stock.toLong(),
            activo = if (producto.activo) 1L else 0L,
            descripcion = producto.descripcion,
            imagen = producto.imagen,
            categoria = producto.categoria,
            categoriaId = producto.categoriaId ?: 1L,
            pendiente = if (producto.pendiente) 1L else 0L,
            id = producto.id
        )
    }

    fun eliminarLocal(id: Long) {
        consultas.borrarPorId(id)
    }

    private fun ProductoEntity.aDominio(): Producto = Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock.toInt(),
        activo = activo == 1L,
        descripcion = descripcion,
        imagen = imagen,
        categoria = categoria,
        categoriaId = categoriaId,
        pendiente = pendiente == 1L
    )
}
