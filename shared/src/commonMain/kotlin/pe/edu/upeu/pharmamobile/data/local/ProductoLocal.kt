package pe.edu.upeu.pharmamobile.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobile.db.PharmaDb
import pe.edu.upeu.pharmamobile.db.Producto as ProductoEntity
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.generarClaveIdempotencia

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

    fun pendientesParaSincronizacion(ahoraMillis: Long): List<Producto> =
        consultas.listarPendientesParaSincronizacion(ahoraMillis).executeAsList()
            .map { it.aDominio() }

    fun fallidas(): List<Producto> =
        consultas.listarFallidas().executeAsList()
            .map { it.aDominio() }

    fun contarPendientes(): Long =
        consultas.contarPendientes().executeAsOne()

    fun contarFallidas(): Long =
        consultas.contarFallidas().executeAsOne()

    fun observarContadores(): Flow<Pair<Long, Long>> {
        val flowPendientes = consultas.contarPendientes().asFlow().mapToOne(dispatcher)
        val flowFallidas = consultas.contarFallidas().asFlow().mapToOne(dispatcher)
        return flowPendientes.combine(flowFallidas) { p, f -> Pair(p, f) }
    }

    fun obtenerPorId(id: Long): Producto? =
        consultas.obtenerPorId(id).executeAsOneOrNull()?.aDominio()

    fun guardarPendiente(producto: Producto): Producto {
        return consultas.transactionWithResult {
            val menor = consultas.siguienteIdLocal().executeAsOne()
            val nuevoId = minOf(menor, 0L) - 1L
            val clave = producto.claveIdempotencia ?: generarClaveIdempotencia()
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
                pendiente = 1L,
                intentos = 0L,
                ultimoError = null,
                falloPermanente = 0L,
                proximoReintentoEpochMillis = 0L,
                claveIdempotencia = clave
            )
            producto.copy(
                id = nuevoId,
                pendiente = true,
                intentos = 0,
                ultimoError = null,
                falloPermanente = false,
                proximoReintentoEpochMillis = 0L,
                claveIdempotencia = clave
            )
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
                    pendiente = 0L,
                    intentos = 0L,
                    ultimoError = null,
                    falloPermanente = 0L,
                    proximoReintentoEpochMillis = 0L,
                    claveIdempotencia = null
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
                pendiente = 0L,
                intentos = 0L,
                ultimoError = null,
                falloPermanente = 0L,
                proximoReintentoEpochMillis = 0L,
                claveIdempotencia = null
            )
        }
    }

    fun registrarFalloTransitorio(id: Long, error: String, proximoReintentoEpochMillis: Long) {
        consultas.registrarFalloTransitorio(
            ultimoError = error,
            proximoReintentoEpochMillis = proximoReintentoEpochMillis,
            id = id
        )
    }

    fun registrarFalloPermanente(id: Long, error: String) {
        consultas.registrarFalloPermanente(
            ultimoError = error,
            id = id
        )
    }

    fun rearmarReintentoManual(id: Long) {
        consultas.rearmarReintentoManual(id)
    }

    fun actualizarClaveIdempotencia(id: Long, clave: String) {
        consultas.actualizarClaveIdempotencia(
            claveIdempotencia = clave,
            id = id
        )
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
            intentos = producto.intentos.toLong(),
            ultimoError = producto.ultimoError,
            falloPermanente = if (producto.falloPermanente) 1L else 0L,
            proximoReintentoEpochMillis = producto.proximoReintentoEpochMillis,
            claveIdempotencia = producto.claveIdempotencia,
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
        pendiente = pendiente == 1L,
        intentos = intentos.toInt(),
        ultimoError = ultimoError,
        falloPermanente = falloPermanente == 1L,
        proximoReintentoEpochMillis = proximoReintentoEpochMillis,
        claveIdempotencia = claveIdempotencia
    )
}
