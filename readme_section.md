
## Código específico de plataforma

Esta sección documenta las capacidades nativas implementadas usando `expect/actual` y patrones de inyección.

### Capacidades del Inventario
1. **Formato monetario** (`expect fun formatearSoles(valor: Double): String`)
   - **Contrato común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`
   - **Android:** `shared/src/androidMain/.../Format.kt` (API: `java.text.NumberFormat`)
   - **iOS:** `shared/src/iosMain/.../Format.kt` (API: `NSNumberFormatter`)
   - **Funcionalidad:** Muestra el precio formateado en la lista de productos y detalles (`ProductoScreen.kt`).

2. **Compartidor** (`interface Compartidor`)
   - **Contrato común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/domain/platform/Compartidor.kt`
   - **Android:** `CompartidorAndroid.kt` (API: `Intent.ACTION_SEND` con `Context`)
   - **iOS:** `CompartidorIos.kt` (API: `UIActivityViewController`)
   - **Funcionalidad:** Permite compartir los detalles del producto mediante el selector nativo.

3. **Información del Dispositivo** (`expect class InfoDispositivo()`)
   - **Contrato común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/InfoDispositivo.kt`
   - **Android:** `InfoDispositivo.kt` (API: `android.os.Build.VERSION.RELEASE`)
   - **iOS:** `InfoDispositivo.kt` (API: `platform.UIKit.UIDevice`)
   - **Funcionalidad:** Muestra el sistema operativo y su versión en la pantalla `AcercaDeScreen.kt`.

4. **Plataforma Base** (`expect fun getPlatform(): Platform`)
   - **Contrato común:** `Platform.kt`
   - **Funcionalidad:** Utilidad base.

5. **Cliente HTTP Ktor** (`expect fun httpClient()`)
   - **Contrato común:** `KtorClient.kt`
   - **Funcionalidad:** Cliente de red configurado por plataforma.

6. **Módulo de Plataforma Koin** (`expect val platformModule: Module`)
   - **Contrato común:** `PlatformModule.kt`
   - **Funcionalidad:** Inyección de dependencias nativas.

### Requisitos de Ejecución
- **Android:** Android Studio o CLI, emulador o dispositivo físico, Backend PharmaSoft corriendo localmente.
- **iOS:** macOS con Xcode instalado, dispositivo o simulador iOS.

### Estado real de validación
- **Android:** Validado ✅ (Se verificó compilación exitosa, error provocado de compilación documentado, y pantallas operativas de formato de moneda, compartidor e información de sistema).
- **iOS:** Pendiente ⏳ (Requiere ejecución por un integrante del equipo con entorno macOS).
