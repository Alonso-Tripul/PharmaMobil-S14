package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

/** Estado del permiso de notificaciones, que la app usa para avisar de productos no enviados. */
class PermisoViewModel(
    private val gestor: GestorPermisos,
    private val telemetria: Telemetria,
    private val permiso: Permiso = Permiso.NOTIFICACIONES,
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoPermiso?>(null)
    val estado: StateFlow<EstadoPermiso?> = _estado.asStateFlow()

    /** Se llama al volver a la pantalla: el usuario pudo cambiarlo en Ajustes. */
    fun refrescar() {
        viewModelScope.launch { _estado.value = gestor.estado(permiso) }
    }

    /** Solo después de que la tarjeta explicó para qué se pide. */
    fun solicitar() {
        viewModelScope.launch {
            val resultado = gestor.solicitar(permiso)
            _estado.value = resultado
            telemetria.registrarEvento(
                "permiso_respuesta",
                mapOf("permiso" to permiso.name, "estado" to (resultado::class.simpleName ?: "?"))
            )
        }
    }

    fun abrirAjustes() = gestor.abrirAjustes()
}
