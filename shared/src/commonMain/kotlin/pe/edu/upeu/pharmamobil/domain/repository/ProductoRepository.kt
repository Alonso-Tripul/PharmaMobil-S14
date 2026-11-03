package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ProductoRepository {
    /** La implementación offline observa la base local; valor por defecto para dobles existentes. */
    fun observar(): Flow<List<Producto>> = flow { emit(listar()) }

    /** Envía pendientes y después actualiza la base local. */
    suspend fun sincronizar() {}

    /** Incorpora el producto al inventario y devuelve el producto ya identificado. */
    suspend fun registrar(producto: Producto): Producto

    /** Entrega el inventario completo en el orden en que fue registrado. */
    suspend fun listar(): List<Producto>

    suspend fun buscar(id: Long): Producto = listar().first { it.id == id }
    suspend fun actualizar(producto: Producto): Producto =
        throw UnsupportedOperationException("Este repositorio no admite actualización")
    suspend fun eliminar(id: Long): Unit =
        throw UnsupportedOperationException("Este repositorio no admite eliminación")
}
