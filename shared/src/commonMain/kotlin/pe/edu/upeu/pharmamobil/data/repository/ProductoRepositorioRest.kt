package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import pe.edu.upeu.pharmamobil.data.network.ConfiguracionApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Contrato del ProductoController incluido: paginación con contenido/ultima. */
class ProductoRepositorioRest(
    private val cliente: HttpClient,
    configuracion: ConfiguracionApi
) : ProductoRepository {
    private val endpoint = configuracion.baseUrl.trimEnd('/') + "/productos"

    override suspend fun listar(): List<Producto> {
        val productos = mutableListOf<Producto>()
        var pagina = 0
        do {
            val respuesta = cliente.get(endpoint) {
                parameter("pagina", pagina)
                parameter("tamanio", 100)
                parameter("ordenarPor", "id")
                parameter("direccion", "asc")
            }.body<JsonObject>()
            val contenido = requireNotNull(respuesta["contenido"]) {
                "PharmaSoft no devolvió el campo contenido"
            }.jsonArray
            productos.addAll(contenido.map { it.jsonObject.aProducto() })
            val ultima = requireNotNull(respuesta["ultima"]).jsonPrimitive.boolean
            if (ultima) break
            check(contenido.isNotEmpty()) { "Respuesta paginada sin productos ni fin" }
            pagina++
        } while (true)
        return productos
    }

    override suspend fun buscar(id: Long): Producto =
        cliente.get("$endpoint/$id").body<JsonObject>().aProducto()

    override suspend fun registrar(producto: Producto): Producto =
        cliente.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(producto.aSolicitud())
        }.body<JsonObject>().aProducto()

    override suspend fun actualizar(producto: Producto): Producto =
        cliente.put("$endpoint/${producto.id}") {
            contentType(ContentType.Application.Json)
            setBody(producto.aSolicitud())
        }.body<JsonObject>().aProducto()

    override suspend fun eliminar(id: Long) {
        cliente.delete("$endpoint/$id")
    }

    private fun Producto.aSolicitud(): JsonObject = buildJsonObject {
        put("nombre", nombre)
        put("precio", precio)
        put("stock", stock)
        put("estado", estado)
        put("categoriaId", categoriaId)
    }

    private fun JsonObject.aProducto() = Producto(
        id = getValue("id").jsonPrimitive.long,
        nombre = getValue("nombre").jsonPrimitive.content,
        precio = getValue("precio").jsonPrimitive.double,
        stock = getValue("stock").jsonPrimitive.int,
        categoriaId = getValue("categoriaId").jsonPrimitive.long,
        estado = getValue("estado").jsonPrimitive.boolean
    )
}
