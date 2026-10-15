package pe.edu.upeu.pharmamobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile.data.network.AuthApi
import pe.edu.upeu.pharmamobile.data.network.DiagnosticoS10Api
import pe.edu.upeu.pharmamobile.data.network.ResultadoDiagnostico
import pe.edu.upeu.pharmamobile.domain.model.Credenciales
import pe.edu.upeu.pharmamobile.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobile.domain.model.Sesion
import pe.edu.upeu.pharmamobile.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.ObtenerSesionGuardadaUseCase

class AuthViewModel(
    private val iniciarSesionUseCase: IniciarSesionUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val obtenerSesionGuardadaUseCase: ObtenerSesionGuardadaUseCase,
    private val authApi: AuthApi,
    private val diagnosticoApi: DiagnosticoS10Api? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _sesionState = MutableStateFlow<Sesion?>(null)
    val sesionState: StateFlow<Sesion?> = _sesionState.asStateFlow()

    private val _mensajePermiso = MutableStateFlow<String?>(null)
    val mensajePermiso: StateFlow<String?> = _mensajePermiso.asStateFlow()

    private val _resultadoDiagnostico = MutableStateFlow<ResultadoDiagnostico?>(null)
    val resultadoDiagnostico: StateFlow<ResultadoDiagnostico?> = _resultadoDiagnostico.asStateFlow()

    private val _diagnosticoCargando = MutableStateFlow(false)
    val diagnosticoCargando: StateFlow<Boolean> = _diagnosticoCargando.asStateFlow()

    init {
        verificarSesionGuardada()
    }

    fun onUsuarioChange(valor: String) {
        _uiState.update {
            it.copy(
                usuario = valor,
                usuarioError = if (valor.isBlank()) "El usuario es obligatorio" else null,
                error = null
            )
        }
    }

    fun onClaveChange(valor: String) {
        _uiState.update {
            it.copy(
                clave = valor,
                claveError = if (valor.isBlank()) "La contraseña es obligatoria" else null,
                error = null
            )
        }
    }

    fun toggleClaveVisible() {
        _uiState.update { it.copy(claveVisible = !it.claveVisible) }
    }

    fun iniciarSesion() {
        val usuario = _uiState.value.usuario.trim()
        val clave = _uiState.value.clave.trim()

        var tieneError = false
        if (usuario.isBlank()) {
            _uiState.update { it.copy(usuarioError = "Ingrese su usuario") }
            tieneError = true
        }
        if (clave.isBlank()) {
            _uiState.update { it.copy(claveError = "Ingrese su contraseña") }
            tieneError = true
        }

        if (tieneError) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, avisoExpiracion = null) }
            try {
                val sesion = iniciarSesionUseCase(Credenciales(usuario, clave))
                _sesionState.value = sesion
                _uiState.update {
                    it.copy(
                        cargando = false,
                        sesion = sesion,
                        error = null,
                        clave = ""
                    )
                }
            } catch (e: ErrorSesion) {
                val mensaje = when (e) {
                    is ErrorSesion.CredencialesInvalidas -> "Usuario o contraseña incorrectos"
                    is ErrorSesion.SesionExpirada -> "La sesión ha expirado"
                    is ErrorSesion.SinPermiso -> "No tiene permisos para acceder"
                    is ErrorSesion.SinConexion -> "No se pudo conectar con el servidor. Verifique su red."
                    is ErrorSesion.Desconocido -> e.mensaje
                }
                _uiState.update { it.copy(cargando = false, error = mensaje) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message ?: "Error al autenticar") }
            }
        }
    }

    fun verificarSesionGuardada() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                val sesion = obtenerSesionGuardadaUseCase()
                if (sesion != null) {
                    _sesionState.value = sesion
                    _uiState.update { it.copy(cargando = false, sesion = sesion) }
                } else {
                    _uiState.update { it.copy(cargando = false) }
                }
            } catch (e: ErrorSesion.SinConexion) {
                // Distingue falta de conexión: mantiene estado informativo sin desloguear agresivamente
                _uiState.update { it.copy(cargando = false, error = "Modo sin conexión: no se pudo validar la sesión remota.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    fun cerrarSesion(aviso: String? = null) {
        viewModelScope.launch {
            cerrarSesionUseCase()
            _sesionState.value = null
            _uiState.value = LoginUiState(avisoExpiracion = aviso)
            _mensajePermiso.value = null
        }
    }

    fun notificarSesionExpirada(aviso: String = "Su sesión ha expirado o no es válida. Por favor, vuelva a identificarse.") {
        cerrarSesion(aviso)
    }

    fun ejecutarEscenarioDiagnostico(num: Int) {
        val diag = diagnosticoApi ?: return
        viewModelScope.launch {
            _diagnosticoCargando.value = true
            val resultado = when (num) {
                1 -> diag.ejecutarE01TokenValido(_sesionState.value)
                2 -> diag.ejecutarE02TokenExpirado(_sesionState.value) { aviso -> notificarSesionExpirada(aviso) }
                3 -> diag.ejecutarE03TokenAlterado(_sesionState.value) { aviso -> notificarSesionExpirada(aviso) }
                4 -> diag.ejecutarE04SinCabecera()
                5 -> diag.ejecutarE05PrefijoIncorrecto(_sesionState.value)
                6 -> diag.ejecutarE06UsuarioDeshabilitado(_sesionState.value) { aviso -> notificarSesionExpirada(aviso) }
                else -> null
            }
            _resultadoDiagnostico.value = resultado
            _diagnosticoCargando.value = false
        }
    }

    fun ejecutarOperacionMatriz(op: Int) {
        val diag = diagnosticoApi ?: return
        viewModelScope.launch {
            _diagnosticoCargando.value = true
            val resultado = when (op) {
                1 -> diag.opListarProductos(_sesionState.value)
                2 -> diag.opRegistrarProducto(_sesionState.value)
                3 -> diag.opListarClientes(_sesionState.value)
                4 -> diag.opRegistrarVenta(_sesionState.value)
                5 -> diag.opVerReportes(_sesionState.value)
                6 -> diag.opConsultarAuthMe(_sesionState.value)
                else -> null
            }
            _resultadoDiagnostico.value = resultado
            _diagnosticoCargando.value = false
        }
    }

    fun limpiarResultadoDiagnostico() {
        _resultadoDiagnostico.value = null
    }

    /**
     * Prueba controlada de depuración para evidenciar que ante un error HTTP 403 (Forbidden):
     * - La sesión NO se cierra.
     * - El usuario ve un aviso claro de falta de privilegios.
     */
    fun probar403Controlado() {
        viewModelScope.launch {
            try {
                authApi.probarAccesoClientes()
                _mensajePermiso.value = "Acceso concedido a clientes (Rol con permiso CLIENTES_LEER)."
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.Forbidden) {
                    _mensajePermiso.value = "Acceso denegado (HTTP 403): Su rol no tiene el permiso CLIENTES_LEER. La sesión se mantiene activa."
                } else {
                    _mensajePermiso.value = "Error en solicitud: ${e.response.status}"
                }
            } catch (e: Exception) {
                _mensajePermiso.value = "Resultado de prueba: ${e.message}"
            }
        }
    }

    fun limpiarMensajePermiso() {
        _mensajePermiso.value = null
    }

    fun rellenarCredencialesPrueba(usuario: String, clave: String) {
        _uiState.update {
            it.copy(
                usuario = usuario,
                clave = clave,
                usuarioError = null,
                claveError = null,
                error = null,
                avisoExpiracion = null
            )
        }
    }
}

