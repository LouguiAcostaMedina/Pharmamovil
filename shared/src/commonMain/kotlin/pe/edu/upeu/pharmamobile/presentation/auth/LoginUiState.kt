package pe.edu.upeu.pharmamobile.presentation.auth

import pe.edu.upeu.pharmamobile.domain.model.Sesion

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val claveVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val usuarioError: String? = null,
    val claveError: String? = null,
    val avisoExpiracion: String? = null,
    val sesion: Sesion? = null
)
