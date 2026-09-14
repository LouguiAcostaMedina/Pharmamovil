package pe.edu.upeu.pharmamobile

class DesktopPlatform : Platform {
    override val name: String = "Desktop ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = DesktopPlatform()
