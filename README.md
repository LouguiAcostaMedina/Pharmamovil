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

---

## Actividad Autónoma S11: Pruebas Unitarias y Evidencia de Escenarios Offline

Esta sección documenta los entregables, diseño técnico y resultados de la **Actividad Autónoma S11** en la rama `feature/s11-autonoma-acosta`.

### 1. Suite de Pruebas Unitarias en `commonTest` (Producto 1 — 7 pts)

Se diseñó e implementó la correspondencia exhaustiva entre los seis requerimientos obligatorios de la guía y los métodos en [ProductoRepositorioOfflineTest.kt](file:///shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobile/data/repository/ProductoRepositorioOfflineTest.kt):

| Requisito del PDF | Método de Prueba en `commonTest` | Comprobaciones Realizadas |
| :--- | :--- | :--- |
| **1. Sincronización con red** | `caso1_sincronizacion_con_red_escribe_en_base_y_flow_emite_lista_completa` | Inicialmente vacío; ejecuta sincronización con `MockEngine`; comprueba que SQLite contiene los 2 productos remotos y el `Flow` de `repo.observar()` emite automáticamente la lista completa ordenada alfabéticamente con marcas `pendiente = false`. |
| **2. Registro sin red** | `caso2_registro_sin_red_conserva_producto_con_id_negativo_y_marca_pendiente` | Falla transporte de red simulando desconexión (`IOException`); el producto sobrevive en SQLite con ID negativo (`id < 0`), bandera `pendiente = true` y el `Flow` lo emite de inmediato. |
| **3. Envío al reconectar** | `caso3_envio_al_reconectar_sube_pendiente_mediante_post_antes_de_descarga_asigna_id_positivo_y_elimina_pendiente` | Prepara producto pendiente local (-100); simula reconexión; ejecuta sincronización verificando orden estricto (primero sube pendientes vía POST, luego GET de catálogo); la respuesta confirma ID positivo oficial (+55), elimina la marca pendiente y deja una única fila en SQLite. |
| **4. La descarga no borra pendientes** | `caso4_descarga_no_borra_pendientes_locales_e_incorpora_productos_del_servidor` | Inserta pendiente local (-1); ejecuta `descargarCatalogo()` que hace un GET real con 2 productos del servidor; verifica que el pendiente local NO es eliminado ni sobrescrito por la descarga y convive intacto junto a los 2 productos descargados (total 3). |
| **5. Identificadores sin colisión** | `caso5_identificadores_sin_colision_entre_productos_locales_negativos_y_remotos_positivos` | Inserta producto offline con ID negativo (-1L) y producto remoto confirmado con ID positivo (+100L); verifica que ambos coexisten en la base sin pisarse, comprobando cantidad (2), IDs respectivos y nombres independientes. |
| **6. Sincronizar dos veces no duplica** | `caso6_sincronizar_dos_veces_no_duplica_registros_ni_repite_post_de_creacion_confirmada` | Ejecuta dos llamadas sucesivas a `repo.sincronizar()`; comprueba que la cantidad de registros en la base no se altera (siguen siendo 2), los IDs permanecen constantes y que el POST de creación ya confirmada no se vuelve a invocar en la segunda sincronización. |

#### Comandos de Ejecución y Apertura del Reporte
Para ejecutar la suite completa de pruebas unitarias multiplataforma:
```powershell
cd c:\Users\XD\Documents\PharmaMobile\PharmaMobile
.\gradlew.bat desktopTest --info
```

Para visualizar el reporte interactivo completo en Chrome o Edge:
```powershell
# Abrir en Microsoft Edge:
Start-Process msedge "file:///c:/Users/XD/Documents/PharmaMobile/PharmaMobile/shared/build/reports/tests/desktopTest/index.html"

# O en Google Chrome:
Start-Process chrome "file:///c:/Users/XD/Documents/PharmaMobile/PharmaMobile/shared/build/reports/tests/desktopTest/index.html"
```
Una vez abierto el reporte en el navegador:
1. Ir a la pestaña **Classes**.
2. Hacer clic en `pe.edu.upeu.pharmamobile.data.repository.ProductoRepositorioOfflineTest`.
3. Se desplegarán los 14 métodos (los 6 obligatorios, 5 de antigüedad y 3 de regresión) con 100% de tasa de éxito.

---

### 2. Antigüedad de los Datos y Marca Temporal (Producto 3 — 3 pts)

* **Tabla de Metadatos y Persistencia:** Se añadió la tabla `metadata_sincronizacion(clave TEXT PRIMARY KEY, valor TEXT)` en `SincronizacionMetadata.sq` y la migración `1.sqm`. Adicionalmente, el driver desktop (`CreadorDeDriverDesktop.kt`) aplica creación idempotente `CREATE TABLE IF NOT EXISTS` garantizando compatibilidad retrospectiva sin necesidad de borrar bases existentes.
* **Regla de Actualización Atómica:** La marca temporal (`ultima_sincronizacion_epoch_seg`) se almacena exclusivamente tras completar con éxito:
  1. La confirmación de todas las subidas de pendientes locales.
  2. La descarga paginada íntegra de los productos remotos.
  3. La transacción local de actualización en SQLite.
  Cualquier fallo de transporte en red o corte intermedio aborta la actualización de la marca, conservando intacto el valor previo.
* **Formato en Lenguaje Natural con Reloj Inyectable:** La clase `AntiguedadSincronizacion` calcula la diferencia temporal frente a un reloj inyectable (`tiempoActualEpoch: () -> Long`), permitiendo pruebas unitarias deterministas sin depender del reloj del sistema:
  - `null` o sin registro previo: `"Nunca sincronizado"`
  - Menor a 60 segundos: `"Actualizado hace unos instantes"`
  - 1 minuto: `"Actualizado hace 1 minuto"`
  - 2 a 59 minutos: `"Actualizado hace X minutos"`
  - 1 hora: `"Actualizado hace 1 hora"`
  - 2 a 23 horas: `"Actualizado hace X horas"`
  - 1 día: `"Actualizado hace 1 día"`
  - 2 o más días: `"Actualizado hace X días"`
* **Visualización Dinámica en UI:** En `ProductoScreen.kt`, la cabecera del catálogo muestra el indicador con icono de actualización, el texto en lenguaje natural y un botón de sincronización manual rápida. Un ticker en segundo plano refresca la etiqueta periódicamente mientras la pantalla permanece abierta.

---

### 3. Bitácora de Escenarios Offline (Producto 2 — 6 pts)

* **B01 — Apertura sin red con datos previos:** Con sesión iniciada y catálogo sincronizado, al cerrar la app y pasar a modo desconectado, SQLite mantiene intactos los registros. La UI muestra el catálogo inmediatamente y presenta el aviso contextual de modo offline.
* **B02 — Registro sin red:** Estando autenticado como `admin` sin conexión al backend, se registra un producto. La aplicación lo inserta en SQLite de inmediato asignándole un ID temporal negativo (`-1`), lo marca con estado visual "Pendiente" y lo lista en el catálogo.
* **B03 — Reconexión y envío:** Al restablecer conexión con PharmaSoft y pulsar "Sincronizar", el repositorio sube el pendiente mediante `POST /api/v1/productos`, recibe el ID positivo oficial asignado por el backend, actualiza la fila local en SQLite y elimina la marca pendiente.
* **B04 — Primera apertura sin red:** En una instalación limpia sin datos previos ni caché local, la aplicación abre en estado informativo: muestra `"Nunca sincronizado"`, estado vacío amigable indicando la necesidad de conectar para sincronizar por primera vez, sin generar excepciones no controladas ni bloqueos de pantalla.
* **B05 — Sesión expirada sin red (Política Implementada):** Si el token JWT ha vencido según la hora local y no hay conexión con el backend:
  1. Se preserva el acceso de solo lectura al catálogo guardado en SQLite para consulta operativa.
  2. Los permisos se restringen a operaciones `_LEER`.
  3. Se muestra un banner amigable advirtiendo que la sesión caducó y requerirá autenticación al reconectar.
  4. NO se eliminan los productos pendientes guardados localmente.
  5. Al recuperar la red, cualquier petición que responda HTTP 401 invalida formalmente la sesión forzando el login.

---

### 4. Resultados Verificados y Limitaciones
* **Suite de Pruebas Desktop / JVM:** 39 pruebas ejecutadas, 39 exitosas (100% de éxito), 0 fallos, 0 omitidas.
* **Backend PharmaSoft:** Verificado en commit compatible `dd72f15` con base de datos H2 persistente (`./data/pharmadb.mv.db`), respetando el esquema de seguridad JWT y catálogo de medicamentos.
* **Declaración de Entorno Móvil / Multiplataforma:** La compilación y arquitectura de Android están completamente integradas con `AndroidSqliteDriver` y componentes Compose. El target iOS permanece como **PENDIENTE DE ENTORNO** al no disponerse de un host macOS/Xcode físico.
---

## Actividad Práctica S13: Proyecto Integrador — Refinamiento Offline-First, Firebase y Permisos Nativos

En la Sesión 13 se culmina el ciclo del proyecto integrador incorporando resiliencia transaccional avanzada, persistencia de intentos y fallos en SQLite, clasificación de errores en la sincronización, observabilidad de estados en la interfaz de usuario, gestión nativa de permisos en Android con tres estados y arquitectura de telemetría desacoplada con adaptadores.

### Paso 1 — Recorrido del Flujo y Tabla de Huecos

Tras auditar exhaustivamente el comportamiento del sistema a través de los ocho momentos críticos del flujo de usuario, se identificaron y categorizaron los siguientes hallazgos:

| ID | Momento | Hallazgo | Gravedad | Decisión | Estado | Evidencia |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **H01** | 1. Primera apertura sin sesión | La aplicación inicia en estado no autenticado y navega al Login. No existe indicador previo de estado de conectividad en el formulario de acceso antes de intentar autenticar. | Baja | Posponer a S14 | Verificado | `LoginScreen` muestra formulario limpio sin advertencias de red |
| **H02** | 2. Login incorrecto y login sin conexión | Con credenciales inválidas el servidor responde 401; sin conexión Ktor captura `IOException` y presenta mensaje de error sin permitir acceso a nuevos usuarios offline (política correcta de seguridad). | Baja | Descartar con justificación | Verificado | `AuthRepositorioRest` captura `ErrorApi.SinConexion` y `ErrorApi.NoAutorizado` |
| **H03** | 3. Listado local vacío | Al iniciar sesión con catálogo vacío o fallo inicial de carga remota, se visualiza estado vacío sin acceso rápido a reintento desde el cuerpo central. | Media | Cerrar en S13 | Resuelto en S13 | `ProductoScreen` provee acción de sincronización manual en cabecera y estado vacío |
| **H04** | 4. Creación en modo avión | Al crear un producto sin red, se guarda con ID negativo (-1) y `pendiente = 1`, pero la tabla no almacena intentos, último error ni fecha de reintento, generando ciclos ciegos de reenvío. | Alta | Cerrar en S13 | Resuelto en S13 | **Prioridad 1:** Esquema ampliado con `intentos`, `ultimoError`, `falloPermanente`, `proximoReintentoEpochMillis` y `claveIdempotencia` |
| **H05** | 5. Reconexión y sincronización | Durante la subida de pendientes, cualquier fallo transitorio (503/timeout) cancela todo el lote sin backoff exponencial ni espera progresiva con tope de 16s. | Alta | Cerrar en S13 | Resuelto en S13 | **Prioridad 2:** Subida resiliente por fila con `ResultadoEnvio.Transitorio`, reintentos limitados (MAX 5) y espera progresiva |
| **H06** | 6. Validación del servidor | Errores 400 (validación) o 409 (conflicto) relanzan excepción deteniendo el lote y dejando la fila para reenvío infinito en lugar de marcar fallo permanente y excluirlo de sync automática. | Alta | Cerrar en S13 | Resuelto en S13 | **Prioridad 3:** Clasificación en `ResultadoEnvio.Permanente`, exclusión de sync automática y acción manual de reintento |
| **H07** | 7. Token vencido durante un formulario | Al vencer el JWT (3600s), si el cliente intenta sincronizar recibe 401. Debe invalidarse la sesión de forma controlada sin perder los registros pendientes locales en SQLite. | Media | Cerrar en S13 | Resuelto en S13 | `ProductoRepositorioOffline` detecta 401, detiene subida y preserva pendientes locales para el reingreso |
| **H08** | 8. Logout y cambio de usuario | El cierre de sesión purga `AlmacenSeguro` y la caché Ktor; los datos SQLite persisten pero la UI debe refrescar de inmediato privilegios acordes al nuevo rol. | Baja | Posponer a S14 | Verificado | `AuthViewModel.cerrarSesion` limpia estado reactivo y almacén |

#### Tres Prioridades Seleccionadas para S13:
1. **Prioridad 1 (H04 - Paso 2):** Persistencia de intentos, último error y estado de fallo permanente en SQLDelight mediante migración `2.sqm`.
2. **Prioridad 2 (H05 - Paso 3):** Clasificación de fallos transitorios vs permanentes y limitación de reintentos por fila con backoff exponencial progresivo (tope de 16s) y protección de sesión.
3. **Prioridad 3 (H06/H04 - Paso 4):** Visibilidad reactiva del estado de sincronización por fila (pendientes vs fallidas), contadores en pantalla y acción de reintento manual autorizado.

---

## Actividad Práctica S14: Mejoras Finales, Firma y Generación de Builds AAB e IPA

En la Sesión 14 se formaliza la salida del proyecto hacia producción: cierre de la ficha de integración S13, congelación definitiva del alcance, configuración de almacén de claves de entrega (keystore) fuera del repositorio, lectura segura de credenciales mediante `keystore.properties`, minificación R8/ProGuard preservando serialización y dependencias Koin, empaquetado de artefactos AAB y APK release, verificación oficial con `apksigner`, documentación de limitaciones de entorno iOS y matriz de ejecución en dispositivos móviles.

### Paso 1 — Cierre de la Ficha de Integración S13 y Congelación del Alcance

Se revisó la tabla de huecos de la Sesión 13, clasificando el estado definitivo de cada momento del recorrido:

| ID | Momento Auditado | Decisión Original | Estado de Cierre S14 | Justificación y Evidencia Observable |
| :--- | :--- | :--- | :--- | :--- |
| **H01** | 1. Primera apertura sin sesión | Posponer a S14 | **Cerrado** | Formulario de Login validado. Si no hay red, Ktor reporta mensaje claro de verificación de conexión sin excepciones no controladas. |
| **H02** | 2. Login incorrecto y sin conexión | Descartar con justificación | **Cerrado** | Se distingue con exactitud: credenciales inválidas (HTTP 401) vs desconexión de red (`ErrorApi.SinConexion`), sin admitir sesiones espurias offline. |
| **H03** | 3. Listado local vacío | Cerrar en S13 | **Cerrado** | Pantalla con estado vacío informativo y botón de sincronización manual operativa; no se bloquea la navegación. |
| **H04** | 4. Creación en modo avión | Cerrar en S13 | **Cerrado** | Esquema SQLDelight ampliado (`2.sqm`) con `intentos`, `ultimoError`, `falloPermanente`, `proximoReintentoEpochMillis` y `claveIdempotencia`. |
| **H05** | 5. Reconexión y sincronización | Cerrar en S13 | **Cerrado** | Algoritmo de reintentos por fila con backoff exponencial (1s, 2s, 4s, 8s, 16s tope), límite de 5 intentos y preservación transaccional. |
| **H06** | 6. Validación del servidor | Cerrar en S13 | **Cerrado** | Errores 400 y 409 clasificados como `ResultadoEnvio.Permanente`; se excluyen del reenvío automático y ofrecen botón de reintento manual en UI. |
| **H07** | 7. Token vencido en formulario | Cerrar en S13 | **Cerrado** | Detección de HTTP 401 invalida la sesión en curso pero preserva intactas las filas locales pendientes en SQLite para reintento tras reautenticación. |
| **H08** | 8. Logout y cambio de usuario | Posponer a S14 | **Cerrado** | Cierre de sesión purga llaves JWT en `AlmacenSeguro` y reinicia estado reactivo en UI. |

#### Limitaciones Declaradas:
1. **L01 — Ejecución física en dos teléfonos sin host conectado:** El recorrido fue verificado funcionalmente en emulador Android API 36 (`Pixel_5`), confirmando la ausencia de excepciones y arranque limpio. La instalación física en dos terminales de marcas distintas requiere transferencia manual ad hoc del APK de entrega.
2. **L02 — Compilación y archivo IPA en iOS:** Al ejecutarse el entorno de desarrollo sobre host Windows 11 sin estación macOS ni Xcode con cuenta Apple Developer, se declara la limitación técnica de empaquetado IPA, preservando la compatibilidad de código en `iosMain`.

#### Declaración de Congelación de Alcance:
Queda formalmente **CONGELADO** el alcance funcional y técnico del producto: no se añadirán nuevas librerías, dependencias, pantallas, migraciones de esquema ni cambios de arquitectura a partir de este punto, asegurando la reproducibilidad total de los builds finales.
