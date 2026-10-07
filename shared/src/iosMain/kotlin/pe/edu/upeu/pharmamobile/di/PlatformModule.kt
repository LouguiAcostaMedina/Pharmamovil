package pe.edu.upeu.pharmamobile.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile.platform.CompartidorIos

actual val platformModule: Module = module {
    single<Compartidor> { CompartidorIos() }
}
