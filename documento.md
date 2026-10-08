# Actividad Autónoma 09: Desarrollo de Aplicaciones Móviles
**Alumno:** Acosta
**Curso:** Desarrollo de Aplicaciones Móviles
**Docente:** (Completar nombre del docente)
**Sesión:** 09
**Fecha:** 12 de Octubre de 2026
**Enlace a la rama:** [Completar con el enlace a GitHub de la rama feature/s09-autonoma-acosta]

---

## Producto 1: Inventario de capacidades nativas
A continuación, se listan todas las declaraciones expect encontradas en el proyecto y sus correspondientes implementaciones actual o inyección por plataforma.

### 1. Formato monetario
- **Capacidad:** formatearSoles
- **Firma exacta:** `expect fun formatearSoles(valor: Double): String`
- **Declaración común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`
- **Android:** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt` (Utiliza `java.text.NumberFormat`)
- **iOS:** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt` (Utiliza `NSNumberFormatter`)
- **Desktop:** `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`

### 2. Compartir (Interfaz inyectada)
*(Nota: Compartidor es una interfaz inyectada, no una declaración expect)*
- **Capacidad:** Compartidor
- **Firma exacta:** `interface Compartidor { fun compartir(texto: String) }`
- **Declaración común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/domain/platform/Compartidor.kt`
- **Android:** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/platform/CompartidorAndroid.kt` (Utiliza `Intent(Intent.ACTION_SEND)`)
- **iOS:** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/platform/CompartidorIos.kt` (Utiliza `UIActivityViewController`)
- **Desktop:** `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/platform/CompartidorDesktop.kt`

### 3. Información del Dispositivo (Tercera capacidad)
- **Capacidad:** InfoDispositivo
- **Firma exacta:** `expect class InfoDispositivo() { val sistema: String; val version: String }`
- **Declaración común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/InfoDispositivo.kt`
- **Android:** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/platform/InfoDispositivo.kt` (Utiliza `android.os.Build.VERSION.RELEASE`)
- **iOS:** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/platform/InfoDispositivo.kt` (Utiliza `platform.UIKit.UIDevice`)
- **Desktop:** `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/platform/InfoDispositivo.kt`

### 4. Plataforma
- **Capacidad:** getPlatform
- **Firma exacta:** `expect fun getPlatform(): Platform`
- **Declaración común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/Platform.kt`
- **Android:** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/Platform.kt`
- **iOS:** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/Platform.kt`
- **Desktop:** `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/Platform.kt`

### 5. Cliente HTTP
- **Capacidad:** httpClient
- **Firma exacta:** `expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient`
- **Declaración común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/data/network/KtorClient.kt`
- **Android:** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/data/network/KtorClient.kt`
- **iOS:** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/data/network/KtorClient.kt`
- **Desktop:** `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/data/network/KtorClient.kt`

### 6. Módulo de Plataforma
- **Capacidad:** platformModule
- **Firma exacta:** `expect val platformModule: Module`
- **Declaración común:** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt`
- **Android:** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt`
- **iOS:** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt`
- **Desktop:** `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt`

---

## Producto 2: Informe comparativo

### 1. ¿Por qué las APIs de formato monetario difieren aunque utilicen la configuración regional de Perú?
Las APIs de formato monetario difieren porque cada plataforma implementa sus propias bibliotecas estándar para internacionalización, las cuales han evolucionado independientemente y tienen reglas ligeramente distintas para presentar monedas. En Android, que corre bajo la máquina virtual de Java (JVM), se emplea la clase `java.text.NumberFormat`. Al usar `NumberFormat.getCurrencyInstance(Locale("es", "PE"))` (en el archivo `Format.kt` de `androidMain`), la JVM devuelve típicamente un formato como "S/ 12.34". En cambio, en iOS se interactúa directamente con el framework nativo `Foundation` mediante `NSNumberFormatter`. Al configurar `NSLocale("es_PE")` (en el archivo `Format.kt` de `iosMain`), el comportamiento depende enteramente del sistema operativo de Apple, que puede devolver valores como "S/ 12.34" o "S/. 12.34" dependiendo de la versión exacta de iOS.

### 2. ¿Por qué CompartidorAndroid necesita Context y CompartidorIos no lo recibe de la misma manera?
La diferencia radica en cómo cada sistema operativo gestiona el ciclo de vida de la interfaz de usuario y los permisos. En Android, `CompartidorAndroid` necesita un `Context` (inyectado a través de Koin con `androidContext()`) porque para lanzar un `Intent` (en este caso, `Intent.ACTION_SEND` para compartir) o iniciar cualquier actividad del sistema, el framework de Android requiere estrictamente un contexto de origen que autorice y maneje la solicitud. Por su parte, `CompartidorIos` no necesita este contexto porque iOS utiliza un enfoque basado en controladores de vista de la capa UIKit. En la implementación de `CompartidorIos`, se crea un `UIActivityViewController` y se solicita al sistema que lo presente directamente utilizando el `window.rootViewController` actual, una operación global en la jerarquía de vistas de Apple que no requiere inyectar un objeto de contexto específico como en Android.

### 3. ¿Qué habría cambiado al usar una interfaz con inyección en lugar de expect/actual?
El uso de una interfaz con inyección de dependencias (como se hizo con `Compartidor`) cambia el momento de resolución de las dependencias de tiempo de compilación a tiempo de ejecución. Con `expect/actual` (como en `formatearSoles` y `InfoDispositivo`), el compilador de Kotlin exige que exista una implementación `actual` exacta para cada objetivo soportado durante la fase de compilación. Si falta alguna, el proyecto no compila. Por otro lado, utilizar una interfaz permite un menor acoplamiento, ya que la presentación solo depende de la abstracción (`Compartidor`) y el módulo de inyección (`PlatformModule`) decide en tiempo de ejecución qué implementación entregar. Esto mejora la capacidad de realizar pruebas, ya que es más fácil crear dobles de prueba (mocks) para las interfaces que para funciones `expect` estáticas globales. Se justificó usar la interfaz para el `Compartidor` porque involucra interacción con el sistema operativo que requiere dependencias complejas (como el `Context`), mientras que funciones puras como formatear un número o leer una propiedad simple del dispositivo (`InfoDispositivo`) se benefician de la resolución estática directa de `expect/actual`.

### 4. ¿Qué ocurre si una implementación actual no existe?
Si una declaración `expect` carece de su respectiva implementación `actual` en uno de los módulos nativos, el compilador de Kotlin Multiplatform detectará la inconsistencia y detendrá el proceso de compilación, lanzando un error específico. Durante la prueba deliberada, al comentar la función `formatearSoles` en `androidMain` y ejecutar el comando `.\gradlew :androidApp:assembleDebug --no-daemon`, el target afectado (`Android`) provocó el siguiente error literal:
`e: file:///.../shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt:3:1 Expected formatearSoles has no actual declaration in module <commonMain> for JVM`
Este error confirma que Kotlin garantiza la seguridad estricta al requerir que todo lo prometido en el código común (el `expect`) esté cubierto en todas las plataformas soportadas antes de generar el binario. Al restaurar la función en `Format.kt`, el código volvió a compilar exitosamente.

### 5. ¿Qué capacidad del proyecto debe permanecer en código común y por qué?
La lógica de validación de negocio y el control de estado (como los ViewModels) deben permanecer siempre en el código común `commonMain`. Por ejemplo, la regla que determina cómo un `ProductoViewModel` carga la lista de productos interactuando con `ObtenerProductoUseCase` debe ser idéntica para Android, iOS y Desktop. Mantener este código independiente del sistema operativo garantiza que la aplicación se comporte exactamente de la misma manera en todas las plataformas, reduciendo la duplicación de código, minimizando los errores (bugs) específicos por plataforma y asegurando que las reglas de negocio sean centralizadas y fácilmente comprobables (testeables). Delegar únicamente capacidades estrictamente nativas (como la API de interfaz de usuario, sensores o formatos del sistema) es el núcleo del patrón Clean Architecture aplicado en este proyecto.

---

## Producto 3: Tercera capacidad nativa (InfoDispositivo)
Se implementó la capacidad "Información del Dispositivo" usando el patrón `expect/actual`.
- **Archivos:** Se creó la clase `InfoDispositivo` en `shared/src/commonMain/.../platform/`, `androidMain`, `iosMain` y `desktopMain`.
- **Android:** Utiliza `android.os.Build.VERSION.RELEASE`
- **iOS:** Utiliza `platform.UIKit.UIDevice.currentDevice.systemName` y `systemVersion`
- **Integración:** Se conectó a la interfaz mediante una nueva pantalla común `AcercaDeScreen.kt` y se agregó una nueva entrada al menú principal en `App.kt` ("Acerca de") para acceder a ella, conservando la arquitectura limpia del proyecto y mostrando los datos extraídos genuinamente del dispositivo.

---

## Producto 4: Capturas reales

> **[Nota para la entrega final: Reemplazar los marcadores E1-E7 con las imágenes reales]**

**E1. Precio formateado en Android**
[Insertar captura aquí]
*(Mismo producto y precio en Android)*

**E2. Precio formateado en iOS**
[Insertar captura aquí]
*(Mismo producto y precio en iOS - Requiere ser ejecutado por un miembro con Mac)*

**E3. Selector nativo de compartir en Android**
[Insertar captura aquí]
*(Mostrando el texto del producto con nombre, precio y stock)*

**E4. Hoja nativa de compartir en iOS**
[Insertar captura aquí]
*(Mostrando el texto del producto - Requiere ser ejecutado por un miembro con Mac)*

**E5. Error real del compilador por falta de actual**
[Insertar captura aquí]
*(Captura del error de compilación documentado generado al comentar formatearSoles)*

**E6. Pantalla Acerca de mostrando sistema y versión en Android**
[Insertar captura aquí]
*(Mostrando "Android" y la versión del sistema)*

**E7. Pantalla Acerca de mostrando sistema y versión en iOS**
[Insertar captura aquí]
*(Mostrando el sistema y versión en iOS - Requiere ser ejecutado por un miembro con Mac)*

---
## Validaciones Pendientes y Ejecución iOS
Como no se dispuso de una Mac para esta ejecución, las evidencias E2, E4 y E7 para iOS quedan como **pendientes**.
**Instrucciones para ejecución en Mac:**
1. Clona el repositorio y cambia a la rama `feature/s09-autonoma-acosta`.
2. Inicia el backend `PharmaSoft` y ajusta las URLs de base en `KtorClient` (si es necesario).
3. Abre Xcode o usa Fleet/Android Studio en macOS y compila el target `iosApp`.
4. Ingresa a la app. Toma la captura **E2** de un producto en la lista.
5. Abre el detalle de un producto y presiona compartir. Toma la captura **E4**.
6. Abre el menú de navegación, ve a "Acerca de" y toma la captura **E7**.
7. **Responsable de la ejecución iOS:** (Completar nombre, fecha y commit)
---
## Enlaces Remotos
- **Rama:** [Enlace a la rama]
- **Commit 1 (Inventario y Rutas):** [Enlace al commit]
- **Commit 2 (Análisis y Resultados):** [Enlace al commit]
- **Commit 3 (Tercera capacidad):** [Enlace al commit]
- **Commit 4 (Evidencias y README):** [Enlace al commit]
- **README actualizado:** [Enlace al README]

Estado de las validaciones: *Provisional, a la espera de capturas iOS y enlaces finales de GitHub.*
