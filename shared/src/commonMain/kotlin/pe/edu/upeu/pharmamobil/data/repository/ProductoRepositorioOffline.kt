package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.AlmacenProductos
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * La interfaz siempre lee SQLDelight. La red solo modifica esta base.
 * El mutex evita dos sincronizaciones simultáneas y dos subidas del mismo ID.
 */
class ProductoRepositorioOffline(
    private val local: AlmacenProductos,
    private val remoto: ProductoRepository
) : ProductoRepository {
    private val candado = Mutex()

    override fun observar(): Flow<List<Producto>> = local.observar()
    override suspend fun listar(): List<Producto> = local.listar()
    override suspend fun buscar(id: Long): Producto =
        local.buscar(id) ?: throw NoSuchElementException("Producto $id no encontrado")

    override suspend fun registrar(producto: Producto): Producto {
        val guardado = candado.withLock { local.guardarPendiente(producto) }
        // El registro local ya se completó; la red no impide el alta offline.
        try {
            sincronizar()
        } catch (cancelado: CancellationException) {
            throw cancelado
        } catch (_: Exception) {
            // Permanece pendiente para el siguiente intento explícito.
        }
        return guardado
    }

    override suspend fun sincronizar() {
        candado.withLock {
            local.pendientes().forEach { pendiente ->
                val creado = remoto.registrar(pendiente)
                local.confirmar(pendiente.id, creado)
            }
            local.reemplazarSincronizados(remoto.listar())
        }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        require(producto.id >= 0) { "Sincroniza el producto pendiente antes de editarlo" }
        val actualizado = remoto.actualizar(producto)
        local.guardarSincronizado(actualizado)
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        require(id >= 0) { "Sincroniza el producto pendiente antes de eliminarlo" }
        remoto.eliminar(id)
        // El backend usa baja lógica; conserva la fila como inactiva.
        local.buscar(id)?.let { local.guardarSincronizado(it.copy(estado = false)) }
    }
}
