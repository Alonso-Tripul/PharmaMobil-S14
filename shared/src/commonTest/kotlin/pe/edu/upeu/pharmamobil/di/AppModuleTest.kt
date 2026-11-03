package pe.edu.upeu.pharmamobil.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.data.repository.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.GestorPermisosFalso
import pe.edu.upeu.pharmamobil.data.repository.NotificadorFalso
import pe.edu.upeu.pharmamobil.data.repository.RedFalsa
import pe.edu.upeu.pharmamobil.data.repository.RemotoFalso
import pe.edu.upeu.pharmamobil.data.repository.TelemetriaFalsa
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed
import pe.edu.upeu.pharmamobil.domain.platform.Reloj
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRemotoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase
import pe.edu.upeu.pharmamobil.presentation.permiso.PermisoViewModel
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.SincronizacionViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Comprueba que el grafo se ensambla sin arrancar la aplicacion: si una
 * definicion falta o esta declarada con el tipo equivocado, falla aqui.
 *
 * Se cargan domainModule y presentationModule con dobles de plataforma:
 * no se necesita Context ni una conexión real. Construir un
 * ViewModel es justo lo que se rompe al cambiar un constructor, y antes era
 * lo unico que el grafo no cubria. Como los ViewModel arrancan una carga en
 * su init, hace falta un Dispatchers.Main de prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun detenerKoin() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun grafoCompleto(): Koin = startKoin {
        modules(domainModule, presentationModule, module {
            single<ProductoRepository> { ProductoRepositorioEnMemoria() }
            single<ClienteRepository> { ClienteRepositorioEnMemoria() }
            single<Compartidor> { object : Compartidor { override fun compartir(texto: String) {} } }
            // Sesión 13: dobles de lo que cada plataforma registra en platformModule.
            single<ProductoLocalRepository> { AlmacenEnMemoria() }
            single<ProductoRemotoRepository> { RemotoFalso() }
            single<Reloj> { Reloj { 0L } }
            single<MonitorRed> { RedFalsa(inicial = false) }
            single<Notificador> { NotificadorFalso() }
            single<Telemetria> { TelemetriaFalsa() }
            single<GestorPermisos> { GestorPermisosFalso() }
        })
    }.koin

    @Test
    fun resuelveLosRepositoriosPorSuInterfazDeDominio() {

        val koin = grafoCompleto()

        assertIs<ProductoRepositorioEnMemoria>(koin.get<ProductoRepository>())
        assertIs<ClienteRepositorioEnMemoria>(koin.get<ClienteRepository>())
    }

    @Test
    fun losRepositoriosSonUnicosEnTodaLaAplicacion() {

        val koin = grafoCompleto()

        assertSame(
            koin.get<ProductoRepository>(),
            koin.get<ProductoRepository>()
        )
        assertSame(
            koin.get<ClienteRepository>(),
            koin.get<ClienteRepository>()
        )
    }

    @Test
    fun resuelveLosCuatroCasosDeUsoConSusRepositorios() {

        val koin = grafoCompleto()

        koin.get<RegistrarProductoUseCase>()
        koin.get<ListarProductosUseCase>()
        koin.get<RegistrarClienteUseCase>()
        koin.get<ListarClientesUseCase>()
        assertSame(koin.get<SincronizarUseCase>(), koin.get<SincronizarUseCase>())
    }

    @Test
    fun resuelveLosViewModelConSusCasosDeUso() {

        val koin = grafoCompleto()

        koin.get<DetalleProductoViewModel>()
        koin.get<ProductoViewModel>()
        koin.get<ClienteViewModel>()
        koin.get<SincronizacionViewModel>()
        koin.get<PermisoViewModel>()
    }
}
