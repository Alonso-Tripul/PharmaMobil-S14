package pe.edu.upeu.pharmamobil.di

import org.koin.core.module.Module
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import pe.edu.upeu.pharmamobil.data.network.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.network.configurarPharmaSoft
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.android.ext.koin.androidContext
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverAndroid

import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.platform.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobil.data.network.GestorSesion
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.data.firebase.TelemetriaFirebase
import pe.edu.upeu.pharmamobil.platform.MonitorRedAndroid

actual val platformModule: Module = module {
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    single { ConfiguracionApi("http://10.0.2.2:8080/api/v1/") }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single { GestorSesion(get()) }
    single { run {
        val config: ConfiguracionApi = get(); val almacen: AlmacenSeguro = get(); val gestor: GestorSesion = get()
        val debug = (androidContext().applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        HttpClient(Android) { configurarPharmaSoft(config, almacen, gestor, debug) }
    } } onClose { it?.close() }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<MonitorRed> { MonitorRedAndroid(androidContext()) }
    single<Telemetria> { TelemetriaFirebase() }
}
