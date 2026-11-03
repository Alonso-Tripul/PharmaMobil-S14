package pe.edu.upeu.pharmamobil.domain.model

import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlinx.serialization.json.*

// Solo agenda el cierre local. La firma y la autorización se validan en PharmaSoft.
fun segundosRestantes(token: String, ahora: Long = Clock.System.now().epochSeconds): Long {
    val parte = token.split('.').getOrNull(1) ?: error("Token no válido")
    val texto = Base64.UrlSafe.decode(parte.padEnd((parte.length + 3) / 4 * 4, '=')).decodeToString()
    return (Json.parseToJsonElement(texto).jsonObject.getValue("exp").jsonPrimitive.long - ahora).coerceAtLeast(0)
}
