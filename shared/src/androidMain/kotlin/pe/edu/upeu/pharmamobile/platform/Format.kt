package pe.edu.upeu.pharmamobile.platform

import java.text.NumberFormat
import java.util.Locale

actual fun formatearSoles(valor: Double): String {
    val locale = Locale("es", "PE")
    val format = NumberFormat.getCurrencyInstance(locale)
    return format.format(valor)
}
