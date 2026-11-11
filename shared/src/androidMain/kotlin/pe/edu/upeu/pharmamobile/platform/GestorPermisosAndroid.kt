package pe.edu.upeu.pharmamobile.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import pe.edu.upeu.pharmamobile.domain.permission.EstadoPermiso
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.permission.Permiso
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

class GestorPermisosAndroid(
    private val context: Context
) : GestorPermisos {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gestor_permisos_prefs", Context.MODE_PRIVATE)

    private var currentActivityRef: WeakReference<Activity>? = null
    private var launcherCallback: ((Permiso, (Boolean) -> Unit) -> Unit)? = null

    fun vincularActividad(activity: Activity, launcher: (Permiso, (Boolean) -> Unit) -> Unit) {
        currentActivityRef = WeakReference(activity)
        launcherCallback = launcher
    }

    fun desvincularActividad() {
        currentActivityRef = null
        launcherCallback = null
    }

    private fun permisoAManifest(permiso: Permiso): String? = when (permiso) {
        Permiso.NOTIFICACIONES -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            }
        }
        Permiso.CAMARA -> Manifest.permission.CAMERA
        Permiso.UBICACION -> Manifest.permission.ACCESS_FINE_LOCATION
    }

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        if (permiso == Permiso.NOTIFICACIONES) {
            val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!notificationsEnabled) {
                val yaSolicitado = prefs.getBoolean("solicitado_${permiso.name}", false)
                if (!yaSolicitado) return EstadoPermiso.Denegado
                val activity = currentActivityRef?.get()
                val manifestPerm = permisoAManifest(permiso)
                return if (activity != null && manifestPerm != null) {
                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPerm)
                    if (showRationale) EstadoPermiso.Denegado else EstadoPermiso.DenegadoParaSiempre
                } else {
                    EstadoPermiso.DenegadoParaSiempre
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return EstadoPermiso.Concedido
            }
        }

        val manifestPerm = permisoAManifest(permiso) ?: return EstadoPermiso.Concedido
        val concedido = ContextCompat.checkSelfPermission(context, manifestPerm) == PackageManager.PERMISSION_GRANTED
        if (concedido) {
            return EstadoPermiso.Concedido
        }

        val yaSolicitado = prefs.getBoolean("solicitado_${permiso.name}", false)
        if (!yaSolicitado) {
            return EstadoPermiso.Denegado
        }

        val activity = currentActivityRef?.get()
        return if (activity != null) {
            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPerm)
            if (showRationale) {
                EstadoPermiso.Denegado
            } else {
                EstadoPermiso.DenegadoParaSiempre
            }
        } else {
            EstadoPermiso.Denegado
        }
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        val estadoActual = estado(permiso)
        if (estadoActual is EstadoPermiso.Concedido) {
            return EstadoPermiso.Concedido
        }

        val launcher = launcherCallback ?: return estado(permiso)

        return suspendCancellableCoroutine { continuation ->
            prefs.edit().putBoolean("solicitado_${permiso.name}", true).apply()
            launcher(permiso) { resultadoConcedido ->
                if (continuation.isActive) {
                    if (resultadoConcedido) {
                        continuation.resume(EstadoPermiso.Concedido)
                    } else {
                        val activity = currentActivityRef?.get()
                        val manifestPerm = permisoAManifest(permiso)
                        val nuevoEstado = if (activity != null && manifestPerm != null) {
                            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPerm)
                            if (showRationale) EstadoPermiso.Denegado else EstadoPermiso.DenegadoParaSiempre
                        } else {
                            EstadoPermiso.Denegado
                        }
                        continuation.resume(nuevoEstado)
                    }
                }
            }
        }
    }

    override fun abrirAjustes() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

