package pe.edu.upeu.pharmamobile.di

import org.koin.dsl.module
import pe.edu.upeu.pharmamobile.data.repository.ProductRepositoryImpl
import pe.edu.upeu.pharmamobile.domain.repository.ProductRepository
import pe.edu.upeu.pharmamobile.domain.usecase.GetProductsUseCase
import pe.edu.upeu.pharmamobile.domain.usecase.RegisterProductUseCase
import pe.edu.upeu.pharmamobile.presentation.producto.ProductoViewModel

val appModule = module {
    single<ProductRepository> { ProductRepositoryImpl() }
    factory { RegisterProductUseCase(get()) }
    factory { GetProductsUseCase(get()) }
    factory { ProductoViewModel(get(), get()) }
}
