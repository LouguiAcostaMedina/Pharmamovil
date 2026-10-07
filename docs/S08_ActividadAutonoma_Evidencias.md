# EVIDENCIAS DE LA ACTIVIDAD AUTÓNOMA 08

[EVIDENCIA 1 - GET LISTAR 200]
[PEGAR CAPTURA AQUÍ]
Se ha comprobado que el listado de productos funciona y devuelve un objeto de tipo PaginaResponseDto con una lista y el código HTTP 200 OK.

[EVIDENCIA 2 - GET POR ID 200]
[PEGAR CAPTURA AQUÍ]
El endpoint devuelve un ProductoResponseDto correcto con HTTP 200.

[EVIDENCIA 3 - POST 201]
[PEGAR CAPTURA AQUÍ]
Al registrar un producto con un JSON válido se obtiene un código HTTP 201 Created y el nuevo ID asignado.

[EVIDENCIA 4 - PUT 200]
[PEGAR CAPTURA AQUÍ]
La modificación de un producto existente procesa los datos correctamente y retorna HTTP 200.

[EVIDENCIA 5 - DELETE 204]
[PEGAR CAPTURA AQUÍ]
La eliminación lógica del producto con ID existente responde con HTTP 204 No Content.

[EVIDENCIA 6 - VALIDACIÓN NOMBRE 400]
[PEGAR CAPTURA AQUÍ]
Al enviar un nombre de 2 caracteres, el backend devuelve 400 Bad Request con el mensaje en map "El nombre debe tener entre 3 y 150 caracteres".

[EVIDENCIA 7 - PRECIO INVALIDO 400]
[PEGAR CAPTURA AQUÍ]
Al enviar un precio negativo, el backend devuelve 400 Bad Request indicando que el precio debe ser mayor que cero.

[EVIDENCIA 8 - RECURSO INEXISTENTE 404]
[PEGAR CAPTURA AQUÍ]
Consultar o intentar eliminar un ID inexistente, como el 9999, devuelve HTTP 404 Not Found.

[EVIDENCIA 9 - DOBLE DELETE 404]
[PEGAR CAPTURA AQUÍ]
Al intentar eliminar un producto que ya fue eliminado (inactivo), el servidor retorna un error (como 409 Conflicto).

[EVIDENCIA 10 - CONFLICTO 409]
[PEGAR CAPTURA AQUÍ]
El servidor retorna HTTP 409 cuando se intenta dar de baja un producto que ya está inactivo: "El producto X ya se encuentra inactivo".

[EVIDENCIA 11 - SIN CONEXIÓN]
[PEGAR CAPTURA AQUÍ]
Al detener el backend, la aplicación muestra el mensaje "No hay conexión a internet".

[EVIDENCIA 12 - TIMEOUT]
[PEGAR CAPTURA AQUÍ]
Al cambiar el timeout a 1L, se genera una HttpRequestTimeoutException que es convertida a ErrorApi.TiempoAgotado.

[EVIDENCIA 13 - CANCELACIÓN]
[PEGAR CAPTURA AQUÍ]
Las CancellationExceptions son relanzadas correctamente y no devuelven falsos errores 500 o 400 al usuario.

[EVIDENCIA 14 - KTOR FALLIDO]
[PEGAR CAPTURA AQUÍ]
Los logs del cliente Ktor registran la respuesta de error 400 del servidor.

[EVIDENCIA 15 - COMMONTEST SUCCESSFUL]
[PEGAR CAPTURA AQUÍ]
Las 5 pruebas de ProductoViewModelTest pasan exitosamente (Cargando, Sin Productos, Error, Errores de Validación, Cambio de Estado al Eliminar).

[EVIDENCIA 16 - ANDROID]
[PEGAR CAPTURA AQUÍ]
Build exitoso de Android: :androidApp:assembleDebug con la app corriendo.

[EVIDENCIA 17 - iOS]
[PEGAR CAPTURA AQUÍ]
El código en :shared es multiplatforma por lo que compila para iosX64, iosArm64, iosSimulatorArm64 (comprobación en Mac teórica).

[EVIDENCIA 18 - RAMA Y COMMITS]
[PEGAR CAPTURA AQUÍ]
Se muestran los commits detallados en la rama feature/crud-productos-acosta.

[EVIDENCIA EXTRA - NUEVO DISEÑO]
[PEGAR CAPTURA AQUÍ]
El aplicativo cuenta con un rediseño general en base a Material 3 con bordes curvos y paletas armónicas.

[EVIDENCIA EXTRA - MODO CLARO]
[PEGAR CAPTURA AQUÍ]
El modo claro utiliza los colores de Pharma Teal & Fresh Surfaces.

[EVIDENCIA EXTRA - MODO OSCURO]
[PEGAR CAPTURA AQUÍ]
El modo oscuro cuenta con fondos adaptados, un toggle visual nuevo y colores calibrados.

[EVIDENCIA EXTRA - FORMULARIO CLIENTES]
[PEGAR CAPTURA AQUÍ]
Se ha implementado una UI moderna para registrar clientes.

[EVIDENCIA EXTRA - FORMULARIO PEDIDOS]
[PEGAR CAPTURA AQUÍ]
Se ha implementado una UI completa para crear pedidos, aún sin backend, con su estado vacío correspondiente.
