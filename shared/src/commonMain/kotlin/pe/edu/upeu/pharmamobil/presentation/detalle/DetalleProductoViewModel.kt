package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobil.domain.usecase.resultadoDe
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUi
import pe.edu.upeu.pharmamobil.presentation.producto.aUi

data class DetalleProductoUiState(
    val producto: ProductoUi? = null,
    val cargando: Boolean = true,
    val ocupado: Boolean = false,
    val mensaje: String? = null,
    val eliminado: Boolean = false,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val categoria: String = "",
    val activo: Boolean = true
)

class DetalleProductoViewModel(
    private val repository: ProductoRepository,
    private val compartidor: Compartidor
) : ViewModel() {
    private var productoActual: Producto? = null
    private var carga: Job? = null
    private val _uiState = MutableStateFlow(DetalleProductoUiState())
    val uiState = _uiState.asStateFlow()

    fun cargar(id: Long) {
        carga?.cancel()
        productoActual = null
        _uiState.value = DetalleProductoUiState()
        carga = viewModelScope.launch {
            resultadoDe { repository.buscar(id) }.fold(
                onSuccess = { mostrar(it) },
                onFailure = { error ->
                    _uiState.update { it.copy(cargando = false, mensaje = error.message ?: "No se pudo cargar el producto") }
                }
            )
        }
    }

    private fun mostrar(producto: Producto, mensaje: String? = null) {
        productoActual = producto
        _uiState.value = DetalleProductoUiState(
            producto = producto.aUi(), cargando = false, mensaje = mensaje,
            nombre = producto.nombre, precio = producto.precio.toString(),
            stock = producto.stock.toString(), categoria = producto.categoriaId.toString(),
            activo = producto.estado
        )
    }

    fun onNombreChange(valor: String) { _uiState.update { it.copy(nombre = valor, mensaje = null) } }
    fun onPrecioChange(valor: String) { _uiState.update { it.copy(precio = valor, mensaje = null) } }
    fun onStockChange(valor: String) { _uiState.update { it.copy(stock = valor, mensaje = null) } }
    fun onCategoriaChange(valor: String) { _uiState.update { it.copy(categoria = valor, mensaje = null) } }
    fun onActivoChange(valor: Boolean) { _uiState.update { it.copy(activo = valor, mensaje = null) } }

    fun compartir() {
        if (_uiState.value.ocupado || _uiState.value.cargando) return
        val producto = productoActual ?: return
        runCatching { compartidor.compartir(producto.comoTextoParaCompartir()) }
            .onFailure { error ->
                _uiState.update { it.copy(mensaje = error.message ?: "No se pudo abrir el selector") }
            }
    }

    fun guardar() {
        if (_uiState.value.ocupado) return
        val original = productoActual ?: return
        val formulario = _uiState.value
        val precio = formulario.precio.toDoubleOrNull()
        val stock = formulario.stock.toIntOrNull()
        val categoria = formulario.categoria.toLongOrNull()
        if (formulario.nombre.trim().length !in 3..150 ||
            precio == null || !precio.isFinite() || precio < 0.01 ||
            stock == null || stock < 0 || categoria == null || categoria <= 0
        ) {
            _uiState.update { it.copy(mensaje = "Revisa nombre (3–150 caracteres), precio (mínimo 0.01), stock y categoría válida.") }
            return
        }
        val actualizado = original.copy(
            nombre = formulario.nombre.trim(), precio = precio, stock = stock,
            categoriaId = categoria, estado = formulario.activo
        )
        _uiState.update { it.copy(ocupado = true, mensaje = null) }
        viewModelScope.launch {
            resultadoDe { repository.actualizar(actualizado) }.fold(
                onSuccess = { mostrar(it, "Cambios guardados") },
                onFailure = { error ->
                    _uiState.update { it.copy(ocupado = false, mensaje = error.message ?: "No se pudo actualizar") }
                }
            )
        }
    }

    fun eliminar() {
        if (_uiState.value.ocupado) return
        val producto = productoActual ?: return
        _uiState.update { it.copy(ocupado = true, mensaje = null) }
        viewModelScope.launch {
            resultadoDe { repository.eliminar(producto.id) }.fold(
                onSuccess = {
                    productoActual = null
                    _uiState.update { it.copy(ocupado = false, eliminado = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(ocupado = false, mensaje = error.message ?: "No se pudo eliminar") }
                }
            )
        }
    }
}
