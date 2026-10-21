package pe.edu.upeu.pharmamobile.data.local

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class AlmacenSeguroDesktop : AlmacenSeguro {

    private var tokenEnMemoria: String? = null
    private var datosEnMemoria: DatosSesionSegura? = null

    private val archivoSesion: File by lazy {
        val dir = File(System.getProperty("user.home"), ".pharmamobile")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "session_secure.properties")
    }

    override fun guardarToken(token: String) {
        tokenEnMemoria = token
        guardarEnArchivo()
    }

    override fun leerToken(): String? {
        if (tokenEnMemoria == null && archivoSesion.exists()) {
            cargarDeArchivo()
        }
        return tokenEnMemoria
    }

    override fun guardarDatosSesion(usuario: String, nombreCompleto: String, permisos: Set<String>) {
        datosEnMemoria = DatosSesionSegura(usuario, nombreCompleto, permisos)
        guardarEnArchivo()
    }

    override fun leerDatosSesion(): DatosSesionSegura? {
        if (datosEnMemoria == null && archivoSesion.exists()) {
            cargarDeArchivo()
        }
        return datosEnMemoria
    }

    override fun borrarToken() {
        tokenEnMemoria = null
        datosEnMemoria = null
        if (archivoSesion.exists()) {
            archivoSesion.delete()
        }
    }

    private fun guardarEnArchivo() {
        try {
            val props = Properties()
            tokenEnMemoria?.let { props.setProperty("jwt", it) }
            datosEnMemoria?.let {
                props.setProperty("usuario", it.usuario)
                props.setProperty("nombreCompleto", it.nombreCompleto)
                props.setProperty("permisos", it.permisos.joinToString(","))
            }
            FileOutputStream(archivoSesion).use { out ->
                props.store(out, "PharmaMobile Desktop Local Session Store")
            }
        } catch (_: Exception) {
        }
    }

    private fun cargarDeArchivo() {
        try {
            if (!archivoSesion.exists()) return
            val props = Properties()
            FileInputStream(archivoSesion).use { `in` ->
                props.load(`in`)
            }
            tokenEnMemoria = props.getProperty("jwt")
            val usuario = props.getProperty("usuario")
            if (usuario != null) {
                val nombreCompleto = props.getProperty("nombreCompleto") ?: usuario
                val permisosRaw = props.getProperty("permisos") ?: ""
                val permisos = if (permisosRaw.isBlank()) emptySet() else permisosRaw.split(",").map { it.trim() }.toSet()
                datosEnMemoria = DatosSesionSegura(usuario, nombreCompleto, permisos)
            }
        } catch (_: Exception) {
        }
    }
}
