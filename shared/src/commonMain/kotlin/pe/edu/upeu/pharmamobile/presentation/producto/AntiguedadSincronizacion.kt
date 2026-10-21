package pe.edu.upeu.pharmamobile.presentation.producto

import pe.edu.upeu.pharmamobile.tiempoActualEpochSegundos

object AntiguedadSincronizacion {

    fun formatear(
        timestampEpochSegundos: Long?,
        ahoraEpochSegundos: Long = tiempoActualEpochSegundos()
    ): String {
        if (timestampEpochSegundos == null || timestampEpochSegundos <= 0L) {
            return "Nunca sincronizado"
        }
        val diffSegundos = (ahoraEpochSegundos - timestampEpochSegundos).coerceAtLeast(0L)
        return when {
            diffSegundos < 60L -> "Actualizado hace unos instantes"
            diffSegundos < 3600L -> {
                val minutos = diffSegundos / 60L
                if (minutos == 1L) "Actualizado hace 1 minuto" else "Actualizado hace $minutos minutos"
            }
            diffSegundos < 86400L -> {
                val horas = diffSegundos / 3600L
                if (horas == 1L) "Actualizado hace 1 hora" else "Actualizado hace $horas horas"
            }
            else -> {
                val dias = diffSegundos / 86400L
                if (dias == 1L) "Actualizado hace 1 día" else "Actualizado hace $dias días"
            }
        }
    }
}
