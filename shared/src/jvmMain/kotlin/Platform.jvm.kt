package pe.edu.upeu.pharmamobile

class JVMPlatform : Platform {
    override val name: String = "JVM (Desktop)"
}

actual fun getPlatform(): Platform = JVMPlatform()
