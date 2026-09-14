package pe.edu.upeu.pharmamobile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        title = "App Móvil",
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
