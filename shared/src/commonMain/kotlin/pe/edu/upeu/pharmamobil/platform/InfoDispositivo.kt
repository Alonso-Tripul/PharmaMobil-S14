package pe.edu.upeu.pharmamobil.platform

/** Nombre y versión del sistema que ejecuta la aplicación. */
expect class InfoDispositivo() {
    val sistema: String
    val version: String
}
