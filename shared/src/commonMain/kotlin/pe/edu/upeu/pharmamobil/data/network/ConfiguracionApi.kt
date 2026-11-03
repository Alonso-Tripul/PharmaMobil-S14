package pe.edu.upeu.pharmamobil.data.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

data class ConfiguracionApi(val baseUrl: String)

fun HttpClientConfig<*>.configurarPharmaSoft(config: ConfiguracionApi? = null,
    almacen: AlmacenSeguro? = null, gestor: GestorSesion? = null, debug: Boolean = false) {
    expectSuccess = true
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 15000; connectTimeoutMillis = 10000 }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    if (config != null && almacen != null && gestor != null) {
        val origen = Url(config.baseUrl)
        install(DefaultRequest) { url(config.baseUrl) }
        install(Auth) {
            bearer {
                cacheTokens = false
                loadTokens { almacen.leerToken()?.let { BearerTokens(it, "") } }
                sendWithoutRequest { request ->
                    request.url.esMismoOrigen(origen) && !request.url.encodedPath.endsWith("/auth/login")
                }
            }
        }
        install(createClientPlugin("PharmaOrigenSeguro") {
            on(Send) { request ->
                if (!request.url.esMismoOrigen(origen) || request.url.encodedPath.endsWith("/auth/login")) {
                    request.headers.remove(HttpHeaders.Authorization)
                }
                proceed(request)
            }
        })
        install(Logging) {
            level = if (debug) LogLevel.HEADERS else LogLevel.NONE
            sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
            filter { !it.url.encodedPath.endsWith("/auth/login") }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { error, request ->
                val respuesta = (error as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
                when (respuesta.status) {
                    HttpStatusCode.Unauthorized -> {
                        if (request.url.encodedPath.endsWith("/auth/login")) throw ErrorSesion.CredencialesInvalidas()
                        gestor.cerrar("Tu sesión terminó. Vuelve a iniciar sesión.")
                        throw ErrorSesion.Expirada()
                    }
                    HttpStatusCode.Forbidden -> throw ErrorSesion.SinPermiso()
                    else -> Unit
                }
            }
        }
    }
}

/**
 * URLBuilder guarda el puerto como 0 cuando la URL no lo escribe
 * (http://host/ o https://host/), mientras que Url ya lo resuelve a 80 o 443.
 * Compararlos tal cual quitaba el Bearer en cualquier URL sin puerto explícito.
 */
private fun URLBuilder.esMismoOrigen(origen: Url): Boolean {
    val puerto = if (port == 0 || port == DEFAULT_PORT) protocol.defaultPort else port
    return host == origen.host && puerto == origen.port && protocol == origen.protocol &&
        encodedPath.startsWith(origen.encodedPath)
}
