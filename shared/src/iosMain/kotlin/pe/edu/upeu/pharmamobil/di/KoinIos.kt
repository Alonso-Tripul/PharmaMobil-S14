package pe.edu.upeu.pharmamobil.di

/**
 * Punto de arranque de Koin para iOS. Se invoca desde iOSApp.swift.
 *
 * Kotlin/Native antepone "do" a las funciones cuyo nombre empieza con "init",
 * asi que del lado Swift esta funcion se llama KoinIosKt.doInitKoinIos().
 */
fun initKoinIos() {
    initKoin()
}
