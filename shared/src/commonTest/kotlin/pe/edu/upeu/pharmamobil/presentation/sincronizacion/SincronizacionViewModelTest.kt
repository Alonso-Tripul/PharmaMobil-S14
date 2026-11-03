package pe.edu.upeu.pharmamobil.presentation.sincronizacion

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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobil.data.repository.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.NotificadorFalso
import pe.edu.upeu.pharmamobil.data.repository.RedFalsa
import pe.edu.upeu.pharmamobil.data.repository.RemotoFalso
import pe.edu.upeu.pharmamobil.data.repository.TelemetriaFalsa
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class SincronizacionViewModelTest {
    private val local = AlmacenEnMemoria()
    private val remoto = RemotoFalso()
    private val red = RedFalsa()
    private val notificador = NotificadorFalso()
    private val telemetria = TelemetriaFalsa()
    private val ahora = 10 * 60 * 60_000L

    @BeforeTest fun instalarMain() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun restaurarMain() { Dispatchers.resetMain() }

    private fun nuevo() = SincronizacionViewModel(
        SincronizarUseCase(local, remoto) { ahora }, local, red, { ahora }, notificador, telemetria
    )

    @Test
    fun sinRedMuestraLosCambiosPorEnviarYNoLlamaAlServidor() = runTest {
        red.conectado.value = false
        local.guardarPendiente(Producto(0, "Paracetamol", 2.5, 40))
        local.guardarPendiente(Producto(0, "Ibuprofeno", 3.0, 20))

        val estado = nuevo().estado.value

        assertEquals(2, estado.pendientes)
        assertEquals("Sin conexión · 2 cambios por enviar", estado.titulo)
        assertEquals("Aún no hay datos del servidor", estado.antiguedad)
        assertTrue(remoto.enviadas.isEmpty())
    }

    @Test
    fun alVolverLaRedSincronizaSolaYLaPantallaLoRefleja() = runTest {
        red.conectado.value = false
        local.guardarPendiente(Producto(0, "Paracetamol", 2.5, 40))
        val vm = nuevo()

        red.conectado.value = true

        assertEquals(1, remoto.enviadas.size)
        assertEquals(0, vm.estado.value.pendientes)
        assertEquals("Todo enviado", vm.estado.value.titulo)
        assertEquals("Actualizado hace un momento", vm.estado.value.antiguedad)
        assertEquals("sync_completada", telemetria.eventos.last().first)
    }

    @Test
    fun registrarConRedDisparaLaSincronizacionPorqueCreceLaCola() = runTest {
        val vm = nuevo()
        assertEquals(1, remoto.descargas)          // al abrir la pantalla

        local.guardarPendiente(Producto(0, "Amoxicilina", 8.0, 30))

        assertEquals(1, remoto.enviadas.size)
        assertEquals(0, vm.estado.value.pendientes)
    }

    @Test
    fun unaFilaRechazadaQuedaVisibleAvisaYSePuedeReintentar() = runTest {
        remoto.responder = { ResultadoEnvio.Permanente("Ya existe un producto con el nombre Paracetamol") }
        red.conectado.value = false
        local.guardarPendiente(Producto(0, "Paracetamol", 2.5, 40))
        val vm = nuevo()
        red.conectado.value = true

        val estado = vm.estado.value
        assertEquals(1, estado.fallidas)
        assertEquals("1 producto no se pudo enviar", estado.textoFallidas)
        assertEquals("Ya existe un producto con el nombre Paracetamol", estado.filasFallidas.single().motivo)
        assertEquals(1, notificador.avisos.size)
        assertIs<FilasRechazadasException>(telemetria.errores.single())

        remoto.responder = { ResultadoEnvio.Exito(it.producto.copy(id = 7)) }
        vm.reintentarFallidas()

        assertEquals(0, vm.estado.value.fallidas)
        assertEquals(2, remoto.enviadas.size)
    }

    @Test
    fun descartarQuitaLaFilaFallida() = runTest {
        remoto.responder = { ResultadoEnvio.Permanente("Categoria no encontrada con id: 99") }
        local.guardarPendiente(Producto(0, "Paracetamol", 2.5, 40, categoriaId = 99))
        val vm = nuevo()
        val id = vm.estado.value.filasFallidas.single().id

        vm.descartar(id)

        assertEquals(0, vm.estado.value.fallidas)
        assertTrue(local.listar().isEmpty())
    }

    @Test
    fun laAntiguedadSeRedactaEnLenguajeNatural() {
        val hora = 60 * 60_000L
        assertEquals("Actualizado hace 2 h", describirAntiguedad(0L, 2 * hora + 5 * 60_000L))
        assertEquals("Actualizado hace 15 min", describirAntiguedad(0L, 15 * 60_000L))
        assertEquals("Actualizado hace 3 d", describirAntiguedad(0L, 3 * 24 * hora))
    }
}
