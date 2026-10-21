package pe.edu.upeu.pharmamobile.di

import io.ktor.client.*
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile.data.network.AuthApi
import pe.edu.upeu.pharmamobile.data.network.ProductoApi
import pe.edu.upeu.pharmamobile.data.network.DiagnosticoS10Api
import pe.edu.upeu.pharmamobile.data.network.crearClienteDiagnostico
import pe.edu.upeu.pharmamobile.data.network.crearHttpClient
import pe.edu.upeu.pharmamobile.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobile.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobile.db.PharmaDb
import pe.edu.upeu.pharmamobile.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile.domain.usecase.*
import pe.edu.upeu.pharmamobile.presentation.auth.AuthViewModel
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoViewModel

val appModule = module {
    // Cliente HTTP unificado con plugin Auth, logging y timeouts
    single<HttpClient> { crearHttpClient(get()) }

    // APIs
    single { ProductoApi(get()) }
    single { AuthApi(get()) }
    single { DiagnosticoS10Api(get(), crearClienteDiagnostico(), get()) }

    // Base de datos local SQLDelight y capa local (Paso 2 y Paso 3)
    single { PharmaDb(get<CreadorDeDriver>().crear()) }
    single { ProductoLocal(get()) }

    // Repositorios
    single<ProductoRepository> { ProductoRepositorioOffline(get(), get()) }
    single<AuthRepository> { AuthRepositorioRest(get(), get(), get()) }

    // Casos de uso de Productos (CRUD S08)
    factory { ListarProductosUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }

    // Casos de uso de Sesión y Autenticación (S10)
    factory { IniciarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObtenerSesionGuardadaUseCase(get()) }

    // ViewModels
    viewModelOf(::ProductoViewModel)
    viewModelOf(::AuthViewModel)
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModule, platformModule)
    }
}
