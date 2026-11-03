package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.Flow

class ListarProductosUseCase(
    private val productoRepository: ProductoRepository
) {
    fun observar(): Flow<List<Producto>> = productoRepository.observar()

    suspend fun sincronizar() = productoRepository.sincronizar()

    suspend operator fun invoke(): Result<List<Producto>> = resultadoDe {
        productoRepository.listar()
    }
}
