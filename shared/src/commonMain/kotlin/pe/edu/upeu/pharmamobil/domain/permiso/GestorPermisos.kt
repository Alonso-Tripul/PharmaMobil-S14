package pe.edu.upeu.pharmamobil.domain.permiso

enum class Permiso { CAMARA, UBICACION, NOTIFICACIONES }

sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso
    data object Denegado : EstadoPermiso               // se puede volver a pedir
    data object DenegadoParaSiempre : EstadoPermiso    // solo queda Ajustes
}

/**
 * Se declara aquí como interfaz porque la implementación de Android necesita
 * la Activity para lanzar el diálogo del sistema, y el código común no puede
 * nombrarla. Cada plataforma registra la suya con Koin.
 */
interface GestorPermisos {
    suspend fun estado(permiso: Permiso): EstadoPermiso
    suspend fun solicitar(permiso: Permiso): EstadoPermiso
    fun abrirAjustes()
}
