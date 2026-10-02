[EVIDENCIA 1 - LISTAR]
- Qué ejecutar: Abrir la aplicación y esperar la carga inicial.
- Qué debe verse: El listado real de productos proveniente del backend.

[EVIDENCIA 2 - CREAR]
- Qué ejecutar: Llenar el formulario con datos válidos y hacer clic en "Registrar".
- Qué debe verse: Indicador de carga en el botón, mensaje de éxito en verde y recarga automática de la lista con el nuevo producto.

[EVIDENCIA 3 - ACTUALIZAR]
- Qué ejecutar: Clic en "Editar" en un producto, modificar el precio o stock, y hacer clic en "Actualizar".
- Qué debe verse: Indicador de carga, mensaje de éxito en verde y lista actualizada con los nuevos valores.

[EVIDENCIA 4 - ELIMINAR]
- Qué ejecutar: Clic en "Eliminar" en un producto.
- Qué debe verse: Indicador de carga general o deshabilito de la UI, mensaje de éxito en verde y producto desaparecido de la lista.

[EVIDENCIA 5 - ERROR 400]
- Qué ejecutar: Ingresar un nombre de 2 caracteres y presionar "Registrar".
- Qué debe verse: HTTP 400 devuelto por backend, y un texto en rojo debajo del campo nombre diciendo "El nombre debe tener entre 3 y 150 caracteres".

[EVIDENCIA 6 - KTOR ÉXITO]
- Qué ejecutar: Mirar Logcat (Android) o Consola (Desktop).
- Qué debe verse: Log de Ktor (REQUEST y RESPONSE) con código HTTP 200 o 201.

[EVIDENCIA 7 - KTOR FALLIDO]
- Qué ejecutar: Mirar Logcat/Consola tras forzar el error 400.
- Qué debe verse: Log de Ktor con código HTTP 400 Bad Request y el JSON con validationErrors.

[EVIDENCIA 8 - ANDROID]
- Qué ejecutar: .\gradlew.bat :androidApp:assembleDebug
- Qué debe verse: Build Successful y la app ejecutándose en emulador.

[EVIDENCIA 9 - iOS]
- Qué ejecutar: Abrir en XCode o ejecutar en mac (o en simulador si aplica).
- Qué debe verse: Build exitoso en código común y UI. (Solo teórico si es en Windows).

[EVIDENCIA 10 - GIT]
- Qué ejecutar: git log y git status
- Qué debe verse: Rama feature/crud-productos-acosta con todos los commits pequeños y push realizado.
