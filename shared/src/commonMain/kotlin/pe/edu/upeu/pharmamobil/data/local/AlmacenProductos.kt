package pe.edu.upeu.pharmamobil.data.local

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

/** Permite probar la sincronización sin un driver de plataforma. */
interface AlmacenProductos {
    fun observar(): Flow<List<Producto>>
    fun listar(): List<Producto>
    fun buscar(id: Long): Producto?
    fun pendientes(): List<Producto>
    fun guardarPendiente(producto: Producto): Producto
    fun confirmar(idTemporal: Long, producto: Producto)
    fun guardarSincronizado(producto: Producto)
    fun borrar(id: Long)
    fun reemplazarSincronizados(productos: List<Producto>)
}
