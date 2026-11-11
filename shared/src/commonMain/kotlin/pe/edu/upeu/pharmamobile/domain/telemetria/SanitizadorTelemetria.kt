package pe.edu.upeu.pharmamobile.domain.telemetria

object SanitizadorTelemetria {

    private val CLAVES_SENSIBLES = setOf(
        "token", "jwt", "password", "contrasena", "clave", "authorization", "bearer", "secret", "credencial",
        "email", "correo", "paciente", "cliente", "apikey", "api_key"
    )

    fun sanearParametros(datos: Map<String, Any>): Map<String, Any> {
        return datos.mapValues { (clave, valor) ->
            val claveLower = clave.lowercase()
            if (CLAVES_SENSIBLES.any { claveLower.contains(it) }) {
                "[PROTEGIDO]"
            } else {
                when (valor) {
                    is String -> sanearTexto(valor)
                    else -> valor
                }
            }
        }
    }

    fun sanearContexto(contexto: Map<String, String>): Map<String, String> {
        return contexto.mapValues { (clave, valor) ->
            val claveLower = clave.lowercase()
            if (CLAVES_SENSIBLES.any { claveLower.contains(it) }) {
                "[PROTEGIDO]"
            } else {
                sanearTexto(valor)
            }
        }
    }

    fun sanearTexto(texto: String): String {
        return texto
            .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*"), "Bearer [PROTEGIDO]")
            .replace(Regex("(?i)password=([^&\\s]+)"), "password=[PROTEGIDO]")
            .replace(Regex("(?i)contrasena=([^&\\s]+)"), "contrasena=[PROTEGIDO]")
            .replace(Regex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"), "[EMAIL_PROTEGIDO]")
    }
}
