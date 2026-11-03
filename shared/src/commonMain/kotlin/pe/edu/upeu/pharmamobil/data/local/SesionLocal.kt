package pe.edu.upeu.pharmamobil.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Sesion

/** Solo metadatos de una sesión previamente verificada. El JWT sigue en el almacén seguro. */
class SesionLocal(base: PharmaDb) {
    private val consultas = base.productoQueries

    fun guardar(sesion: Sesion) {
        consultas.guardarSesion(
            sesion.usuario, sesion.nombreCompleto,
            JsonArray(sesion.permisos.map { JsonPrimitive(it) }).toString(),
            JsonArray(sesion.roles.map { JsonPrimitive(it) }).toString()
        )
    }

    fun leer(token: String): Sesion? = consultas.leerSesion().executeAsOneOrNull()?.let { fila ->
        Sesion(
            token, fila.usuario, fila.nombre,
            Json.parseToJsonElement(fila.permisos_json).jsonArray.map { it.jsonPrimitive.content }.toSet(),
            Json.parseToJsonElement(fila.roles_json).jsonArray.map { it.jsonPrimitive.content }.toSet()
        )
    }
}
