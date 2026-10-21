package pe.edu.upeu.pharmamobile.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {

    private val llave = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        contexto,
        "sesion_segura",
        llave,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun guardarToken(token: String) {
        prefs.edit().putString(CLAVE, token).apply()
    }

    override fun leerToken(): String? {
        return prefs.getString(CLAVE, null)
    }

    override fun guardarDatosSesion(usuario: String, nombreCompleto: String, permisos: Set<String>) {
        prefs.edit()
            .putString(KEY_USUARIO, usuario)
            .putString(KEY_NOMBRE, nombreCompleto)
            .putStringSet(KEY_PERMISOS, permisos)
            .apply()
    }

    override fun leerDatosSesion(): DatosSesionSegura? {
        val usuario = prefs.getString(KEY_USUARIO, null) ?: return null
        val nombre = prefs.getString(KEY_NOMBRE, usuario) ?: usuario
        val permisos = prefs.getStringSet(KEY_PERMISOS, emptySet()) ?: emptySet()
        return DatosSesionSegura(usuario, nombre, permisos)
    }

    override fun borrarToken() {
        prefs.edit()
            .remove(CLAVE)
            .remove(KEY_USUARIO)
            .remove(KEY_NOMBRE)
            .remove(KEY_PERMISOS)
            .apply()
    }

    private companion object {
        const val CLAVE = "jwt"
        const val KEY_USUARIO = "sesion_usuario"
        const val KEY_NOMBRE = "sesion_nombre"
        const val KEY_PERMISOS = "sesion_permisos"
    }
}
