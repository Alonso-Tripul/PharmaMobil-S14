package pe.edu.upeu.pharmamobil.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.notificacion.NotificadorAndroid
import pe.edu.upeu.pharmamobil.permiso.GestorPermisosAndroid

/**
 * Lo que necesita la Activity vive en el módulo de aplicación: el gestor de
 * permisos recibe el lanzador de MainActivity a través de PuentePermisos.
 */
val androidAppModule = module {
    single {
        GestorPermisosAndroid(
            androidContext(),
            androidContext().getSharedPreferences("pharma_permisos", Context.MODE_PRIVATE)
        )
    }
    single<GestorPermisos> { get<GestorPermisosAndroid>() }
    single<Notificador> { NotificadorAndroid(androidContext()) }
}
