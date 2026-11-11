package pe.edu.upeu.pharmamobile.domain.telemetria

interface Telemetria {
    /**
     * Registra un evento de analítica saneado (sin JWT, contraseñas ni datos sensibles).
     */
    fun registrarEvento(nombre: String, datos: Map<String, Any> = emptyMap())

    /**
     * Registra un error o excepción relevante con contexto contextual saneado.
     */
    fun registrarError(error: Throwable, contexto: Map<String, String> = emptyMap())
}

