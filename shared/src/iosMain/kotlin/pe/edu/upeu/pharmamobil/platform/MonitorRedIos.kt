package pe.edu.upeu.pharmamobil.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed

/**
 * Pendiente para iOS: implementar con NWPathMonitor (framework Network).
 * Mientras tanto se asume conectado; la sincronización detecta la falta de
 * red al fallar el envío (ResultadoEnvio.Transitorio con sinRed = true).
 */
class MonitorRedIos : MonitorRed {
    override val conectado: Flow<Boolean> = flowOf(true)
}
