package pe.edu.upeu.pharmamobil.data.local

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository

/** Permite probar el repositorio y la sincronización sin un driver de plataforma. */
interface AlmacenProductos : ProductoLocalRepository {
    fun observar(): Flow<List<Producto>>
    fun listar(): List<Producto>
    fun buscar(id: Long): Producto?
    fun guardarPendiente(producto: Producto): Producto
    fun guardarSincronizado(producto: Producto)
    fun borrar(id: Long)
}
