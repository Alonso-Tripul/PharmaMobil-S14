package pe.edu.upeu.pharmamobil.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.ProveedorUsuario

class GestorSesion(private val almacen: AlmacenSeguro) : ProveedorUsuario {
    override fun usuarioActual(): String = requireNotNull(actual.value?.usuario) { "Inicia sesión para ver el inventario" }
    private val actual = MutableStateFlow<Sesion?>(null)
    val sesion = actual.asStateFlow()
    private val mensaje = MutableStateFlow<String?>(null)
    val aviso = mensaje.asStateFlow()
    fun establecer(sesion: Sesion) { almacen.guardarToken(sesion.token); actual.value = sesion; mensaje.value = null }
    fun cerrar(aviso: String? = null) { almacen.borrarToken(); actual.value = null; mensaje.value = aviso }
}
