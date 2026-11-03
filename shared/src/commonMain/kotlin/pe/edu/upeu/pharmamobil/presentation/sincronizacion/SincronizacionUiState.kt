package pe.edu.upeu.pharmamobil.presentation.sincronizacion

data class SincronizacionUiState(
    val enCurso: Boolean = false,
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    val ultimaSync: Long? = null,          // epoch en milisegundos
    val conectado: Boolean = true,
    val filasFallidas: List<FilaFallidaUi> = emptyList(),
    val aviso: String? = null,
    val ahora: Long = 0L,
) {
    /** «Sin conexión · 3 cambios por enviar» dice más que un icono rojo. */
    val titulo: String
        get() = buildString {
            append(
                when {
                    enCurso -> "Sincronizando…"
                    !conectado -> "Sin conexión"
                    pendientes == 0 && fallidas == 0 -> "Todo enviado"
                    else -> "En línea"
                }
            )
            if (pendientes > 0) append(" · $pendientes ${if (pendientes == 1) "cambio" else "cambios"} por enviar")
        }

    /** «Actualizado hace 2 h» dice más que una fecha completa. */
    val antiguedad: String
        get() = describirAntiguedad(ultimaSync, ahora)

    val textoFallidas: String?
        get() = when (fallidas) {
            0 -> null
            1 -> "1 producto no se pudo enviar"
            else -> "$fallidas productos no se pudieron enviar"
        }
}

data class FilaFallidaUi(val id: Long, val nombre: String, val motivo: String, val intentos: Int)

fun describirAntiguedad(ultimaSync: Long?, ahora: Long): String {
    if (ultimaSync == null) return "Aún no hay datos del servidor"
    val minutos = ((ahora - ultimaSync).coerceAtLeast(0L)) / 60_000L
    return when {
        minutos < 1 -> "Actualizado hace un momento"
        minutos < 60 -> "Actualizado hace $minutos min"
        minutos < 60 * 24 -> "Actualizado hace ${minutos / 60} h"
        else -> "Actualizado hace ${minutos / (60 * 24)} d"
    }
}
