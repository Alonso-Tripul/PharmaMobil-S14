package pe.edu.upeu.pharmamobil.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import pe.edu.upeu.pharmamobil.data.local.SesionLocal
import pe.edu.upeu.pharmamobil.data.network.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.network.GestorSesion
import pe.edu.upeu.pharmamobil.data.network.configurarPharmaSoft
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

class SesionOfflineTest {
    @Test
    fun reabreUnaSesionVigenteCuandoElServidorNoResponde() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val payload = Base64.UrlSafe.encode(
            """{"exp":${Clock.System.now().epochSeconds + 3600}}""".encodeToByteArray()
        ).trimEnd('=')
        val token = "a.$payload.c"
        val almacen = object : AlmacenSeguro {
            var guardado: String? = token
            override fun guardarToken(token: String) { guardado = token }
            override fun leerToken() = guardado
            override fun borrarToken() { guardado = null }
        }
        val gestor = GestorSesion(almacen)
        val config = ConfiguracionApi("http://localhost/api/v1/")
        val cliente = HttpClient(MockEngine { respond("{}", HttpStatusCode.ServiceUnavailable) }) {
            configurarPharmaSoft(config, almacen, gestor)
        }
        try {
            PharmaDb.Schema.create(driver)
            val cache = SesionLocal(PharmaDb(driver))
            cache.guardar(Sesion(token, "admin", "Administrador", setOf("CATALOGO_LEER"), setOf("ADMIN")))
            val repo = AuthRepositorioRest(cliente, config, almacen, gestor, cache)

            val restaurada = assertNotNull(repo.sesionGuardada())

            assertEquals("admin", restaurada.usuario)
            assertEquals(token, gestor.sesion.value?.token)
            assertEquals(setOf("CATALOGO_LEER"), restaurada.permisos)
        } finally {
            cliente.close()
            driver.close()
        }
    }
}
