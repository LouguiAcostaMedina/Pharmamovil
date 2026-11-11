package pe.edu.upeu.pharmamobile.platform

import pe.edu.upeu.pharmamobile.domain.permission.EstadoPermiso
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.permission.Permiso

class GestorPermisosIos : GestorPermisos {
    override suspend fun estado(permiso: Permiso): EstadoPermiso = EstadoPermiso.Concedido
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso = EstadoPermiso.Concedido
    override fun abrirAjustes() {}
}

