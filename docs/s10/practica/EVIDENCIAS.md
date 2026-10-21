# Evidencias de Verificación — Práctica S10: Autenticación JWT y Protección Ktor
**Alumno:** Lougui Acosta Medina  
**Curso:** Desarrollo de Aplicaciones Móviles — UPeU  
**Fecha:** 14/10/2026  
**Entorno de Trabajo:** Windows 10/11 x64, PowerShell 5.1, Java 21, Gradle 8.x, Android SDK 34/35.

---

## 1. Evidencias del Backend Spring Boot (PharmaSoft)

### Evidencia 1.1: Solicitud a Endpoint Protegido sin Token (HTTP 401 Unauthorized)
**Comando ejecutado en PowerShell:**
```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/productos" -Method Get
} catch {
    Write-Host "STATUS_SIN_TOKEN: $($_.Exception.Response.StatusCode.value__)"
}
```
**Salida observada:**
```text
STATUS_SIN_TOKEN: 401
```

---

### Evidencia 1.2: Login Exitoso con Usuario `admin` (HTTP 200 OK)
**Comando ejecutado:**
```powershell
$loginAdmin = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method Post `
    -Body '{"username":"admin","password":"Admin123"}' `
    -ContentType "application/json"
$loginAdmin | Select-Object username, nombreCompleto, tipo, expiraEnSegundos, roles, permisos | Format-List
```
**Salida observada:**
```text
username         : admin
nombreCompleto   : Carlos Ramirez Torres
tipo             : Bearer
expiraEnSegundos : 3600
roles            : {ADMIN}
permisos         : {CATALOGO_ESCRIBIR, CATALOGO_LEER, CLIENTES_ESCRIBIR, CLIENTES_LEER...}
```

---

### Evidencia 1.3: Consulta Protegida de Catálogo con Token de `admin` (HTTP 200 OK)
**Comando ejecutado:**
```powershell
$prods = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/productos" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $($loginAdmin.token)" }
Write-Host "Total productos devueltos: $($prods.totalElementos)"
$prods.contenido | Select-Object id, nombre, precio, stock | Format-Table
```
**Salida observada:**
```text
Total productos devueltos: 9

id nombre                   precio stock
-- ------                   ------ -----
 1 Paracetamol 500mg          2.50   100
 2 Ibuprofeno 400mg           3.00    50
 3 Amoxicilina 500mg          5.00    30
 4 otimax                     5.80   100
 5 oxicodona                  3.50    45
36 Test Producto Modificado  15.00    30
37 diclofenaco                5.50    30
38 omeprazol                 10.00    56
39 OPIO                      52.00    12
```
*Comentario:* Se valida que la base de datos H2 local `./data/pharmadb.mv.db` conserva los 9 registros de productos creados en las sesiones previas (S07-S09).

---

### Evidencia 1.4: Login con Usuario `almacen` y Violación de Permiso en Clientes (HTTP 403 Forbidden)
**Comando ejecutado:**
```powershell
$loginAlmacen = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method Post `
    -Body '{"username":"almacen","password":"Almacen123"}' `
    -ContentType "application/json"

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/clientes" `
        -Method Get `
        -Headers @{ Authorization = "Bearer $($loginAlmacen.token)" }
} catch {
    Write-Host "STATUS_ALMACEN_CLIENTES: $($_.Exception.Response.StatusCode.value__)"
}
```
**Salida observada:**
```text
STATUS_ALMACEN_CLIENTES: 403
```
*Comentario:* El usuario `almacen` posee el rol `ALMACENERO` y permisos de catálogo (`CATALOGO_LEER`, `CATALOGO_ESCRIBIR`), pero carece de `CLIENTES_LEER`. El servidor deniega el acceso con HTTP 403 manteniendo la sesión activa.

---

### Evidencia 1.5: Verificación de Identidad con `GET /api/v1/auth/me`
**Comando ejecutado:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/me" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $($loginAdmin.token)" }
```
**Salida observada:**
```text
id             : 1
username       : admin
empleadoId     : 1
empleadoNombre : Carlos Ramirez Torres
cargo          : Administrador
roles          : {ADMIN}
permisos       : {CATALOGO_ESCRIBIR, CATALOGO_LEER, CLIENTES_ESCRIBIR, CLIENTES_LEER...}
estado         : True
fechaCreacion  : 2026-10-14T11:42:31.164087
```

---

## 2. Evidencias del Cliente Kotlin Multiplatform (PharmaMobile)

### Evidencia 2.1: Pruebas Unitarias de Repositorio y Casos de Uso (`desktopTest`)
**Comando ejecutado:**
```powershell
.\gradlew.bat :shared:desktopTest
```
**Salida observada:**
```text
> Task :shared:compileKotlinDesktop UP-TO-DATE
> Task :shared:desktopJar UP-TO-DATE
> Task :shared:compileTestKotlinDesktop
> Task :shared:desktopTest

BUILD SUCCESSFUL in 2m 22s
17 actionable tasks: 7 executed, 10 up-to-date
```
**Detalle de Suites Ejecutadas (`build/reports/tests/desktopTest/index.html`):**
- `AuthRepositorioRestTest`:
  - `login_exitoso_guarda_token_y_retorna_sesion`: PASÓ
  - `login_con_credenciales_invalidas_lanza_CredencialesInvalidas`: PASÓ
  - `cerrar_sesion_borra_token_del_almacen`: PASÓ
  - `sesionGuardada_reconstruye_sesion_con_auth_me`: PASÓ
  - `sesionGuardada_con_token_expirado_401_limpia_almacen_y_retorna_null`: PASÓ
- `ProductoViewModelTest`: PASÓ (pruebas de CRUD, carga de estados y compartidor).
- `Total Tests Ejecutados`: 18 tests. **0 fallos**.

---

### Evidencia 2.2: Compilación y Empaquetado Android (`:androidApp:assembleDebug`)
**Comando ejecutado:**
```powershell
.\gradlew.bat :androidApp:assembleDebug
```
**Salida observada:**
```text
> Task :androidApp:compileDebugKotlin
> Task :androidApp:mergeLibDexDebug
> Task :androidApp:dexBuilderDebug
> Task :androidApp:mergeProjectDexDebug
> Task :androidApp:packageDebug
> Task :androidApp:assembleDebug

BUILD SUCCESSFUL in 1h 7m 43s
57 actionable tasks: 52 executed, 5 up-to-date
```
- **APK Resultante:** `androidApp\build\outputs\apk\debug\androidApp-debug.apk`

---

## 3. Estado de la Plataforma iOS

- **Implementación de Código:** Completa. Se desarrolló `AlmacenSeguroIos.kt` utilizando la API C de Apple `Security.framework` (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`) con servicio `pe.edu.upeu.pharmamobile.auth` y cuenta `jwt_access_token`.
- **Estado de Ejecución:** `PENDIENTE ENTORNO` (No ejecutado). La consigna prohíbe inventar resultados o capturas en plataformas no ejecutadas. Requiere ser compilado y ejecutado en una máquina con macOS y Xcode instalado.

---

## 4. Guía para la Obtención de Capturas en Emulador Android

Para adjuntar en la carpeta `docs/s10/practica/capturas/`:

1. **`01_login_pantalla.png`**:
   - Abrir la app en el emulador Android.
   - Observar el formulario de Login con los campos de usuario y contraseña oculta.
2. **`02_login_error_credenciales.png`**:
   - Ingresar `admin` con clave errada `1234`.
   - Pulsar "Iniciar Sesión" y capturar el mensaje de error: *"Usuario o contraseña incorrectos"*.
3. **`03_catalogo_admin.png`**:
   - Iniciar sesión con `admin` / `Admin123`.
   - Capturar el listado con los 9 productos y el botón flotante `+` disponible para agregar productos.
4. **`04_catalogo_vendedor_lectura.png`**:
   - Cerrar sesión y acceder con `vendedor` / `Vendedor123`.
   - Capturar el listado observando el banner: *"Modo solo lectura: Su rol (VENDEDOR) no tiene permisos de edición de catálogo."* y la ausencia de botones de edición/eliminación.
5. **`05_error_403_almacen.png`**:
   - Iniciar sesión con `almacen` / `Almacen123`.
   - En la tarjeta de depuración S10, pulsar el botón *"Probar 403 (Clientes)"*.
   - Capturar el mensaje en pantalla: *"Acceso Denegado (HTTP 403): Su usuario no cuenta con el permiso CLIENTES_LEER."* y verificar que la sesión del usuario no se cerró.
6. **`06_log_sanitizado.png`**:
   - En Logcat (filtrando por `HttpClient`), capturar la cabecera `Authorization: Bearer ***` comprobando que el token real no se expone en los registros.
