package pe.edu.upeu.pharmamobil.data.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion

/**
 * Errores de PharmaSoft clasificados. Es el único lugar de la app donde se
 * mira un código HTTP; hacia arriba solo viajan estas ramas.
 */
sealed class ErrorApi(val mensaje: String) {
    class SinConexion : ErrorApi("Sin conexión con el servidor")
    class TiempoAgotado : ErrorApi("El servidor tardó demasiado en responder")
    class Servidor(codigo: Int) : ErrorApi("El servidor no está disponible ($codigo)")
    /** [campos] trae los validationErrors del GlobalExceptionHandler: campo → mensaje. */
    class Validacion(mensaje: String, val campos: Map<String, String> = emptyMap()) : ErrorApi(mensaje)
    class NoEncontrado(mensaje: String) : ErrorApi(mensaje)
    class Conflicto(mensaje: String) : ErrorApi(mensaje)
    class SinPrivilegio : ErrorApi("No tienes permiso para registrar productos")
    class NoAutorizado : ErrorApi("Tu sesión terminó")
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** Traduce cualquier fallo de una petición. Relanza la cancelación de la corrutina. */
suspend fun Throwable.aErrorApi(): ErrorApi = when (this) {
    is CancellationException -> throw this
    is ErrorSesion.Expirada -> ErrorApi.NoAutorizado()
    is ErrorSesion.SinPermiso -> ErrorApi.SinPrivilegio()
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
        ErrorApi.TiempoAgotado()
    is ResponseException -> desdeRespuesta(this)
    // ConnectException (servidor apagado) y la falta de red son IOException.
    is kotlinx.io.IOException -> ErrorApi.SinConexion()
    // Cuerpo inesperado u otro fallo: se trata como del servidor y se reintenta.
    else -> ErrorApi.Servidor(0)
}

private suspend fun desdeRespuesta(error: ResponseException): ErrorApi {
    val codigo = error.response.status.value
    val cuerpo = try {
        json.parseToJsonElement(error.response.bodyAsText()).jsonObject
    } catch (cancelado: CancellationException) {
        throw cancelado
    } catch (_: Exception) {
        null
    }
    val mensaje = cuerpo?.texto("message")
    val campos = cuerpo?.get("validationErrors")
        ?.let { it as? JsonObject }
        ?.mapValues { (_, v) -> v.jsonPrimitive.contentOrNull.orEmpty() }
        .orEmpty()
    return when (codigo) {
        401 -> ErrorApi.NoAutorizado()
        403 -> ErrorApi.SinPrivilegio()
        404 -> ErrorApi.NoEncontrado(mensaje ?: "El recurso no existe en el servidor")
        409 -> ErrorApi.Conflicto(mensaje ?: "El servidor rechazó el registro por un conflicto")
        408, 429 -> ErrorApi.TiempoAgotado()
        in 500..599 -> ErrorApi.Servidor(codigo)
        else -> ErrorApi.Validacion(
            if (campos.isNotEmpty()) campos.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            else mensaje ?: "El servidor rechazó los datos ($codigo)",
            campos
        )
    }
}

private fun JsonObject.texto(clave: String): String? =
    (get(clave) as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
