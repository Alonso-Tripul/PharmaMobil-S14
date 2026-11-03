package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase


class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val listarProductos: ListarProductosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            listarProductos.observar().collect { productos ->
                _uiState.update {
                    it.copy(fase = if (productos.isEmpty()) ProductoUiState.Fase.SinProductos
                        else ProductoUiState.Fase.ConProductos(productos.map { producto -> producto.aUi() }))
                }
            }
        }
        cargarProductos()
    }

    /** Conserva el nombre para la recarga; la pantalla observa siempre la base local. */
    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true, avisoRed = null) }
            try {
                listarProductos.sincronizar()
                _uiState.update { it.copy(sincronizando = false) }
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(sincronizando = false, avisoRed = "Sin conexión o error del servidor: mostrando datos guardados")
                }
            }
        }
    }

    fun onNombreChange(nombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(nombre = nombre, nombreError = null),
                mensajeExito = null
            )
        }
    }

    fun onPrecioChange(precio: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(precio = precio, precioError = null),
                mensajeExito = null
            )
        }
    }

    fun onStockChange(stock: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(stock = stock, stockError = null),
                mensajeExito = null
            )
        }
    }

    fun onCategoriaChange(categoria: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(categoria = categoria, categoriaError = null))
        }
    }

    fun registrar() {

        if (_uiState.value.registrando) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(registrando = true, mensajeExito = null)
            }

            val formulario = _uiState.value.formulario

            registrarProducto(
                nombre = formulario.nombre,
                precio = formulario.precio,
                stock = formulario.stock,
                categoria = formulario.categoria
            ).fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            registrando = false,
                            formulario = FormularioProducto(),
                            mensajeExito = "Producto \"${producto.nombre}\" guardado. Si no hay red, se enviará al sincronizar."
                        )
                    }
                },
                onFailure = { fallo ->
                    when (fallo) {

                        is ProductoInvalidoException -> _uiState.update {
                            it.copy(
                                registrando = false,
                                formulario = it.formulario.copy(
                                    nombreError = fallo.errores.nombre,
                                    precioError = fallo.errores.precio,
                                    stockError = fallo.errores.stock,
                                    categoriaError = fallo.errores.categoria
                                )
                            )
                        }

                        else -> _uiState.update {
                            it.copy(
                                registrando = false,
                                fase = ProductoUiState.Fase.Error(
                                    fallo.message ?: "No se pudo registrar el producto"
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}
