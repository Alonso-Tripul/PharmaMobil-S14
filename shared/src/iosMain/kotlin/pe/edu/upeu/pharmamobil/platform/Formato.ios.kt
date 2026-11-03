
package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

@OptIn(ExperimentalForeignApi::class)
actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        locale = NSLocale("es_PE")
    }
    return formateador.stringFromNumber(NSNumber(valor)) ?: "S/ $valor"
}
