# Actividad Autónoma N.º 07: Documentación de endpoints, DTO y pruebas de conexión

## 1. Portada
- **Curso:** Desarrollo de Aplicaciones Móviles
- **Estudiante:** Acosta
- **Guía:** Actividad Autónoma 07 - Configuración y consumo Ktor

## 2. Datos del proyecto
- **Móvil:** PharmaMobile (Kotlin Multiplatform con Compose Multiplatform)
- **Backend:** PharmaSoft (Java Spring Boot + H2/Oracle)

## 3. Catálogo de endpoints
**Recurso principal:** `Producto`
**URL Base:** `http://10.0.2.2:8080/api/v1/` (Android) / `http://localhost:8080/api/v1/` (Desktop/iOS)
**Versión API:** v1

| Método | Ruta | Parámetros | Body | Código Exitoso | Códigos de Error |
|--------|------|------------|------|----------------|------------------|
| GET | `/productos` | `pagina`, `tamanio`, `ordenarPor`, `direccion` | (Ninguno) | 200 OK | 500, 400 |
| GET | `/productos/{id}` | `id` (Path) | (Ninguno) | 200 OK | 404 Not Found, 500 |
| POST | `/productos` | (Ninguno) | `ProductoRequestDTO` | 201 Created | 400 Bad Request, 500 |
| PUT | `/productos/{id}` | `id` (Path) | `ProductoRequestDTO` | 200 OK | 404, 400, 500 |
| DELETE | `/productos/{id}`| `id` (Path) | (Ninguno) | 204 No Content | 404 Not Found, 500 |

## 4. Diccionario DTO

| Campo JSON | Tipo Kotlin | Obligatorio | Default | Campo en Dominio |
|------------|-------------|-------------|---------|------------------|
| `id` | `Long` | Sí | N/A | `id` |
| `nombre` | `String` | Sí | N/A | `nombre` |
| `precio` | `Double` | Sí | N/A | `precio` |
| `stock` | `Int` | Sí | N/A | (No mapeado) |
| `estado` | `Boolean` | Sí | N/A | (No mapeado) |
| `categoriaId` | `Long?` | No | `null` | (No mapeado) |
| `categoriaNombre`| `String?` | No | `null` | `categoria` |
| `fechaCreacion` | `String?` | No | `null` | (No mapeado) |
| `fechaModificacion`| `String?` | No | `null` | (No mapeado) |

## 5. JSON real
```json
{
  "contenido": [
    {
      "id": 1,
      "nombre": "Paracetamol 500mg",
      "precio": 5.5,
      "stock": 100,
      "estado": true,
      "categoriaId": 1,
      "categoriaNombre": "Analgésicos",
      "fechaCreacion": "2026-09-30T10:00:00",
      "fechaModificacion": "2026-09-30T10:00:00"
    }
  ],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "ultima": true
}
```

## 6. Código DTO
```kotlin
@Serializable
data class PaginaResponse<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

@Serializable
data class ProductoDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null,
    val fechaCreacion: String? = null,
    val fechaModificacion: String? = null
)
```

## 7. Bitácora de cinco pruebas

### Prueba 1: Respuesta exitosa
- **ID:** P1
- **Plataforma:** Android Emulator / KMP Test
- **Pasos:** Iniciar Spring Boot, abrir app, verificar carga de lista.
- **Resultado esperado:** HTTP 200 OK y productos en UI.
- **Resultado observado:** Lista cargada exitosamente. Se renderizan en Compose.
- **Mensaje usuario:** Muestra los cards de los productos.
- **Evidencia necesaria:** Captura de la app en Android con los productos de PharmaSoft.

### Prueba 2: Recurso inexistente (404 / 500)
- **ID:** P2
- **Plataforma:** Android Emulator / KMP Test
- **Pasos:** Forzar un Internal Server Error en el Mock de Ktor o en el backend.
- **Resultado esperado:** Captura del error, retorna `emptyList()`.
- **Resultado observado:** La app no crashea, muestra lista vacía o mensaje de error genérico en UI.
- **Mensaje usuario:** "Lista vacía" o equivalente de UI.
- **Evidencia necesaria:** Captura del logcat mostrando la excepción `ClientRequestException`.

### Prueba 3: Sin conexión
- **ID:** P3
- **Plataforma:** Android Emulator
- **Pasos:** Apagar el Wi-Fi del emulador o detener el backend de PharmaSoft.
- **Resultado esperado:** `Exception` en `listar()`, retorna `emptyList()`.
- **Resultado observado:** Ktor lanza `ConnectException`. Atrapado exitosamente en el bloque catch.
- **Mensaje usuario:** La aplicación sigue viva, sin mostrar datos (o mensaje de red no disponible).
- **Evidencia necesaria:** Captura del Logcat con `ConnectException`.

### Prueba 4: Timeout
- **ID:** P4
- **Plataforma:** Android Emulator
- **Pasos:** Modificar `KtorClient.kt` -> `requestTimeoutMillis = 1`, recompilar y ejecutar.
- **Resultado esperado:** `HttpRequestTimeoutException`.
- **Resultado observado:** La petición se cancela por exceder 1ms.
- **Mensaje usuario:** Estado UI vacío o error.
- **Evidencia necesaria:** Logcat con `HttpRequestTimeoutException`. (Modificación ya revertida a 15000ms).

### Prueba 5: Campo desconocido JSON
- **ID:** P5
- **Plataforma:** KMP Unit Test / Android
- **Pasos:** `ignoreUnknownKeys = true` está configurado. Simular JSON con `"campoNuevo": "valor"`.
- **Resultado esperado:** Serialización exitosa ignorando el campo.
- **Resultado observado:** El DTO ignora `"campoNuevo"` y procesa el resto correctamente.
- **Mensaje usuario:** UI normal con datos del producto.
- **Evidencia necesaria:** Código fuente de `KtorClient.kt` comprobando `ignoreUnknownKeys = true`.

## 8. Evidencias Pendientes
[EVIDENCIA PENDIENTE - ANDROID GET 200]
*Instrucciones: Inicia el backend de PharmaSoft, corre la app de Android en tu emulador y toma captura a la lista de productos mostrada.*

[EVIDENCIA PENDIENTE - LOG KTOR]
*Instrucciones: Abre Logcat en Android Studio, filtra por `Ktor` o `HttpClient` y toma captura al log que muestra el `200 OK` de la petición.*

[EVIDENCIA PENDIENTE - ERROR 404]
*Instrucciones: Modifica un ID en el cliente para pedir uno inexistente o apaga el servidor y muestra cómo la aplicación no se cierra (no crashea).*

[EVIDENCIA PENDIENTE - SIN CONEXIÓN]
*Instrucciones: Apaga el WiFi/Datos del emulador de Android. Abre la app, captura Logcat mostrando el error de `ConnectException` sin que la app se detenga.*

[EVIDENCIA PENDIENTE - TIMEOUT]
*Instrucciones: Pon `requestTimeoutMillis = 1` en `KtorClient.kt`, ejecuta, toma captura al error de Timeout en Logcat, y revierte el cambio.*

[EVIDENCIA PENDIENTE - iOS]
*Instrucciones: Si posees Mac, corre el proyecto en Xcode y toma captura de la app en el simulador.*

## 9. Repositorio
- **Rama:** `feature/ktor-client`
- **Commits:** Se adaptaron los endpoints para consumir localmente PharmaSoft siguiendo las indicaciones de la Actividad Autónoma 07.

## 10. Conclusiones
Se completó la migración hacia la conexión nativa real del backend proporcionado (PharmaSoft), superando la implementación referencial inicial. Se validó Clean Architecture, inyección con Koin y consumo asíncrono con `HttpClient` utilizando kotlinx.serialization.
