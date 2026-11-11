package pe.edu.upeu.pharmamobile.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguroDesktop
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriverDesktop
import pe.edu.upeu.pharmamobile.data.telemetria.TelemetriaDesktop
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.domain.platform.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria
import pe.edu.upeu.pharmamobile.platform.CompartidorDesktop
import pe.edu.upeu.pharmamobile.platform.GestorPermisosDesktop
import pe.edu.upeu.pharmamobile.platform.NotificadorSincronizacionDesktop

actual val platformModule: Module = module {
    single<Compartidor> { CompartidorDesktop() }
    single<AlmacenSeguro> { AlmacenSeguroDesktop() }
    single<CreadorDeDriver> { CreadorDeDriverDesktop() }
    single<GestorPermisos> { GestorPermisosDesktop() }
    single<NotificadorSincronizacion> { NotificadorSincronizacionDesktop() }
    single<Telemetria> { TelemetriaDesktop() }
}
