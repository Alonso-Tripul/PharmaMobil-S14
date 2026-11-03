package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.network.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.network.configurarPharmaSoft
import kotlin.test.*

class ProductoRepositorioRestTest {
    private fun jsonProducto(id: Int) =
        """{"id":$id,"nombre":"Producto $id","precio":12.5,"stock":3,"estado":true,"categoriaId":1}"""

    @Test fun recorreTodasLasPaginasDelContratoDelBackend() = runTest {
        val paginas = mutableListOf<String?>()
        val motor = MockEngine { solicitud ->
            val pagina = solicitud.url.parameters["pagina"]
            paginas.add(pagina)
            val id = if (pagina == "0") 1 else 2
            respond(
                """{"contenido":[${jsonProducto(id)}],"ultima":${pagina != "0"}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val cliente = HttpClient(motor) { configurarPharmaSoft() }
        try {
            val repositorio = ProductoRepositorioRest(cliente, ConfiguracionApi("http://localhost/api/v1/"))
            assertEquals(listOf(1L, 2L), repositorio.listar().map { it.id })
            assertEquals<List<String?>>(listOf("0", "1"), paginas.toList())
        } finally { cliente.close() }
    }

    @Test fun propagaEl404EnLugarDeMostrarUnProductoDeMemoria() = runTest {
        val cliente = HttpClient(MockEngine { respond("No existe", HttpStatusCode.NotFound) }) {
            configurarPharmaSoft()
        }
        try {
            val repositorio = ProductoRepositorioRest(cliente, ConfiguracionApi("http://localhost/api/v1/"))
            assertFailsWith<ClientRequestException> { repositorio.buscar(99) }
        } finally { cliente.close() }
    }
}
