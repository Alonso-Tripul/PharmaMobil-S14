package pe.edu.upeu.pharmamobil.domain.model

/**
 * Una fila de la cola de envío: el producto creado sin red y lo que la cola
 * recuerda de él. Sin intentos ni último error, una cola que falla en silencio
 * no se distingue de una que funciona.
 */
data class EnvioPendiente(
    val producto: Producto,
    val intentos: Int = 0,
    val ultimoError: String? = null,
    /** Clave de operación: permite al servidor reconocer un POST repetido. */
    val claveOperacion: String? = null
) {
    val id: Long get() = producto.id
}
