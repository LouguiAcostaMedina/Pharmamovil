package pe.edu.upeu.pharmamobile.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.model.ClienteResponseDto
import pe.edu.upeu.pharmamobile.data.model.DetalleVentaRequestDto
import pe.edu.upeu.pharmamobile.data.model.ErrorResponseDto
import pe.edu.upeu.pharmamobile.data.model.ProductoRequestDto
import pe.edu.upeu.pharmamobile.data.model.VentaRequestDto
import pe.edu.upeu.pharmamobile.data.repository.authProvider
import pe.edu.upeu.pharmamobile.domain.model.Sesion

@Serializable
data class ResultadoDiagnostico(
    val escenario: String,
    val usuario: String,
    val metodo: String,
    val ruta: String,
    val codigoHttp: Int,
    val mensaje: String,
    val estadoSesion: String
)

class DiagnosticoS10Api(
    private val clienteAutenticado: HttpClient,
    private val clienteDiagnostico: HttpClient,
    private val almacen: AlmacenSeguro
) {

    private suspend fun parsearMensajeError(response: HttpResponse, fallback: String): String {
        return try {
            val dto = response.body<ErrorResponseDto>()
            dto.message ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private suspend fun parsearMensajeClientRequest(e: ClientRequestException): String {
        return try {
            val dto = e.response.body<ErrorResponseDto>()
            dto.message ?: e.message
        } catch (_: Exception) {
            e.message
        }
    }

    // ==========================================
    // SEIS ESCENARIOS DE TOKEN
    // ==========================================

    suspend fun ejecutarE01TokenValido(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("productos")
            ResultadoDiagnostico(
                escenario = "E01: Token Válido",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = response.status.value,
                mensaje = "Operación completada exitosamente",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "E01: Token Válido",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        } catch (e: Exception) {
            ResultadoDiagnostico(
                escenario = "E01: Token Válido",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = 0,
                mensaje = e.message ?: "Fallo de conexión",
                estadoSesion = "Activa"
            )
        }
    }

    suspend fun ejecutarE02TokenExpirado(
        sesionActual: Sesion?,
        onSesionInvalida: (String) -> Unit
    ): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("productos")
            ResultadoDiagnostico(
                escenario = "E02: Token Expirado",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = response.status.value,
                mensaje = "El token sigue siendo válido en el servidor",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            val mensaje = parsearMensajeClientRequest(e)
            if (e.response.status == HttpStatusCode.Unauthorized) {
                onSesionInvalida(mensaje)
                ResultadoDiagnostico(
                    escenario = "E02: Token Expirado",
                    usuario = usuario,
                    metodo = "GET",
                    ruta = "/api/v1/productos",
                    codigoHttp = 401,
                    mensaje = mensaje,
                    estadoSesion = "Sesión cerrada y limpiada"
                )
            } else {
                ResultadoDiagnostico(
                    escenario = "E02: Token Expirado",
                    usuario = usuario,
                    metodo = "GET",
                    ruta = "/api/v1/productos",
                    codigoHttp = e.response.status.value,
                    mensaje = mensaje,
                    estadoSesion = "Activa"
                )
            }
        }
    }

    suspend fun ejecutarE03TokenAlterado(
        sesionActual: Sesion?,
        onSesionInvalida: (String) -> Unit
    ): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        val tokenOriginal = almacen.leerToken()
        if (tokenOriginal.isNullOrBlank()) {
            return ResultadoDiagnostico(
                escenario = "E03: Token Alterado",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = 0,
                mensaje = "No hay token en almacén para alterar. Inicie sesión primero.",
                estadoSesion = "Sin sesión"
            )
        }

        // Alterar un carácter significativo en la firma del token sin exponerlo en texto plano
        val tokenAlterado = if (tokenOriginal.endsWith("X")) {
            tokenOriginal.dropLast(1) + "Y"
        } else {
            tokenOriginal.dropLast(1) + "X"
        }

        // Guardar token alterado en el almacén seguro
        almacen.guardarToken(tokenAlterado)

        // Invalidar caché BearerAuthProvider para forzar el envío del token alterado
        clienteAutenticado.authProvider<BearerAuthProvider>()?.clearToken()

        return try {
            val response = clienteAutenticado.get("productos")
            ResultadoDiagnostico(
                escenario = "E03: Token Alterado",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = response.status.value,
                mensaje = "Inesperado: Token aceptado",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            val mensaje = parsearMensajeClientRequest(e)
            if (e.response.status == HttpStatusCode.Unauthorized) {
                onSesionInvalida(mensaje)
                ResultadoDiagnostico(
                    escenario = "E03: Token Alterado",
                    usuario = usuario,
                    metodo = "GET",
                    ruta = "/api/v1/productos",
                    codigoHttp = 401,
                    mensaje = mensaje,
                    estadoSesion = "Sesión cerrada y limpiada"
                )
            } else {
                ResultadoDiagnostico(
                    escenario = "E03: Token Alterado",
                    usuario = usuario,
                    metodo = "GET",
                    ruta = "/api/v1/productos",
                    codigoHttp = e.response.status.value,
                    mensaje = mensaje,
                    estadoSesion = "Activa"
                )
            }
        }
    }

    suspend fun ejecutarE04SinCabecera(): ResultadoDiagnostico {
        // Usa cliente exclusivo de diagnóstico sin plugin Auth
        val response = clienteDiagnostico.get("productos")
        val mensaje = parsearMensajeError(response, "Debe iniciar sesión para acceder a este recurso")
        return ResultadoDiagnostico(
            escenario = "E04: Sin Cabecera",
            usuario = "Anónimo",
            metodo = "GET",
            ruta = "/api/v1/productos",
            codigoHttp = response.status.value,
            mensaje = mensaje,
            estadoSesion = "Sin sesión"
        )
    }

    suspend fun ejecutarE05PrefijoIncorrecto(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        val token = almacen.leerToken() ?: ""
        if (token.isBlank()) {
            return ResultadoDiagnostico(
                escenario = "E05: Prefijo Incorrecto",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = 0,
                mensaje = "Se requiere una sesión activa con token para enviar con prefijo 'bearer '",
                estadoSesion = "Sin sesión"
            )
        }

        // Usa cliente exclusivo de diagnóstico sin plugin Auth, enviando manualmente 'bearer ' en minúsculas
        val response = clienteDiagnostico.get("productos") {
            header(HttpHeaders.Authorization, "bearer $token")
        }
        val mensaje = parsearMensajeError(response, "Debe iniciar sesión para acceder a este recurso")

        return ResultadoDiagnostico(
            escenario = "E05: Prefijo Incorrecto",
            usuario = usuario,
            metodo = "GET",
            ruta = "/api/v1/productos",
            codigoHttp = response.status.value,
            mensaje = mensaje,
            estadoSesion = "Activa (el almacén conserva el token intacto)"
        )
    }

    suspend fun ejecutarE06UsuarioDeshabilitado(
        sesionActual: Sesion?,
        onSesionInvalida: (String) -> Unit
    ): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("productos")
            ResultadoDiagnostico(
                escenario = "E06: Usuario Deshabilitado",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = response.status.value,
                mensaje = "El usuario sigue habilitado en la base de datos",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            val mensaje = parsearMensajeClientRequest(e)
            if (e.response.status == HttpStatusCode.Unauthorized) {
                onSesionInvalida(mensaje)
                ResultadoDiagnostico(
                    escenario = "E06: Usuario Deshabilitado",
                    usuario = usuario,
                    metodo = "GET",
                    ruta = "/api/v1/productos",
                    codigoHttp = 401,
                    mensaje = mensaje,
                    estadoSesion = "Sesión cerrada y limpiada"
                )
            } else {
                ResultadoDiagnostico(
                    escenario = "E06: Usuario Deshabilitado",
                    usuario = usuario,
                    metodo = "GET",
                    ruta = "/api/v1/productos",
                    codigoHttp = e.response.status.value,
                    mensaje = mensaje,
                    estadoSesion = "Activa"
                )
            }
        }
    }

    // ==========================================
    // SEIS OPERACIONES DE LA MATRIZ DE PERMISOS
    // ==========================================

    suspend fun opListarProductos(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("productos")
            ResultadoDiagnostico(
                escenario = "Matriz: Listar productos",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = response.status.value,
                mensaje = "Catálogo listado con éxito",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "Matriz: Listar productos",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/productos",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        }
    }

    suspend fun opRegistrarProducto(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        val request = ProductoRequestDto(
            nombre = "Prod Matriz S10",
            precio = 12.50,
            stock = 20,
            estado = true,
            categoriaId = 1L
        )
        return try {
            val response = clienteAutenticado.post("productos") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ResultadoDiagnostico(
                escenario = "Matriz: Registrar producto",
                usuario = usuario,
                metodo = "POST",
                ruta = "/api/v1/productos",
                codigoHttp = response.status.value,
                mensaje = "Producto registrado exitosamente",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "Matriz: Registrar producto",
                usuario = usuario,
                metodo = "POST",
                ruta = "/api/v1/productos",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        }
    }

    suspend fun opListarClientes(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("clientes")
            ResultadoDiagnostico(
                escenario = "Matriz: Listar clientes",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/clientes",
                codigoHttp = response.status.value,
                mensaje = "Clientes listados con éxito",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "Matriz: Listar clientes",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/clientes",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        }
    }

    suspend fun opRegistrarVenta(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        val request = VentaRequestDto(
            clienteId = 1L,
            detalles = listOf(DetalleVentaRequestDto(productoId = 1L, cantidad = 1))
        )
        return try {
            val response = clienteAutenticado.post("ventas") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ResultadoDiagnostico(
                escenario = "Matriz: Registrar venta",
                usuario = usuario,
                metodo = "POST",
                ruta = "/api/v1/ventas",
                codigoHttp = response.status.value,
                mensaje = "Venta registrada exitosamente",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "Matriz: Registrar venta",
                usuario = usuario,
                metodo = "POST",
                ruta = "/api/v1/ventas",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        }
    }

    suspend fun opVerReportes(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("reportes/ventas-por-categoria")
            ResultadoDiagnostico(
                escenario = "Matriz: Ver reportes",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/reportes/ventas-por-categoria",
                codigoHttp = response.status.value,
                mensaje = "Reporte de ventas por categoría obtenido exitosamente",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "Matriz: Ver reportes",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/reportes/ventas-por-categoria",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        }
    }

    suspend fun opConsultarAuthMe(sesionActual: Sesion?): ResultadoDiagnostico {
        val usuario = sesionActual?.usuario ?: "desconocido"
        return try {
            val response = clienteAutenticado.get("auth/me")
            ResultadoDiagnostico(
                escenario = "Matriz: Consultar /auth/me",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/auth/me",
                codigoHttp = response.status.value,
                mensaje = "Identidad y permisos confirmados",
                estadoSesion = "Activa"
            )
        } catch (e: ClientRequestException) {
            ResultadoDiagnostico(
                escenario = "Matriz: Consultar /auth/me",
                usuario = usuario,
                metodo = "GET",
                ruta = "/api/v1/auth/me",
                codigoHttp = e.response.status.value,
                mensaje = parsearMensajeClientRequest(e),
                estadoSesion = if (e.response.status == HttpStatusCode.Unauthorized) "Inválida" else "Activa"
            )
        }
    }
}
