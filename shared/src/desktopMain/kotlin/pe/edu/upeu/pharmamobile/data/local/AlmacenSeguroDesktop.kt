package pe.edu.upeu.pharmamobile.data.local

/**
 * Implementación de almacenamiento en memoria para el entorno Desktop (JVM).
 *
 * NOTA DE SEGURIDAD Y DEPURACIÓN:
 * Esta implementación almacena el token JWT exclusivamente en memoria durante la sesión
 * del proceso Desktop para depuración y pruebas locales. No persiste credenciales en texto plano
 * en el sistema de archivos ni en archivos de preferencias. No debe presentarse como sustituto
 * de los almacenes seguros nativos de dispositivos móviles (Android Keystore / iOS Keychain).
 */
class AlmacenSeguroDesktop : AlmacenSeguro {

    private var tokenEnMemoria: String? = null

    override fun guardarToken(token: String) {
        tokenEnMemoria = token
    }

    override fun leerToken(): String? {
        return tokenEnMemoria
    }

    override fun borrarToken() {
        tokenEnMemoria = null
    }
}
