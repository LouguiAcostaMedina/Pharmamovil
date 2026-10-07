# PORTADA
**Estudiante:** Lougui Andre Acosta Medina
**Curso:** Desarrollo de Aplicaciones Móviles
**Sesión:** 08
**Proyecto:** PharmaMobil + PharmaSoft
**Rama:** feature/crud-productos-acosta

---

## 1. Matriz CRUD (Ejecución real en backend)
- **GET listado:** HTTP 200, devuelve JSON con la lista de productos y paginación.
- **GET por ID:** HTTP 200, devuelve JSON de un ProductoRequestDto.
- **POST:** HTTP 201, se envía JSON de producto y devuelve el ID generado.
- **PUT:** HTTP 200, devuelve el producto modificado.
- **DELETE:** HTTP 204 (o 409 si ya estaba eliminado).

## 2. Bitácora de 8 errores provocados
1. **Nombre inválido (2 caracteres):** Retorna HTTP 400 y mensaje en UI.
2. **Precio inválido (-5.0):** Retorna HTTP 400 y mensaje "El precio debe ser mayor que cero".
3. **ID inexistente (9999):** Retorna HTTP 404 No encontrado.
4. **Doble eliminación:** El primer DELETE es exitoso, el segundo retorna HTTP 409.
5. **Regla de negocio (Conflicto 409):** Intentar eliminar un producto ya inactivo arroja ErrorApi.Conflicto.
6. **Servidor caído:** Simulado apagando PharmaSoft, Ktor devuelve IOException (ErrorApi.SinConexion).
7. **Timeout:** Cambiando requestTimeoutMillis = 1L, Ktor lanza Exception transformada en ErrorApi.TiempoAgotado.
8. **Cancelación:** CancellationException de corrutina es relanzada, sin alterar el UIState con errores genéricos.

## 3. Pruebas commonTest
Se ampliaron las pruebas de `ProductoViewModelTest` para cubrir:
1. Cargando -> ConProductos
2. Cargando -> SinProductos
3. Repositorio con error -> Error genérico
4. ErrorApi.Validacion (extracción de nombreError, precioError)
5. Eliminación de Producto: operación Inactiva post-eliminación.

## 4. Evidencias
Ver archivo `S08_ActividadAutonoma_Evidencias.md`.

## 5. Repositorio
Commit pusheados a la rama correspondiente `feature/crud-productos-acosta` tanto para móvil como para backend `feature/h2-local-dev`.

## 6. Mejoras UI/UX realizadas
- **Material 3:** Implementado Theme de colores oscuros/claros personalizados.
- **Tipografía:** Se centralizó una tipografía `PharmaTypography` consistente en la app.
- **Modo Claro/Oscuro:** El toggle switch ahora tiene una interfaz de segmented control con íconos de sol y luna.
- **Componentes:** Rediseño completo de la tarjeta de productos (Stock pills, Badges).
- **Formulario Clientes:** Creada UI sin CRUD.
- **Formulario Pedidos:** Creada UI sin CRUD.
- **Branding:** InicioScreen con logo gigante y diseño limpio corporativo.
