package pe.edu.upeu.pharmamobil.domain.model

class Credenciales(val usuario: String, val clave: String) {
    override fun toString() = "Credenciales(usuario=$usuario)"
}
data class Sesion(val token: String, val usuario: String, val nombreCompleto: String, val permisos: Set<String>, val roles: Set<String> = emptySet()) {
    fun puede(permiso: String) = permiso in permisos
    override fun toString() = "Sesion(usuario=$usuario)"
}
sealed class ErrorSesion(mensaje: String) : Exception(mensaje) {
    class CredencialesInvalidas : ErrorSesion("Usuario o contraseña incorrectos.")
    class Expirada : ErrorSesion("Tu sesión terminó. Vuelve a iniciar sesión.")
    class SinPermiso : ErrorSesion("No tienes permiso para realizar esta acción.")
}
