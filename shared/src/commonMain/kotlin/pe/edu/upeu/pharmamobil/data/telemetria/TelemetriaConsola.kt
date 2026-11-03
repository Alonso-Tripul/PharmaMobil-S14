package pe.edu.upeu.pharmamobil.data.telemetria

import pe.edu.upeu.pharmamobil.domain.port.Telemetria

/** Para iOS mientras no se enlaza Firebase, y para las pruebas. */
class TelemetriaConsola : Telemetria {
    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        println("[telemetria] error ${e::class.simpleName}: ${e.message} $contexto")
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        println("[telemetria] evento $nombre $datos")
    }
}
