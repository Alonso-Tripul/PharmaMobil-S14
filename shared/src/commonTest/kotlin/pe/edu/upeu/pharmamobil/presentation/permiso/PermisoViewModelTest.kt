package pe.edu.upeu.pharmamobil.presentation.permiso

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pe.edu.upeu.pharmamobil.data.repository.GestorPermisosFalso
import pe.edu.upeu.pharmamobil.data.repository.TelemetriaFalsa
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso

@OptIn(ExperimentalCoroutinesApi::class)
class PermisoViewModelTest {
    private val telemetria = TelemetriaFalsa()

    @BeforeTest fun instalarMain() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun restaurarMain() { Dispatchers.resetMain() }

    @Test
    fun noPideNadaHastaQueElUsuarioPulsa() = runTest {
        val gestor = GestorPermisosFalso(actual = EstadoPermiso.Denegado)
        val vm = PermisoViewModel(gestor, telemetria)
        assertNull(vm.estado.value)

        vm.refrescar()

        assertEquals(EstadoPermiso.Denegado, vm.estado.value)
        assertEquals(0, gestor.solicitudes)
    }

    @Test
    fun alConcederQuedaConcedidoYSeRegistraLaRespuesta() = runTest {
        val gestor = GestorPermisosFalso(respuestaAlPedir = EstadoPermiso.Concedido)
        val vm = PermisoViewModel(gestor, telemetria)

        vm.solicitar()

        assertEquals(EstadoPermiso.Concedido, vm.estado.value)
        assertEquals("Concedido", telemetria.eventos.single().second["estado"])
    }

    @Test
    fun denegadoParaSiempreSoloOfreceAjustes() = runTest {
        val gestor = GestorPermisosFalso(respuestaAlPedir = EstadoPermiso.DenegadoParaSiempre)
        val vm = PermisoViewModel(gestor, telemetria)

        vm.solicitar()
        vm.abrirAjustes()

        assertEquals(EstadoPermiso.DenegadoParaSiempre, vm.estado.value)
        assertEquals(1, gestor.ajustesAbiertos)
    }

    @Test
    fun alVolverDeAjustesElEstadoSeActualiza() = runTest {
        val gestor = GestorPermisosFalso(actual = EstadoPermiso.DenegadoParaSiempre)
        val vm = PermisoViewModel(gestor, telemetria)
        vm.refrescar()

        gestor.actual = EstadoPermiso.Concedido   // el usuario lo activó en Ajustes
        vm.refrescar()

        assertEquals(EstadoPermiso.Concedido, vm.estado.value)
    }
}
