package pe.edu.upeu.pharmamobil.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.data.local.AlmacenProductos
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.SesionLocal
import pe.edu.upeu.pharmamobil.data.local.ProveedorUsuario
import pe.edu.upeu.pharmamobil.data.network.GestorSesion
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.data.repository.EnvioRepositorioRest
import pe.edu.upeu.pharmamobil.domain.platform.Reloj
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRemotoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase
import pe.edu.upeu.pharmamobil.platform.RelojSistema


val dataModule = module {
    single { PharmaDb(get<CreadorDeDriver>().crear()) }
    single<ProveedorUsuario> { get<GestorSesion>() }
    single<AlmacenProductos> { ProductoLocal(get(), get()) }
    single { SesionLocal(get()) }
    single<pe.edu.upeu.pharmamobil.domain.repository.AuthRepository> {
        pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest(get(), get(), get(), get(), get())
    }
    single { ProductoRepositorioRest(get(), get()) }
    single<ProductoRepository> { ProductoRepositorioOffline(get(), get<ProductoRepositorioRest>()) }
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }
    // Sesión 13: la cola de envío se expone al dominio por sus contratos.
    single<ProductoLocalRepository> { get<AlmacenProductos>() }
    single<ProductoRemotoRepository> { EnvioRepositorioRest(get()) }
    single<Reloj> { RelojSistema }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { RegistrarClienteUseCase(get()) }
    factory { ListarClientesUseCase(get()) }
    // single: su candado debe ser el mismo para todas las pantallas.
    single { SincronizarUseCase(get(), get(), get()) }
}

val presentationModule = module {
    viewModel { DetalleProductoViewModel(get(), get()) }
    viewModel { ProductoViewModel(get(), get()) }
    viewModel { ClienteViewModel(get(), get()) }
}


expect val platformModule: Module

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule
        )
    }
}
