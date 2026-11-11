package pe.edu.upeu.pharmamobile.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pe.edu.upeu.pharmamobile.domain.platform.NotificadorSincronizacion

class NotificadorSincronizacionAndroid(
    private val context: Context
) : NotificadorSincronizacion {

    companion object {
        const val CANAL_ID = "sync_channel"
        const val CANAL_NOMBRE = "Sincronización PharmaMobile"
        const val NOTIFICACION_ID = 1001
    }

    init {
        crearCanal()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de subida y sincronización de datos"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(canal)
        }
    }

    override fun notificarResultado(titulo: String, mensaje: String) {
        val managerCompat = NotificationManagerCompat.from(context)
        if (managerCompat.areNotificationsEnabled()) {
            val notification = NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            try {
                managerCompat.notify(NOTIFICACION_ID, notification)
            } catch (_: SecurityException) {
                // Silencioso si no cuenta con permiso en Android 13+
            }
        }
    }
}

