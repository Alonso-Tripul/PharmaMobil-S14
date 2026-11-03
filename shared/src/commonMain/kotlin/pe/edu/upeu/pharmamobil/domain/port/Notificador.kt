package pe.edu.upeu.pharmamobil.domain.port

/** Aviso local del sistema. Si el permiso no está concedido, no hace nada. */
interface Notificador {
    fun avisar(titulo: String, texto: String)
}
