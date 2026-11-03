package pe.edu.upeu.pharmamobil.domain.platform

/** Epoch en milisegundos. Interfaz para que las pruebas controlen el tiempo. */
fun interface Reloj {
    fun ahora(): Long
}
