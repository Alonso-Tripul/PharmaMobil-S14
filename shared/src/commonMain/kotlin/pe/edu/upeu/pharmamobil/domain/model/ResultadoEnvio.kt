package pe.edu.upeu.pharmamobil.domain.model

/**
 * Lo que pasó al enviar una fila. La capa de datos traduce cada error HTTP a
 * una de estas ramas; el caso de uso nunca ve un código de estado.
 */
sealed interface ResultadoEnvio {
    data class Exito(val producto: Producto) : ResultadoEnvio {
        val idServidor: Long get() = producto.id
    }

    /**
     * Sin red, tiempo agotado o servicio caído: se reintenta con espera.
     * [sinRed] indica que el servidor no se alcanzó: las filas siguientes
     * tampoco lo alcanzarán, así que la cola se corta sin castigarlas.
     */
    data class Transitorio(val motivo: String, val sinRed: Boolean = false) : ResultadoEnvio

    /** Validación, recurso inexistente, conflicto o falta de privilegio: no se reintenta. */
    data class Permanente(val motivo: String) : ResultadoEnvio

    /**
     * 401: la culpa no es de la fila. Se detiene la cola sin tocar ninguna
     * fila; al volver a iniciar sesión se envían con sus intentos intactos.
     */
    data object SesionVencida : ResultadoEnvio
}

data class ResumenSync(
    val enviadas: Int,
    val fallidas: Int,
    val pendientes: Int,
    /** true si además de subir se pudo bajar el inventario del servidor. */
    val descargado: Boolean,
    /** Motivo del último fallo transitorio o de la descarga, para la pantalla. */
    val aviso: String? = null,
    val sesionVencida: Boolean = false
)
