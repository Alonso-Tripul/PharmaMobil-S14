package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.data.local.AlmacenProductos
import pe.edu.upeu.pharmamobil.domain.model.EnvioPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Doble de la base local con la misma semántica que ProductoLocal: IDs
 * temporales negativos, cola con intentos y descarga que respeta lo pendiente.
 * La prueba con SQLite real está en androidHostTest/ProductoLocalSqlTest.
 */
class AlmacenEnMemoria : AlmacenProductos {
    private val sincronizados = MutableStateFlow<List<Producto>>(emptyList())
    private val cola = MutableStateFlow<List<EnvioPendiente>>(emptyList())
    private val ultima = MutableStateFlow<Long?>(null)
    private var siguienteClave = 1

    /** Para las pruebas: la fila de la cola con ese ID, si sigue pendiente. */
    fun porId(id: Long): EnvioPendiente? = cola.value.firstOrNull { it.id == id }

    override fun observar(): Flow<List<Producto>> =
        kotlinx.coroutines.flow.combine(sincronizados, cola) { s, c -> s + c.map { it.producto } }

    override fun listar(): List<Producto> = sincronizados.value + cola.value.map { it.producto }
    override fun buscar(id: Long): Producto? = listar().firstOrNull { it.id == id }

    override fun guardarPendiente(producto: Producto): Producto {
        val id = minOf(listar().minOfOrNull { it.id } ?: 0L, 0L) - 1
        val guardado = producto.copy(id = id)
        cola.value = cola.value + EnvioPendiente(guardado, claveOperacion = "clave-${siguienteClave++}")
        return guardado
    }

    override fun guardarSincronizado(producto: Producto) {
        sincronizados.value = sincronizados.value.filterNot { it.id == producto.id } + producto
    }

    override fun borrar(id: Long) {
        cola.value = cola.value.filterNot { it.id == id }
        sincronizados.value = sincronizados.value.filterNot { it.id == id }
    }

    override fun pendientes(maxIntentos: Int): List<EnvioPendiente> =
        cola.value.filter { it.intentos < maxIntentos }.sortedByDescending { it.id }

    override fun confirmar(idTemporal: Long, producto: Producto) {
        cola.value = cola.value.filterNot { it.id == idTemporal }
        guardarSincronizado(producto)
    }

    override fun sumarIntento(id: Long, motivo: String) = cambiar(id) {
        it.copy(intentos = it.intentos + 1, ultimoError = motivo)
    }

    override fun marcarFallida(id: Long, motivo: String, maxIntentos: Int) = cambiar(id) {
        it.copy(intentos = maxIntentos, ultimoError = motivo)
    }

    override fun conciliar(productos: List<Producto>) { sincronizados.value = productos }

    override fun contarPendientes(maxIntentos: Int): Int = pendientes(maxIntentos).size

    override fun observarPendientes(maxIntentos: Int): Flow<Int> =
        cola.map { filas -> filas.count { it.intentos < maxIntentos } }

    override fun observarFallidas(maxIntentos: Int): Flow<List<EnvioPendiente>> =
        cola.map { filas -> filas.filter { it.intentos >= maxIntentos } }

    override fun reintentarFallidas(maxIntentos: Int) {
        cola.value = cola.value.map {
            if (it.intentos >= maxIntentos) it.copy(intentos = 0, ultimoError = null) else it
        }
    }

    override fun descartar(id: Long) { if (id < 0) borrar(id) }

    override fun observarUltimaSync(): Flow<Long?> = ultima
    override fun registrarSync(epochMs: Long) { ultima.value = epochMs }

    private fun cambiar(id: Long, cambio: (EnvioPendiente) -> EnvioPendiente) {
        cola.value = cola.value.map { if (it.id == id) cambio(it) else it }
    }
}
