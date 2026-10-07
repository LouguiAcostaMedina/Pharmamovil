package pe.edu.upeu.pharmamobile.platform

import pe.edu.upeu.pharmamobile.domain.platform.Compartidor

class CompartidorDesktop : Compartidor {
    override fun compartir(texto: String) {
        println("Compartir en desktop no soportado nativamente. Texto: $texto")
    }
}
