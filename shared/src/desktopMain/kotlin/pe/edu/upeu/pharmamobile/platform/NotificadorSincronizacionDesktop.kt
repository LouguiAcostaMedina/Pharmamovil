package pe.edu.upeu.pharmamobile.platform

import pe.edu.upeu.pharmamobile.domain.platform.NotificadorSincronizacion

class NotificadorSincronizacionDesktop : NotificadorSincronizacion {
    override fun notificarResultado(titulo: String, mensaje: String) {
        println("[NOTIFICACIÓN DESKTOP] $titulo — $mensaje")
    }
}

