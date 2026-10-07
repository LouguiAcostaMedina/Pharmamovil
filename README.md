This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Conectividad REST

Esta sección detalla la configuración e integración del cliente Ktor con el backend proporcionado.

- **Backend utilizado:** PharmaSoft (Java Spring Boot)
- **URL Base:** `http://10.0.2.2:8080/api/v1/` (para Android) / `http://localhost:8080/api/v1/` (para Desktop/iOS)
- **Puerto:** `8080`
- **Recurso Principal:** `productos`
- **Endpoints documentados:**
  - `GET /productos`
  - `GET /productos/{id}`
  - `POST /productos`
  - `PUT /productos/{id}`
  - `DELETE /productos/{id}`
- **Configuración Ktor:** 
  - `ContentNegotiation` (con `kotlinx.serialization` e `ignoreUnknownKeys = true`).
  - `Logging` (Nivel ALL).
  - `HttpTimeout` configurado para evitar bloqueos por latencia de red.
- **Motores:**
  - Android: `OkHttp` (configurado implícitamente por Ktor o usando CIO/Mock).
  - iOS: `Darwin`.
- **DTO Principales:** `PaginaResponse`, `ProductoDto`.
- **Manejo de Errores:** Se capturan excepciones de red (`ConnectException`, `HttpRequestTimeoutException`) devolviendo estados controlados en el repositorio en lugar de detener la aplicación.

### Instrucciones para ejecutar
- **Backend:** En la carpeta `pharmaSoft`, ejecuta `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2`.
- **Android:** En la carpeta `PharmaMobile`, ejecuta `.\gradlew.bat :androidApp:assembleDebug` o lanza el proyecto desde Android Studio en un emulador.
- **Pruebas:** Ejecuta `.\gradlew.bat :shared:check` para correr los test unitarios de conexión mockeada.

## Manejo de errores
- Implementación de `ErrorApi` mediante sealed class para errores tipificados.
- Intercepción de excepciones HTTP (400, 404, 409) con logs y parseo de `ErrorResponseDto` a `ErrorApi.Validacion`.
- Manejo seguro de timeout, desconexión (`IOException`) y relanzamiento de `CancellationException`.

## Diseño y experiencia de usuario
- **Material 3:** Rediseño completo del sistema de interfaz basándose en los lineamientos Material Design 3.
- **Temas claro/oscuro:** Esquemas de color personalizados (Pharma Teal) con adaptación cuidada para modo oscuro mediante selector de íconos interactivo.
- **Componentes reutilizables:** Creación y centralización de tipografía estandarizada y cards estructurados (ej. TarjetaProducto).
- **Navegación:** Layout responsivo, utilizando NavigationRail en Desktop y Drawer en móviles.
- **Formularios disponibles:**
  - **Productos:** CRUD Completo implementado.
  - **Clientes:** UI solamente. Se han maquetado los campos visualmente sin conexión a lógica.
  - **Pedidos:** UI solamente. Interfaz creada para gestionar las ventas sin integración a backend.

## Capacidades nativas
Esta sección documenta la integración de funcionalidades específicas de plataforma mediante el patrón `expect/actual` y la inyección de dependencias multiplataforma.

- **Formato de precios en soles:** Implementado mediante el patrón `expect/actual` para proveer el formato regional correcto nativo (p. ej. `NumberFormat.getCurrencyInstance` en Android).
- **Compartir productos:** Se abstrajo mediante la interfaz `Compartidor` (`expect interface Compartidor`) y se definieron sus implementaciones nativas correspondientes para invocar los intents de compartir del sistema operativo.
- **Rutas de Formato (expect/actual):**
  - Común: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`
  - Android: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`
  - iOS: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`
  - Desktop: `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/platform/Format.kt`
- **Rutas de Compartir (Compartidor):**
  - Interfaz: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile/platform/Compartidor.kt`
  - Android: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/platform/CompartidorAndroid.kt`
  - iOS: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/platform/CompartidorIos.kt`
  - Desktop: `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/platform/CompartidorDesktop.kt`
- **Inyección y Koin (`platformModule`):**
  - Android: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt` (Utiliza `androidContext()` que viene desde `PharmaApplication`).
  - iOS: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt`
  - Desktop: `shared/src/desktopMain/kotlin/pe/edu/upeu/pharmamobile/di/PlatformModule.kt`
- **Uso en Presentación:**
  - `ProductoViewModel.kt`: Recibe `Compartidor` por inyección y expone el método `compartirProducto` orquestando la construcción del texto compartido.
  - `ProductoScreen.kt`: Utiliza `formatearSoles()` en la UI y conecta el evento `onClick` del botón Compartir hacia el ViewModel.
- **Estado de Validación por Plataforma:**
  - **Android:** ✅ Verificado (Compilado y funcional en entorno Windows/Emulador).
  - **Desktop (JVM):** ✅ Verificado (Compilado exitosamente).
  - **iOS:** ⏳ Pendiente (Desarrollado en código, pendiente compilación o ejecución real en entorno macOS).