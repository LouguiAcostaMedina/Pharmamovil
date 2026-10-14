package pe.edu.upeu.pharmamobile.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile.data.local.AlmacenSeguroDesktop
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.platform.CompartidorDesktop

actual val platformModule: Module = module {
    single<Compartidor> { CompartidorDesktop() }
    single<AlmacenSeguro> { AlmacenSeguroDesktop() }
}
