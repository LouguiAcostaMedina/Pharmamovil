package pe.edu.upeu.pharmamobile.platform

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSLocale

actual fun formatearSoles(valor: Double): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        locale = NSLocale("es_PE")
    }
    return formatter.stringFromNumber(NSNumber(valor)) ?: "S/ 0.00"
}
