package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.db.Producto as FilaProducto
import pe.edu.upeu.pharmamobil.domain.model.EnvioPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoLocal(base: PharmaDb, private val usuarios: ProveedorUsuario) : AlmacenProductos {
    private val consultas = base.productoQueries
    private fun owner() = usuarios.usuarioActual()

    override fun observar(): Flow<List<Producto>> =
        consultas.listarTodos(owner()).asFlow().mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.aDominio() } }

    override fun listar(): List<Producto> = consultas.listarTodos(owner()).executeAsList().map { it.aDominio() }

    override fun buscar(id: Long): Producto? = consultas.buscar(owner(), id).executeAsOneOrNull()?.aDominio()

    override fun guardarPendiente(producto: Producto): Producto {
        var guardado: Producto? = null
        consultas.transaction {
            val menor = consultas.siguienteIdLocal(owner()).executeAsOne()
            val fila = producto.copy(id = minOf(menor, 0L) - 1L)
            consultas.insertarPendiente(
                owner(), fila.id, fila.nombre, fila.precio, fila.stock.toLong(),
                fila.categoriaId, if (fila.estado) 1L else 0L, nuevaClaveOperacion()
            )
            guardado = fila
        }
        return requireNotNull(guardado)
    }

    /** Cambia el ID temporal por el que asignó el servidor antes de descargar. */
    override fun confirmar(idTemporal: Long, producto: Producto) {
        consultas.transaction {
            consultas.borrar(owner(), idTemporal)
            insertar(producto, 0L)
        }
    }

    override fun guardarSincronizado(producto: Producto) {
        consultas.transaction { insertar(producto, 0L) }
    }

    override fun borrar(id: Long) {
        consultas.borrar(owner(), id)
    }

    // ---- Cola de envío (sesión 13) ----

    override fun pendientes(maxIntentos: Int): List<EnvioPendiente> =
        consultas.pendientesPorEnviar(owner(), maxIntentos.toLong()).executeAsList().map { it.aEnvio() }

    override fun sumarIntento(id: Long, motivo: String) {
        consultas.sumarIntento(motivo, owner(), id)
    }

    override fun marcarFallida(id: Long, motivo: String, maxIntentos: Int) {
        consultas.marcarFallida(maxIntentos.toLong(), motivo, owner(), id)
    }

    /** Mantiene intactos los productos aún no enviados. */
    override fun conciliar(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados(owner())
            productos.forEach { insertar(it, 0L) }
        }
    }

    override fun contarPendientes(maxIntentos: Int): Int =
        consultas.contarPendientes(owner(), maxIntentos.toLong()).executeAsOne().toInt()

    override fun observarPendientes(maxIntentos: Int): Flow<Int> =
        consultas.contarPendientes(owner(), maxIntentos.toLong()).asFlow()
            .mapToOne(Dispatchers.Default).map { it.toInt() }

    override fun observarFallidas(maxIntentos: Int): Flow<List<EnvioPendiente>> =
        consultas.listarFallidas(owner(), maxIntentos.toLong()).asFlow()
            .mapToList(Dispatchers.Default).map { filas -> filas.map { it.aEnvio() } }

    override fun reintentarFallidas(maxIntentos: Int) {
        consultas.reintentarFallidas(owner(), maxIntentos.toLong())
    }

    override fun descartar(id: Long) {
        // Solo se descarta lo que nunca llegó al servidor (ID temporal negativo).
        if (id < 0) consultas.borrar(owner(), id)
    }

    override fun observarUltimaSync(): Flow<Long?> =
        consultas.leerUltimaSync(owner()).asFlow().mapToOneOrNull(Dispatchers.Default)

    override fun registrarSync(epochMs: Long) {
        consultas.guardarUltimaSync(owner(), epochMs)
    }

    private fun insertar(producto: Producto, pendiente: Long) {
        consultas.insertar(
            owner(), producto.id, producto.nombre, producto.precio, producto.stock.toLong(),
            producto.categoriaId, if (producto.estado) 1L else 0L, pendiente
        )
    }

    private fun FilaProducto.aDominio() = Producto(
        id = id, nombre = nombre, precio = precio, stock = stock.toInt(),
        categoriaId = categoriaId, estado = estado == 1L
    )

    private fun FilaProducto.aEnvio() = EnvioPendiente(
        producto = aDominio(), intentos = intentos.toInt(),
        ultimoError = ultimoError, claveOperacion = claveOperacion
    )

    /** Formato UUID v4; no hace falta que sea criptográfica, solo única por alta. */
    private fun nuevaClaveOperacion(): String {
        val hex = "0123456789abcdef"
        val c = CharArray(32) { hex[Random.nextInt(16)] }
        c[12] = '4'
        c[16] = hex[8 + Random.nextInt(4)]
        val s = c.concatToString()
        return "${s.substring(0, 8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-${s.substring(16, 20)}-${s.substring(20)}"
    }
}
