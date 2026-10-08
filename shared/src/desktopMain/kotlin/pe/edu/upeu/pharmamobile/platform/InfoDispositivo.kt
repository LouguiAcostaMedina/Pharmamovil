package pe.edu.upeu.pharmamobile.platform

actual class InfoDispositivo actual constructor() {
    actual val sistema: String = System.getProperty("os.name") ?: "Desktop"
    actual val version: String = System.getProperty("os.version") ?: "Unknown"
}
