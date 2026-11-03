package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * viewModelScope corre sobre Dispatchers.Main, que en una prueba no existe:
 * setMain lo sustituye por un dispatcher de prueba antes de cada caso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = ProductoViewModel(
        registrarProducto = RegistrarProductoUseCase(repositorio),
        listarProductos = ListarProductosUseCase(repositorio)
    )

    @Test
    fun arrancaEnSinProductosCuandoElInventarioEstaVacio() = runTest {

        val viewModel = nuevoViewModel()

        assertEquals(ProductoUiState.Fase.SinProductos, viewModel.uiState.value.fase)
    }

    @Test
    fun muestraElInventarioConElPrecioYaFormateado() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5)
            )
        )

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        assertEquals(pe.edu.upeu.pharmamobil.platform.formatearSoles(12.5), fase.productos.first().precio)
        assertEquals("5 u.", fase.productos.first().stock)
        assertTrue(fase.productos.first().requiereReposicion)
    }

    @Test
    fun losErroresDeValidacionCaenEnElFormularioNoEnLaFase() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("")
        viewModel.onPrecioChange("abc")
        viewModel.onStockChange("-1")
        viewModel.registrar()

        val estado = viewModel.uiState.value

        assertEquals("El nombre es obligatorio", estado.formulario.nombreError)
        assertEquals("El precio debe ser un número válido", estado.formulario.precioError)
        assertEquals("El stock no puede ser negativo", estado.formulario.stockError)
        assertEquals(ProductoUiState.Fase.SinProductos, estado.fase)
        assertNull(estado.mensajeExito)
    }

    @Test
    fun registrarLimpiaElFormularioYElFlowActualizaElInventario() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("12.50")
        viewModel.onStockChange("5")
        viewModel.registrar()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals("Paracetamol", fase.productos.single().nombre)
        assertEquals("", estado.formulario.nombre)
        assertEquals("", estado.formulario.precio)
        assertEquals("", estado.formulario.stock)
        assertEquals(
            "Producto \"Paracetamol\" guardado. Si no hay red, se enviará al sincronizar.",
            estado.mensajeExito
        )
    }
}
