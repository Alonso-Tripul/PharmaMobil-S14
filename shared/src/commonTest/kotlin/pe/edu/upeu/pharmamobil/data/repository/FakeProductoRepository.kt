package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble del inventario para las pruebas: sin delay y capaz de fallar a
 * voluntad. Sin el, probar el camino de error del caso de uso o del ViewModel
 * era imposible, porque el repositorio en memoria nunca falla.
 */
class FakeProductoRepository(
    private val productos: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null

    private var siguienteId = 1L
    private val cambios = MutableStateFlow(productos.toList())

    override fun observar(): Flow<List<Producto>> = cambios

    override suspend fun sincronizar() { listar() }

    override suspend fun registrar(producto: Producto): Producto {

        fallaAlRegistrar?.let { throw it }

        val guardado = producto.copy(id = siguienteId++)
        productos.add(guardado)
        cambios.value = productos.toList()
        return guardado
    }

    override suspend fun listar(): List<Producto> {

        fallaAlListar?.let { throw it }

        return productos.toList()
    }
}
