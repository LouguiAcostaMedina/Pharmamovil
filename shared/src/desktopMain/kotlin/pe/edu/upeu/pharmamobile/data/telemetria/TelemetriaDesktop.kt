package pe.edu.upeu.pharmamobile.data.telemetria

import pe.edu.upeu.pharmamobile.domain.telemetria.SanitizadorTelemetria
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria

class TelemetriaDesktop : Telemetria {
    override fun registrarEvento(nombre: String, datos: Map<String, Any>) {
        val saneados = SanitizadorTelemetria.sanearParametros(datos)
        println("[TELEMETRÍA DESKTOP] Evento: $nombre -> $saneados")
    }

    override fun registrarError(error: Throwable, contexto: Map<String, String>) {
        val contextoSaneado = SanitizadorTelemetria.sanearContexto(contexto)
        println("[TELEMETRÍA DESKTOP] Error: ${error.message} -> $contextoSaneado")
    }
}

