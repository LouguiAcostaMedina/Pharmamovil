package pe.edu.upeu.pharmamobile.data.local

data class DatosSesionSegura(
    val usuario: String,
    val nombreCompleto: String,
    val permisos: Set<String>
)

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
    fun guardarDatosSesion(usuario: String, nombreCompleto: String, permisos: Set<String>) {}
    fun leerDatosSesion(): DatosSesionSegura? = null
}
