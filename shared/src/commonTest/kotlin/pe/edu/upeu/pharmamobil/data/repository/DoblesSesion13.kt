package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import pe.edu.upeu.pharmamobil.domain.model.EnvioPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRemotoRepository

/** Servidor programable: cada fila recibe lo que devuelva [responder]. */
class RemotoFalso(
    var responder: (EnvioPendiente) -> ResultadoEnvio = { ResultadoEnvio.Exito(it.producto.copy(id = 100 + -it.id)) }
) : ProductoRemotoRepository {
    val enviadas = mutableListOf<EnvioPendiente>()
    var inventario: List<Producto> = emptyList()
    var descargas = 0

    override suspend fun enviar(fila: EnvioPendiente): ResultadoEnvio {
        enviadas += fila
        return responder(fila).also { r ->
            if (r is ResultadoEnvio.Exito) inventario = inventario + r.producto
        }
    }

    override suspend fun listar(): List<Producto> { descargas++; return inventario }
}

class RedFalsa(inicial: Boolean = true) : MonitorRed {
    override val conectado = MutableStateFlow(inicial)
}

class NotificadorFalso : Notificador {
    val avisos = mutableListOf<String>()
    override fun avisar(titulo: String, texto: String) { avisos += titulo }
}

class TelemetriaFalsa : Telemetria {
    val errores = mutableListOf<Throwable>()
    val eventos = mutableListOf<Pair<String, Map<String, String>>>()
    override fun registrarError(e: Throwable, contexto: Map<String, String>) { errores += e }
    override fun registrarEvento(nombre: String, datos: Map<String, String>) { eventos += nombre to datos }
}

/** La ventaja de declarar el gestor como interfaz: se prueba sin Activity. */
class GestorPermisosFalso(
    var actual: EstadoPermiso = EstadoPermiso.Denegado,
    var respuestaAlPedir: EstadoPermiso = EstadoPermiso.Concedido,
) : GestorPermisos {
    var solicitudes = 0
    var ajustesAbiertos = 0
    override suspend fun estado(permiso: Permiso) = actual
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        solicitudes++; actual = respuestaAlPedir; return actual
    }
    override fun abrirAjustes() { ajustesAbiertos++ }
}
