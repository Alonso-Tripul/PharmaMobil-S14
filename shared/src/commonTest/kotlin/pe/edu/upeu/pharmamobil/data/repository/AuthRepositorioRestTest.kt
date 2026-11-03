package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import pe.edu.upeu.pharmamobil.data.network.*
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

class AuthRepositorioRestTest {
    private class Almacen : AlmacenSeguro {
        var token: String? = null
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken() = token
        override fun borrarToken() { token = null }
    }
    private val config = ConfiguracionApi("http://localhost/api/v1/")
    private fun sesion() = Sesion("token-prueba", "admin", "Administrador", setOf("CATALOGO_ESCRIBIR"))

    @Test fun loginNoLlevaBearerYSiguienteConsultaSi() = runTest {
        val almacen = Almacen(); val gestor = GestorSesion(almacen)
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/login")) {
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("""{"token":"token-prueba","username":"admin","nombreCompleto":"Administrador","roles":["ADMIN"],"permisos":["CATALOGO_ESCRIBIR"]}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                assertEquals("Bearer token-prueba", request.headers[HttpHeaders.Authorization])
                respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }) { configurarPharmaSoft(config, almacen, gestor) }
        try {
            val repo = AuthRepositorioRest(client, config, almacen, gestor)
            assertTrue(repo.iniciarSesion(Credenciales("admin", "clave")).puede("CATALOGO_ESCRIBIR"))
            assertEquals(setOf("ADMIN"), gestor.sesion.value?.roles)
            repo.comprobarClientes(); repo.cerrarSesion()
            assertNull(almacen.token); assertNull(gestor.sesion.value)
        } finally { client.close() }
    }
    @Test fun error401ProtegidoBorraSesionPero403LaConserva() = runTest {
        for (status in listOf(HttpStatusCode.Forbidden, HttpStatusCode.Unauthorized)) {
            val almacen = Almacen(); val gestor = GestorSesion(almacen); gestor.establecer(sesion())
            val client = HttpClient(MockEngine { respond("{}", status) }) { configurarPharmaSoft(config, almacen, gestor) }
            try {
                val repo = AuthRepositorioRest(client, config, almacen, gestor)
                if (status == HttpStatusCode.Forbidden) {
                    assertFailsWith<ErrorSesion.SinPermiso> { repo.comprobarClientes() }
                    assertNotNull(gestor.sesion.value); assertNotNull(almacen.token)
                } else {
                    assertFailsWith<ErrorSesion.Expirada> { repo.comprobarClientes() }
                    assertNull(gestor.sesion.value); assertNull(almacen.token)
                }
            } finally { client.close() }
        }
    }
    @Test fun login401InformaCredencialesInvalidas() = runTest {
        val almacen = Almacen(); val gestor = GestorSesion(almacen)
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.Unauthorized) }) { configurarPharmaSoft(config, almacen, gestor) }
        try {
            assertFailsWith<ErrorSesion.CredencialesInvalidas> {
                AuthRepositorioRest(client, config, almacen, gestor).iniciarSesion(Credenciales("admin", "mala"))
            }
            assertNull(gestor.aviso.value)
        } finally { client.close() }
    }
    @Test fun restauraConMeYNoFiltraTokenAOtroServidor() = runTest {
        val almacen = Almacen().apply { token = "guardado" }; val gestor = GestorSesion(almacen)
        val client = HttpClient(MockEngine { request ->
            if (request.url.host == "localhost") {
                assertEquals("Bearer guardado", request.headers[HttpHeaders.Authorization])
                respond("""{"username":"admin","nombreCompleto":"Administrador","roles":["ADMIN"],"permisos":[]}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                assertNull(request.headers[HttpHeaders.Authorization]); respond("{}")
            }
        }) { configurarPharmaSoft(config, almacen, gestor) }
        try {
            val repo = AuthRepositorioRest(client, config, almacen, gestor)
            assertEquals("admin", repo.sesionGuardada()?.usuario)
            assertEquals(setOf("ADMIN"), gestor.sesion.value?.roles)
            client.get("http://otro-servidor/api/v1/productos")
        } finally { client.close() }
    }
    @Test fun expiracionLocalYPermisos() {
        val payload = kotlin.io.encoding.Base64.UrlSafe.encode("{\"exp\":100}".encodeToByteArray()).trimEnd('=')
        assertEquals(10L, segundosRestantes("a.$payload.c", 90))
        assertEquals(0L, segundosRestantes("a.$payload.c", 101))
        assertFalse(sesion().puede("CLIENTES_LEER"))
    }
}
