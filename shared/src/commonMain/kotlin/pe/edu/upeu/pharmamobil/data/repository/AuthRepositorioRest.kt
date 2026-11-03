package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import pe.edu.upeu.pharmamobil.data.network.*
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.data.local.SesionLocal
import kotlinx.coroutines.CancellationException

class AuthRepositorioRest(private val client: HttpClient, private val config: ConfiguracionApi,
    private val almacen: AlmacenSeguro, private val gestor: GestorSesion,
    private val sesionLocal: SesionLocal? = null) : AuthRepository {
    private fun limpiarCache() { client.authProvider<BearerAuthProvider>()?.clearToken() }
    private fun endpoint(path: String) = config.baseUrl.trimEnd('/') + "/" + path
    private fun leer(json: JsonObject, token: String): Sesion = Sesion(token,
        json.getValue("username").jsonPrimitive.content, json.getValue("nombreCompleto").jsonPrimitive.content,
        json.getValue("permisos").jsonArray.map { it.jsonPrimitive.content }.toSet(),
        json["roles"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet())
    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
        val respuesta = client.post(endpoint("auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("username", credenciales.usuario.trim()); put("password", credenciales.clave) })
        }.body<JsonObject>()
        val sesion = leer(respuesta, respuesta.getValue("token").jsonPrimitive.content)
        sesionLocal?.guardar(sesion)
        gestor.establecer(sesion); limpiarCache()
        return sesion
    }
    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        return try {
            leer(client.get(endpoint("auth/me")).body(), token).also {
                sesionLocal?.guardar(it)
                gestor.establecer(it)
            }
        } catch (_: ErrorSesion.Expirada) { null }
        catch (cancelado: CancellationException) { throw cancelado }
        catch (error: ErrorSesion) { throw error }
        catch (_: Exception) {
            // El servidor apagado no impide abrir un catálogo ya guardado.
            if (segundosRestantes(token) <= 0) {
                gestor.cerrar("Tu sesión terminó. Vuelve a iniciar sesión.")
                null
            } else sesionLocal?.leer(token)?.also { gestor.establecer(it) }
        }
    }
    override suspend fun cerrarSesion() { gestor.cerrar(); limpiarCache() }
    override suspend fun comprobarClientes() { client.get(endpoint("clientes")) }
}
