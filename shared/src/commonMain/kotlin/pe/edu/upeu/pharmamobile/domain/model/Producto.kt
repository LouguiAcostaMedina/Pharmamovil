package pe.edu.upeu.pharmamobile.domain.model

data class Producto(
    val id: Long = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int = 0,
    val activo: Boolean = true,
    val descripcion: String = "",
    val imagen: String = "",
    val categoria: String = "",
    val categoriaId: Long? = null,
    val pendiente: Boolean = false,
    val intentos: Int = 0,
    val ultimoError: String? = null,
    val falloPermanente: Boolean = false,
    val proximoReintentoEpochMillis: Long = 0L,
    val claveIdempotencia: String? = null
) {
    val requiereReposicion: Boolean get() = stock <= STOCK_MINIMO
    val esFallida: Boolean get() = pendiente && (falloPermanente || intentos >= MAX_INTENTOS)

    companion object {
        const val STOCK_MINIMO = 5
        const val MAX_INTENTOS = 5
    }
}
