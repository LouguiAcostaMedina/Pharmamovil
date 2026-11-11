package pe.edu.upeu.pharmamobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun tiempoActualEpochSegundos(): Long

expect fun tiempoActualEpochMillis(): Long

private const val CARACTERES_HEX = "0123456789abcdef"

fun generarClaveIdempotencia(): String {
    val aleatorio = (1..12).map { CARACTERES_HEX.random() }.joinToString("")
    return "req-${tiempoActualEpochMillis()}-$aleatorio"
}
