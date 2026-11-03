package pe.edu.upeu.pharmamobil.domain.usecase

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobil.data.network.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.network.GestorSesion
import pe.edu.upeu.pharmamobil.data.network.configurarPharmaSoft
import pe.edu.upeu.pharmamobil.data.repository.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.EnvioRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase.Companion.MAX_INTENTOS

/**
 * Misma estrategia que la sesión 11: base en memoria y motor HTTP simulado.
 * Pasa por la traducción real de errores (ErrorApi → ResultadoEnvio).
 */
class SincronizacionTest {
    private val config = ConfiguracionApi("http://localhost/api/v1/")
    private val local = AlmacenEnMemoria()
    private val peticiones = mutableListOf<String>()
    private var cliente: HttpClient? = null
    private lateinit var gestor: GestorSesion
    private val laborDePrueba = Producto(0, "Paracetamol 500 mg", 2.5, 40)

    private class Almacen : AlmacenSeguro {
        var token: String? = "token"
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken() = token
        override fun borrarToken() { token = null }
    }

    @AfterTest
    fun cerrar() { cliente?.close() }

    private fun armarCasoDeUso(motor: MockEngine): SincronizarUseCase {
        val almacen = Almacen()
        gestor = GestorSesion(almacen).apply { establecer(Sesion("token", "admin", "Admin", setOf("CATALOGO_ESCRIBIR"))) }
        val http = HttpClient(motor) { configurarPharmaSoft(config, almacen, gestor) }
        cliente = http
        val remoto = EnvioRepositorioRest(ProductoRepositorioRest(http, config))
        return SincronizarUseCase(local, remoto, reloj = { 1_000_000L })
    }

    /** Responde POST con [alCrear] y GET con una página vacía. */
    private fun motor(alCrear: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        MockEngine { request ->
            peticiones += request.method.value
            if (request.method == HttpMethod.Post) alCrear(request)
            else respond(PAGINA_VACIA, headers = JSON)
        }

    @Test
    fun unFalloTransitorioSumaIntentoYNoMarcaFallida() = runTest {
        val caso = armarCasoDeUso(motor { respondError(HttpStatusCode.ServiceUnavailable) })
        val fila = local.guardarPendiente(laborDePrueba)

        val resumen = caso()

        val enCola = local.porId(fila.id)!!
        assertEquals(1, enCola.intentos)
        assertEquals(1, local.contarPendientes(MAX_INTENTOS))
        assertTrue(local.observarFallidas(MAX_INTENTOS).first().isEmpty())
        assertEquals(0, resumen.fallidas)
        // Esperó 1 s antes de seguir (tiempo virtual de runTest).
        assertEquals(1_000L, currentTime)
    }

    @Test
    fun unFalloPermanenteMarcaLaFilaYNoLaReintenta() = runTest {
        val caso = armarCasoDeUso(motor {
            respond("""{"status":409,"message":"Ya existe un producto con el nombre Paracetamol 500 mg"}""",
                HttpStatusCode.Conflict, JSON)
        })
        local.guardarPendiente(laborDePrueba)

        val primero = caso()
        val segundo = caso()

        assertEquals(1, primero.fallidas)
        val fallida = local.observarFallidas(MAX_INTENTOS).first().single()
        assertEquals("Ya existe un producto con el nombre Paracetamol 500 mg", fallida.ultimoError)
        assertEquals(0, segundo.fallidas)
        assertEquals(1, peticiones.count { it == "POST" })   // no se reenvió
        // La fila sigue visible en la lista para que el usuario decida.
        assertEquals(1, local.listar().size)
    }

    @Test
    fun laValidacionDelServidorGuardaElMotivoPorCampo() = runTest {
        val caso = armarCasoDeUso(motor {
            respond("""{"status":400,"message":"Existen errores de validación",
                "validationErrors":{"precio":"El precio debe ser mayor que cero"}}""",
                HttpStatusCode.BadRequest, JSON)
        })
        local.guardarPendiente(laborDePrueba)

        caso()

        val fallida = local.observarFallidas(MAX_INTENTOS).first().single()
        assertEquals("precio: El precio debe ser mayor que cero", fallida.ultimoError)
    }

    @Test
    fun elExitoEnviaLaClaveDeOperacionYSubeAntesDeBajar() = runTest {
        var clave: String? = null
        val caso = armarCasoDeUso(motor { request ->
            clave = request.headers[ProductoRepositorioRest.CABECERA_IDEMPOTENCIA]
            respond(PRODUCTO_CREADO, HttpStatusCode.Created, JSON)
        })
        val fila = local.guardarPendiente(laborDePrueba)
        val claveLocal = local.porId(fila.id)!!.claveOperacion

        val resumen = caso()

        assertEquals(claveLocal, clave)
        assertEquals(listOf("POST", "GET"), peticiones)
        assertEquals(1, resumen.enviadas)
        assertNull(local.porId(fila.id))
        assertTrue(resumen.descargado)
        assertEquals(1_000_000L, local.observarUltimaSync().first())
    }

    @Test
    fun unaFilaRechazadaNoBloqueaALasDemas() = runTest {
        var n = 0
        val caso = armarCasoDeUso(motor {
            if (n++ == 0) respond("""{"message":"Categoria no encontrada con id: 99"}""", HttpStatusCode.NotFound, JSON)
            else respond(PRODUCTO_CREADO, HttpStatusCode.Created, JSON)
        })
        local.guardarPendiente(laborDePrueba.copy(categoriaId = 99))
        local.guardarPendiente(laborDePrueba.copy(nombre = "Ibuprofeno 400 mg"))

        val resumen = caso()

        assertEquals(1, resumen.enviadas)
        assertEquals(1, resumen.fallidas)
        assertEquals("Categoria no encontrada con id: 99",
            local.observarFallidas(MAX_INTENTOS).first().single().ultimoError)
    }

    @Test
    fun alAgotarLosIntentosLaFilaQuedaFallidaYVisible() = runTest {
        val caso = armarCasoDeUso(motor { respondError(HttpStatusCode.ServiceUnavailable) })
        local.guardarPendiente(laborDePrueba)

        repeat(MAX_INTENTOS + 2) { caso() }

        assertEquals(0, local.contarPendientes(MAX_INTENTOS))
        val fallida = local.observarFallidas(MAX_INTENTOS).first().single()
        assertEquals(MAX_INTENTOS, fallida.intentos)
        assertEquals(MAX_INTENTOS, peticiones.count { it == "POST" })   // límite respetado
        // 1 + 2 + 4 + 8 + 16 s: la espera crece.
        assertEquals(31_000L, currentTime)
    }

    @Test
    fun sinRedCortaLaColaSinCastigarALasDemasYNoBorraNada() = runTest {
        val caso = armarCasoDeUso(MockEngine {
            peticiones += it.method.value
            throw kotlinx.io.IOException("Network is unreachable")
        })
        val primera = local.guardarPendiente(laborDePrueba)
        val segunda = local.guardarPendiente(laborDePrueba.copy(nombre = "Ibuprofeno 400 mg"))

        val resumen = caso()

        assertEquals(1, local.porId(primera.id)!!.intentos)
        assertEquals(0, local.porId(segunda.id)!!.intentos)
        assertEquals(listOf("POST"), peticiones)          // no intentó descargar
        assertFalse(resumen.descargado)
        assertEquals("Sin conexión con el servidor", resumen.aviso)
        assertEquals(2, local.listar().size)
    }

    @Test
    fun un401DetieneLaColaSinMarcarFilas() = runTest {
        val caso = armarCasoDeUso(motor { respondError(HttpStatusCode.Unauthorized) })
        val fila = local.guardarPendiente(laborDePrueba)

        val resumen = caso()

        assertTrue(resumen.sesionVencida)
        assertEquals(0, local.porId(fila.id)!!.intentos)
        assertNull(gestor.sesion.value)
    }

    @Test
    fun laEsperaCreceYTieneTope() {
        val caso = SincronizarUseCase(local, pe.edu.upeu.pharmamobil.data.repository.RemotoFalso(), { 0L })
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 16_000L), (0..5).map { caso.espera(it) })
    }

    private companion object {
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        const val PAGINA_VACIA = """{"contenido":[],"ultima":true}"""
        const val PRODUCTO_CREADO =
            """{"id":41,"nombre":"Paracetamol 500 mg","precio":2.5,"stock":40,"categoriaId":1,"estado":true}"""
    }
}

