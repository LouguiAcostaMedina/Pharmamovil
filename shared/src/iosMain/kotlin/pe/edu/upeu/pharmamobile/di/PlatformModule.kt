package pe.edu.upeu.pharmamobile.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguroIos
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriverIos
import pe.edu.upeu.pharmamobile.data.telemetria.TelemetriaIos
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.domain.platform.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria
import pe.edu.upeu.pharmamobile.platform.CompartidorIos
import pe.edu.upeu.pharmamobile.platform.GestorPermisosIos
import pe.edu.upeu.pharmamobile.platform.NotificadorSincronizacionIos

actual val platformModule: Module = module {
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    single<GestorPermisos> { GestorPermisosIos() }
    single<NotificadorSincronizacion> { NotificadorSincronizacionIos() }
    single<Telemetria> { TelemetriaIos() }
}
