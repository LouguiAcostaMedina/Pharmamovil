# PharmaMobile - Aplicación Móvil de Farmacia

Este es un proyecto Kotlin Multiplatform (KMP) con Compose Multiplatform enfocado en la gestión farmacéutica, soportando Android, Desktop (JVM) e iOS.

## Estructura del Proyecto

* **`/androidApp`**: Aplicación nativa de Android que aloja el composable principal de la UI compartida.
* **`/iosApp`**: Aplicación iOS nativa en SwiftUI que sirve de punto de entrada al framework KMP compartido.
* **`/shared`**: Módulo compartido que contiene toda la lógica de dominio, datos y presentación:
  - `commonMain`: Código común multiplataforma (Clean Architecture, MVVM, Ktor, Compose Multiplatform).
  - `androidMain`: Implementaciones nativas para Android (`EncryptedSharedPreferences`, `Format`, `Compartidor`, `InfoDispositivo`).
  - `desktopMain`: Implementaciones nativas para Desktop JVM (`AlmacenSeguroDesktop`, `Format`, `CompartidorDesktop`).
  - `iosMain`: Implementaciones nativas para iOS (`AlmacenSeguroIos` con Keychain, `Format`, `CompartidorIos`).

---

## Autenticación JWT y Control de Sesión (Sesión 10)

En la Semana 10 se integró el sistema completo de autenticación y autorización mediante tokens JWT contra el backend **PharmaSoft** (Spring Boot 3.3.x + Spring Security + JJWT 0.12.6).

### 1. Usuarios de Laboratorio
El backend siembra automáticamente los siguientes usuarios de prueba con contraseñas encriptadas mediante **BCrypt**:

| Usuario | Contraseña | Rol | Permisos Otorgados |
| :--- | :--- | :--- | :--- |
| `admin` | `Admin123` | `ADMIN` | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `VENTAS_LEER`, `VENTAS_REGISTRAR`, `REPORTES_VER`, `USUARIOS_ADMINISTRAR` |
| `vendedor` | `Vendedor123` | `VENDEDOR` | `CATALOGO_LEER` (Solo lectura de catálogo), `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `VENTAS_LEER`, `VENTAS_REGISTRAR` |
| `almacen` | `Almacen123` | `ALMACENERO` | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR` (Escritura en catálogo; **403 Forbidden** en clientes o ventas) |

### 2. Contratos REST de Seguridad
- **`POST /api/v1/auth/login`**:
  - *Request Body:* `{"username": "admin", "password": "Admin123"}`
  - *Response Body:* `{"token": "ey...", "tipo": "Bearer", "expiraEnSegundos": 3600, "username": "admin", "nombreCompleto": "Carlos Ramirez Torres", "roles": ["ADMIN"], "permisos": [...]}`
- **`GET /api/v1/auth/me`**:
  - Exige cabecera `Authorization: Bearer <token>`.
  - Retorna `UsuarioResponseDTO` (`id`, `username`, `empleadoNombre`, `cargo`, `roles`, `permisos`).
- **Expiración:** 3600 segundos (60 minutos). No existe endpoint de refresh en el servidor; al expirar se recibe HTTP 401 y se fuerza nuevo login.

### 3. URL Base por Plataforma (`PlatformUrl`)
Centralizada mediante `DefaultRequest` en Ktor:
- **Android Emulador:** `http://10.0.2.2:8080/api/v1/`
- **Desktop (JVM):** `http://localhost:8080/api/v1/`
- **iOS Simulador:** `http://localhost:8080/api/v1/`

### 4. Almacén Seguro Multiplataforma (`AlmacenSeguro`)
- **Android (`AlmacenSeguroAndroid`):** Utiliza `EncryptedSharedPreferences` respaldado por `MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()`. El token jamás se almacena en texto plano.
- **iOS (`AlmacenSeguroIos`):** Utiliza la API nativa de Apple `Security.framework` (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`) almacenando el token en el Keychain seguro del sistema operativo.
- **Desktop (`AlmacenSeguroDesktop`):** Almacenamiento seguro en memoria para sesiones de depuración local.

### 5. Configuración del Cliente HTTP Ktor (`KtorClient`)
- Un único `HttpClient` adaptado para toda la aplicación (CRUD de productos y autenticación).
- **`ktor-client-auth`:** Plugin `Auth` con proveedor `bearer`:
  - `loadTokens`: Lee el token del `AlmacenSeguro`.
  - `sendWithoutRequest`: Retorna `true` para todas las peticiones excepto las que terminen en `/auth/login`.
- **Invalidación de caché:** Invocación de `cliente.authProvider<BearerAuthProvider>()?.clearToken()` tras guardar o borrar el token para forzar la recarga desde el almacén.
- **Sanitización de Logs:** `sanitizeHeader { it == HttpHeaders.Authorization }` para que el token JWT figure como `Bearer ***` en Logcat y no se filtre información confidencial.

### 6. Control de Respuestas HTTP 401 y 403
- **HTTP 401 (Unauthorized):**
  - En login: Credenciales incorrectas; muestra mensaje de error en pantalla.
  - En peticiones protegidas: Token ausente o expirado; la aplicación borra el token de `AlmacenSeguro`, limpia la caché de Ktor, restablece el estado de sesión y redirige inmediatamente al usuario a la pantalla de Login con un aviso explicativo.
- **HTTP 403 (Forbidden):**
  - Ocurre cuando el rol no cuenta con el permiso para la operación solicitada (ej. `almacen` accediendo a clientes).
  - La sesión del usuario **SE CONSERVA** intacta y se presenta un mensaje no intrusivo (Snackbar/Banner) informando la falta de privilegios.
- **Demostración Controlada de 403:** Se incluye un botón de prueba en la UI de depuración (`Probar 403`) que efectúa una solicitud a `/api/v1/clientes` para verificar en vivo el rechazo del servidor.

### 7. Cierre de Sesión Seguro
- La acción de Logout borra el token del almacenamiento seguro, limpia la caché del proveedor bearer de Ktor, invalida los datos reactivos del ViewModel y resetea la navegación impidiendo regresar mediante el botón Atrás.

---

## Persistencia Offline-First con SQLDelight (Sesión 11)

En la Semana 11 se implementó la arquitectura de persistencia local y sincronización reactiva bidireccional bajo el patrón **Offline-First**, permitiendo que la aplicación continúe operando plenamente ante la ausencia de conectividad o con el backend apagado.

### 1. Diferenciación Arquitectónica: Backend H2 vs SQLite Local
* **Backend PharmaSoft (Servidor Spring Boot 3.3.x):**
  - Utiliza el perfil persistente `h2` sobre el archivo de base de datos relacional `jdbc:h2:file:./data/pharmadb`.
  - Constituye la fuente canónica de datos y emite identificadores positivos autoincrementales (`id > 0`).
  - No debe ser reemplazado por bases móviles ni alterado con scripts destructivos.
* **Cliente PharmaMobile (Kotlin Multiplatform):**
  - Utiliza **SQLDelight 2.1.0** para la generación tipada de código SQLite local en cada plataforma (`pharma.db`).
  - La base de datos local SQLite es la única fuente de verdad directa para la interfaz de usuario.

### 2. Arquitectura de Archivos y Componentes
* **Esquema SQL (`shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobile/db/Producto.sq`):**
  - Define la tabla `Producto` con las columnas requeridas por el modelo completo del dominio: `id`, `nombre`, `precio`, `stock`, `activo`, `descripcion`, `imagen`, `categoria`, `categoriaId` y la bandera `pendiente` (0 = sincronizado, 1 = pendiente de subida).
  - Consultas SQL tipadas generadas en `PharmaDb`: `listarTodos`, `listarPendientes`, `obtenerPorId`, `insertar`, `actualizar`, `borrarPorId`, `borrarSincronizados`, `siguienteIdLocal`, `confirmarSubida`, `contarPendientes`.
* **Drivers de Conexión (`CreadorDeDriver`):**
  - **Android (`CreadorDeDriverAndroid`):** Instancia `AndroidSqliteDriver(PharmaDb.Schema, context, "pharma.db")`.
  - **iOS (`CreadorDeDriverIos`):** Instancia `NativeSqliteDriver(PharmaDb.Schema, "pharma.db")`.
  - **Desktop JVM (`CreadorDeDriverDesktop`):** Instancia `JdbcSqliteDriver` persistente sobre un archivo en el directorio de usuario (`~/.pharmamobile/pharma.db`), ejecutando `PharmaDb.Schema.create(driver)` si el archivo no existe.
  - **Pruebas (`DriverPruebasFactory`):** Fábrica multiplataforma (`expect/actual`) que suministra `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` en Desktop JVM evitando acoplar drivers JDBC dentro del código común compartido con iOS.
* **Capa Local (`ProductoLocal`):**
  - Encapsula todas las operaciones sobre `PharmaDb`.
  - No importa clases de Ktor ni DTOs del backend, preservando la separación estricta de capas.
  - Emite datos reactivos mediante `consultas.listarTodos().asFlow().mapToList(dispatcher).map { ... }`.
  - Proporciona transacciones seguras: `guardarPendiente`, `reemplazarSincronizados` (borra únicamente sincronizados y preserva pendientes) y `confirmarSubida`.
* **Repositorio Offline-First (`ProductoRepositorioOffline`):**
  - Implementa el contrato `ProductoRepository` exponiendo `observar(): Flow<List<Producto>>` y `suspend fun sincronizar()`.
  - La UI consume exclusivamente el `Flow` de la base local; nunca se alimenta directamente de respuestas HTTP.
  - Sincronización protegida por `Mutex` para prevenir ejecuciones concurrentes.

### 3. Protocolo de Sincronización: Subida, Confirmación Atómica y Descarga
La sincronización manual o automática sigue estrictamente el siguiente orden determinista:
1. **Paso A (Subir Pendientes):** Consulta `local.pendientes()` y envía secuencialmente cada creación pendiente al servidor vía `POST /api/v1/productos`.
2. **Paso B (Confirmación Inmediata):** Al recibir la respuesta exitosa del servidor con el ID positivo y los datos canónicos, se ejecuta una transacción atómica local que elimina el registro temporal negativo e inserta el producto confirmado (`pendiente = 0`). Esto evita reenvíos duplicados si una descarga posterior falla.
3. **Paso C (Descarga Paginada):** Recorre todas las páginas del catálogo remoto (`GET /api/v1/productos?pagina=N&tamanio=50`) asegurando que no se pierdan páginas intermedias ni posteriores.
4. **Paso D (Reemplazo Transaccional):** En una sola transacción, borra únicamente los productos sincronizados (`borrarSincronizados()`) e inserta la lista remota completa, manteniendo inalterados los registros locales que aún estén marcados como pendientes.
5. **Paso E (Notificación Reactiva):** El Flow de SQLDelight emite automáticamente la nueva lista de productos actualizando el estado de la pantalla sin requerir recargas manuales imperativas.

### 4. Manejo de IDs Temporales Negativos
* Cuando se registra un producto sin conexión (o antes de sincronizar), `ProductoLocal.guardarPendiente()` calcula atómicamente el identificador temporal:
  `val nuevoId = minOf(menorIdLocal, 0L) - 1L`
* Esto produce identificadores negativos (-1, -2, -3...) garantizando unicidad local y descartando colisiones con los identificadores positivos autoincrementales de H2 generados por el servidor.
* La interfaz de usuario distingue estos elementos mostrando una insignia distintiva ("Pendiente de sincronizar") y su ID temporal.

### 5. Apertura Offline y Sesión JWT
* **Problema Resuelto:** En S10, la validación de sesión ejecutaba `GET /auth/me` de forma obligatoria; si el servidor estaba apagado, la llamada fallaba y forzaba el cierre de sesión, impidiendo ingresar al catálogo offline.
* **Estrategia S11:** `AuthRepositorioRest.sesionGuardada()` implementa apertura offline resiliente:
  - Recupera los datos de identidad y permisos previamente cacheados en `AlmacenSeguro`.
  - Decodifica las reclamaciones del token JWT (`exp`) y contrasta la expiración contra la hora local del dispositivo (`tiempoActualEpochSegundos()`).
  - Si el token está vigente pero el backend no responde (`SinConexion` / `TiempoAgotado`), restaura la sesión de solo lectura para permitir consultar el catálogo SQLite guardado.
  - Al recuperar conexión, la sesión se revalida contra el backend; y un HTTP 401 real continúa invalidando inmediatamente la sesión por motivos de seguridad.

### 6. ViewModel y Comportamiento Reactivo de la UI
* `ProductoViewModel` se suscribe una única vez a `repositorio.observar()` en su bloque `init`.
* Se eliminaron las recargas manuales (`cargarProductos()`) tras `registrar()`, `actualizar()` o `eliminar()`. La base de datos es el único detonador de actualizaciones visuales.
* El estado de la UI (`ProductoUiState`) desacopla limpiamente:
  - `productos`: Lista obtenida exclusivamente de SQLite.
  - `sincronizando`: Indicador de progreso durante el ciclo de sincronización en segundo plano.
  - `avisoRed`: Banner contextual de aviso cuando no hay conexión, sin vaciar el catálogo previo.

### 7. Ciclo de Verificación Offline
1. **Sesión Online:** Iniciar sesión con backend activo (`admin` / `Admin123`) y sincronizar para poblar la base local.
2. **Simulación Offline:** Detener el backend o deshabilitar conectividad de red.
3. **Persistencia Local:** Reabrir la aplicación; el catálogo previo se muestra inmediatamente desde SQLite con banner informativo de modo offline.
4. **Creación Offline:** Crear un nuevo producto; se inserta al instante en SQLite con ID negativo (-1) y marca visual "Pendiente de sincronizar".
5. **Reinicio sin Red:** Cerrar y volver a abrir la aplicación con el backend apagado; el producto pendiente persiste en SQLite.
6. **Reconexión y Sincronización:** Iniciar el backend y pulsar "Sincronizar"; el producto se sube al servidor, se le asigna su ID positivo oficial de PharmaSoft y desaparece la marca de pendiente.
7. **Verificación de No Duplicación:** Una segunda sincronización no duplica el producto en el servidor ni en la base local.

### 8. Estado por Plataformas y Limitaciones Técnicas
* **Desktop (JVM):** Plataforma principal de desarrollo y pruebas automatizadas en este entorno. Soporta base de datos persistente en archivo y suite de pruebas unitarias en memoria.
* **Android:** Implementado con `AndroidSqliteDriver`, verificado a nivel de arquitectura y dependencias de Gradle.
* **iOS:** Implementado con `NativeSqliteDriver` y Keychain en `iosMain`. Debido a que el host actual es Windows 11 x64 y no dispone de macOS/Xcode, la compilación y ejecución de iOS queda formalmente declarada como **PENDIENTE DE ENTORNO**.

---

## Ejecución del Proyecto

### Backend (Spring Boot con H2)
```powershell
cd c:\Users\XD\Documents\PharmaMobile\pharmaSoft
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

### Pruebas Unitarias Multiplataforma
```powershell
cd c:\Users\XD\Documents\PharmaMobile\PharmaMobile
.\gradlew.bat desktopTest --info
```

### Compilar Aplicación Android
```powershell
cd c:\Users\XD\Documents\PharmaMobile\PharmaMobile
.\gradlew.bat :androidApp:assembleDebug
```

---

## Capacidades Nativas Previas (S08 - S09)
- **Formato monetario:** `formatearSoles(valor: Double)` implementado con `expect/actual` (`NumberFormat` en Android y JVM, `NSNumberFormatter` en iOS).
- **Compartir productos:** Interfaz `Compartidor` (`Intent.ACTION_SEND` en Android, `UIActivityViewController` en iOS).
- **Información del dispositivo:** `InfoDispositivo` (`Build.VERSION.RELEASE` en Android, `UIDevice.currentDevice` en iOS).