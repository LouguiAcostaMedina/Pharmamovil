package pe.edu.upeu.pharmamobile.data.telemetria

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import pe.edu.upeu.pharmamobile.domain.telemetria.SanitizadorTelemetria
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria

class TelemetriaFirebaseAndroid(
    private val context: Context
) : Telemetria {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    override fun registrarEvento(nombre: String, datos: Map<String, Any>) {
        val saneados = SanitizadorTelemetria.sanearParametros(datos)
        try {
            val bundle = Bundle().apply {
                saneados.forEach { (clave, valor) ->
                    when (valor) {
                        is String -> putString(clave, valor)
                        is Int -> putInt(clave, valor)
                        is Long -> putLong(clave, valor)
                        is Double -> putDouble(clave, valor)
                        is Float -> putFloat(clave, valor)
                        is Boolean -> putBoolean(clave, valor)
                        else -> putString(clave, valor.toString())
                    }
                }
            }
            firebaseAnalytics.logEvent(nombre, bundle)
            Log.i("TelemetriaFirebase", "Evento [$nombre] enviado a Firebase: $saneados")
        } catch (e: Exception) {
            Log.w("TelemetriaFirebase", "Fallo seguro registrando telemetría: ${e.message}")
        }
    }

    override fun registrarError(error: Throwable, contexto: Map<String, String>) {
        val saneado = SanitizadorTelemetria.sanearContexto(contexto)
        try {
            val bundle = Bundle().apply {
                putString("error_mensaje", (error.message ?: "Sin mensaje").take(100))
                putString("error_tipo", error::class.simpleName ?: "Desconocido")
                saneado.forEach { (k, v) -> putString(k.take(40), v.take(100)) }
            }
            firebaseAnalytics.logEvent("app_error", bundle)
            Log.e("TelemetriaFirebase", "Error registrado en Firebase [${error.message}]: $saneado", error)
        } catch (e: Exception) {
            Log.w("TelemetriaFirebase", "Fallo seguro registrando error en telemetría: ${e.message}")
        }
    }
}
