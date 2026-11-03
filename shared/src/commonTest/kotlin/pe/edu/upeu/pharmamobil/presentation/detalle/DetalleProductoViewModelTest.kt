package pe.edu.upeu.pharmamobil.presentation.detalle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleProductoViewModelTest {
    @BeforeTest fun instalarMain() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun restaurarMain() { Dispatchers.resetMain() }

    @Test fun comparteElProductoPersistidoAunqueElFormularioSeHayaEditado() = runTest {
        val producto = Producto(7, "Paracetamol", 12.5, 8)
        val capturados = mutableListOf<String>()
        val compartidor = object : Compartidor {
            override fun compartir(texto: String) { capturados.add(texto) }
        }
        val vm = DetalleProductoViewModel(FakeProductoRepository(mutableListOf(producto)), compartidor)
        vm.cargar(7)
        vm.onNombreChange("Todavía sin guardar")
        vm.onPrecioChange("99.00")
        vm.compartir()
        assertEquals(listOf(producto.comoTextoParaCompartir()), capturados)
    }

    @Test fun noComparteUnProductoAnteriorTrasFallarLaCargaDelSiguiente() = runTest {
        val capturados = mutableListOf<String>()
        val producto = Producto(7, "Paracetamol", 12.5, 8)
        val compartidor = object : Compartidor {
            override fun compartir(texto: String) { capturados.add(texto) }
        }
        val vm = DetalleProductoViewModel(FakeProductoRepository(mutableListOf(producto)), compartidor)
        vm.cargar(7)
        vm.cargar(999)
        vm.compartir()
        assertTrue(capturados.isEmpty())
        assertFalse(vm.uiState.value.cargando)
        assertNull(vm.uiState.value.producto)
        assertNotNull(vm.uiState.value.mensaje)
    }

    @Test fun elErrorDelSelectorSeMuestraSinCerrarLaPantalla() = runTest {
        val producto = Producto(7, "Paracetamol", 12.5, 8)
        val compartidor = object : Compartidor {
            override fun compartir(texto: String) { error("Sin aplicaciones para compartir") }
        }
        val vm = DetalleProductoViewModel(FakeProductoRepository(mutableListOf(producto)), compartidor)
        vm.cargar(7)
        vm.compartir()
        assertEquals("Sin aplicaciones para compartir", vm.uiState.value.mensaje)
        assertNotNull(vm.uiState.value.producto)
    }
}
