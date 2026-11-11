package pe.edu.upeu.pharmamobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.android.ext.android.inject
import pe.edu.upeu.pharmamobile.domain.permission.Permiso
import pe.edu.upeu.pharmamobile.platform.GestorPermisosAndroid

class MainActivity : ComponentActivity() {

    private val gestorPermisos: GestorPermisosAndroid by inject()
    private var callbackPermiso: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        callbackPermiso?.invoke(concedido)
        callbackPermiso = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        gestorPermisos.vincularActividad(this) { permiso, callback ->
            callbackPermiso = callback
            val manifestPerm = when (permiso) {
                Permiso.NOTIFICACIONES -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.POST_NOTIFICATIONS
                    } else null
                }
                Permiso.CAMARA -> Manifest.permission.CAMERA
                Permiso.UBICACION -> Manifest.permission.ACCESS_FINE_LOCATION
            }
            if (manifestPerm != null) {
                requestPermissionLauncher.launch(manifestPerm)
            } else {
                callback(true)
            }
        }

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gestorPermisos.desvincularActividad()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
