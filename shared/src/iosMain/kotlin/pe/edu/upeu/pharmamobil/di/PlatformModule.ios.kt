package pe.edu.upeu.pharmamobil.di

import org.koin.core.module.Module
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import pe.edu.upeu.pharmamobil.data.network.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.network.configurarPharmaSoft
import org.koin.dsl.module
import org.koin.dsl.onClose
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverIos

import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.platform.AlmacenSeguroIos
import pe.edu.upeu.pharmamobil.data.network.GestorSesion
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed
import pe.edu.upeu.pharmamobil.platform.MonitorRedIos

actual val platformModule: Module = module {
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    single { ConfiguracionApi("http://localhost:8080/api/v1/") }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single { GestorSesion(get()) }
    single { run {
        val config: ConfiguracionApi = get(); val almacen: AlmacenSeguro = get(); val gestor: GestorSesion = get()
        val debug = kotlin.native.Platform.isDebugBinary
        HttpClient(Darwin) { configurarPharmaSoft(config, almacen, gestor, debug) }
    } } onClose { it?.close() }
    single<Compartidor> { CompartidorIos() }
    single<MonitorRed> { MonitorRedIos() }
}
