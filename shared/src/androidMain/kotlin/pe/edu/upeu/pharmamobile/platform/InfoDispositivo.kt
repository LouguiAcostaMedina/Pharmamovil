package pe.edu.upeu.pharmamobile.platform

import android.os.Build

actual class InfoDispositivo actual constructor() {
    actual val sistema: String = "Android"
    actual val version: String = Build.VERSION.RELEASE
}
