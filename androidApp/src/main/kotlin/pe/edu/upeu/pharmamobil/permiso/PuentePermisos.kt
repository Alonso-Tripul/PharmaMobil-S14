package pe.edu.upeu.pharmamobil.permiso

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Conecta la corrutina con el lanzador de la Activity. Se crea en onCreate,
 * porque registerForActivityResult no se puede llamar después de STARTED, y
 * convierte la respuesta del diálogo en el valor que solicitar() espera.
 */
class PuentePermisos(val actividad: ComponentActivity) {
    private var pendiente: CompletableDeferred<Boolean>? = null

    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> pendiente?.complete(concedido) }

    suspend fun pedir(nombre: String): Boolean {
        val espera = CompletableDeferred<Boolean>()
        pendiente = espera
        lanzador.launch(nombre)
        return espera.await()
    }

    /** Si la Activity se destruye con el diálogo abierto, nadie queda esperando. */
    fun liberar() {
        pendiente?.complete(false)
        pendiente = null
    }
}
