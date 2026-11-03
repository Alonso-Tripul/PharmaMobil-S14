package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.EnvioPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Lo que la sincronización necesita de la base local. El dominio solo conoce
 * este contrato; SQLDelight vive detrás, en data/local.
 */
interface ProductoLocalRepository {
    /** Filas pendientes que todavía no agotaron sus intentos, de la más antigua a la más nueva. */
    fun pendientes(maxIntentos: Int): List<EnvioPendiente>

    /** Cambia el ID temporal por el que asignó el servidor y saca la fila de la cola. */
    fun confirmar(idTemporal: Long, producto: Producto)

    /** Fallo transitorio: la fila sigue en la cola con un intento más. */
    fun sumarIntento(id: Long, motivo: String)

    /** Fallo permanente: la fila agota sus intentos y queda visible como fallida. */
    fun marcarFallida(id: Long, motivo: String, maxIntentos: Int)

    /** Reemplaza lo sincronizado por lo que tiene el servidor sin tocar lo pendiente. */
    fun conciliar(productos: List<Producto>)

    fun contarPendientes(maxIntentos: Int): Int
    fun observarPendientes(maxIntentos: Int): Flow<Int>
    fun observarFallidas(maxIntentos: Int): Flow<List<EnvioPendiente>>

    /** Acción del usuario: devuelve las fallidas a la cola con cero intentos. */
    fun reintentarFallidas(maxIntentos: Int)

    /** Acción del usuario: descarta una fila que no se va a enviar. */
    fun descartar(id: Long)

    /** Epoch en milisegundos de la última descarga completa, o null si nunca hubo. */
    fun observarUltimaSync(): Flow<Long?>
    fun registrarSync(epochMs: Long)
}
