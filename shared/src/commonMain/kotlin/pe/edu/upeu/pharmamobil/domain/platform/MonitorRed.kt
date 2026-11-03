package pe.edu.upeu.pharmamobil.domain.platform

import kotlinx.coroutines.flow.Flow

/** Si el dispositivo tiene una red con acceso a Internet. No garantiza que PharmaSoft responda. */
interface MonitorRed {
    val conectado: Flow<Boolean>
}
