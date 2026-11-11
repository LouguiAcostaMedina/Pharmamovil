package pe.edu.upeu.pharmamobile.data.telemetria

import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria

class TelemetriaIos : Telemetria {
    override fun registrarEvento(nombre: String, datos: Map<String, Any>) {}
    override fun registrarError(error: Throwable, contexto: Map<String, String>) {}
}

