package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.db.Producto as FilaProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoLocal(base: PharmaDb, private val usuarios: ProveedorUsuario) : AlmacenProductos {
    private val consultas = base.productoQueries
    private fun owner() = usuarios.usuarioActual()

    override fun observar(): Flow<List<Producto>> =
        consultas.listarTodos(owner()).asFlow().mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.aDominio() } }

    override fun listar(): List<Producto> = consultas.listarTodos(owner()).executeAsList().map { it.aDominio() }

    override fun buscar(id: Long): Producto? = consultas.buscar(owner(), id).executeAsOneOrNull()?.aDominio()

    override fun pendientes(): List<Producto> = consultas.listarPendientes(owner()).executeAsList().map { it.aDominio() }

    override fun guardarPendiente(producto: Producto): Producto {
        var guardado: Producto? = null
        consultas.transaction {
            val menor = consultas.siguienteIdLocal(owner()).executeAsOne()
            guardado = producto.copy(id = minOf(menor, 0L) - 1L)
            insertar(guardado!!, 1L)
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

    /** Mantiene intactos los productos aún no enviados. */
    override fun reemplazarSincronizados(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados(owner())
            productos.forEach { insertar(it, 0L) }
        }
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
}
