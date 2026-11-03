package pe.edu.upeu.pharmamobil.domain.port

/**
 * Lo que la app necesita de un servicio de telemetría. El proveedor concreto
 * (hoy Firebase) vive en la capa de datos; cambiarlo toca una sola clase.
 *
 * Nunca registres datos personales ni el token: esto termina en un servicio
 * de terceros. Solo contexto técnico: pantalla, operación, código, conteos.
 */
interface Telemetria {
    fun registrarError(e: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}
