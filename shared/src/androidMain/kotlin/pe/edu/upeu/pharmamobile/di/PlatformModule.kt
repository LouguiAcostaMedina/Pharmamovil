package pe.edu.upeu.pharmamobile.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriverAndroid
import pe.edu.upeu.pharmamobile.data.telemetria.TelemetriaFirebaseAndroid
import pe.edu.upeu.pharmamobile.domain.permission.GestorPermisos
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.domain.platform.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile.domain.telemetria.Telemetria
import pe.edu.upeu.pharmamobile.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobile.platform.GestorPermisosAndroid
import pe.edu.upeu.pharmamobile.platform.NotificadorSincronizacionAndroid

actual val platformModule: Module = module {
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    single<GestorPermisosAndroid> { GestorPermisosAndroid(androidContext()) }
    single<GestorPermisos> { get<GestorPermisosAndroid>() }
    single<NotificadorSincronizacion> { NotificadorSincronizacionAndroid(androidContext()) }
    single<Telemetria> { TelemetriaFirebaseAndroid(androidContext()) }
}
