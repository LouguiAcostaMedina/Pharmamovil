package pe.edu.upeu.pharmamobile.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobile.domain.model.Producto

interface ProductoRepository {
    /** La interfaz observa el Flow de la base local como única fuente de verdad */
    fun observar(): Flow<List<Producto>>

    /** Sube los productos pendientes y luego descarga el catálogo remoto */
    suspend fun sincronizar()

    /** Guarda localmente como pendiente y luego intenta sincronizar */
    suspend fun registrar(p: Producto): Producto

    /** Retorna la lista actual de productos */
    suspend fun listar(): List<Producto>

    /** Obtiene un producto por su ID */
    suspend fun obtener(id: Long): Producto

    /** Actualiza un producto existente */
    suspend fun actualizar(p: Producto): Producto

    /** Elimina un producto por su ID */
    suspend fun eliminar(id: Long)

    /** Retorna el timestamp epoch en segundos de la última sincronización completa exitosa, o null si nunca sincronizó */
    fun obtenerUltimaSincronizacion(): Long? = null

    /** Descarga el catálogo remoto e incorpora los productos en la base local sin tocar los pendientes */
    suspend fun descargarCatalogo() {}
}
