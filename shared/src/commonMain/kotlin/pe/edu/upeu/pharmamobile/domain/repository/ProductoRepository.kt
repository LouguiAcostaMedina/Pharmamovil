package pe.edu.upeu.pharmamobile.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    /** Observa en tiempo real la cantidad de pendientes (Pair.first) y fallidas (Pair.second) */
    fun observarContadores(): Flow<Pair<Long, Long>> = flowOf(Pair(0L, 0L))

    /** Rearma manualmente una fila fallida para que vuelva a intentar sincronizarse */
    suspend fun reintentarFila(id: Long) {}

    /** Rearma manualmente todas las filas fallidas */
    suspend fun reintentarTodasLasFallidas() {}
}
