package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.data.local.AlmacenProductos
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * La interfaz siempre lee SQLDelight. La red solo modifica esta base.
 *
 * Desde la sesión 13 el alta no intenta enviar: solo encola. El envío lo hace
 * SincronizarUseCase, que conoce los intentos, la espera y la diferencia entre
 * un fallo transitorio y uno permanente. La pantalla lo dispara sola al ver
 * crecer la cola o al volver la red.
 */
class ProductoRepositorioOffline(
    private val local: AlmacenProductos,
    private val remoto: ProductoRepository
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()
    override suspend fun listar(): List<Producto> = local.listar()
    override suspend fun buscar(id: Long): Producto =
        local.buscar(id) ?: throw NoSuchElementException("Producto $id no encontrado")

    override suspend fun registrar(producto: Producto): Producto = local.guardarPendiente(producto)

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
