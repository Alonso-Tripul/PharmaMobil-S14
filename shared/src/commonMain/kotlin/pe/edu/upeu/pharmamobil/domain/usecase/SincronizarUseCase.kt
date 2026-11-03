package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.platform.Reloj
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRemotoRepository

/**
 * Sube la cola y después baja el inventario. Se registra como single en Koin:
 * el candado evita dos sincronizaciones a la vez aunque las pidan dos pantallas.
 */
class SincronizarUseCase(
    private val local: ProductoLocalRepository,
    private val remoto: ProductoRemotoRepository,
    private val reloj: Reloj,
) {
    private val candado = Mutex()

    suspend operator fun invoke(): ResumenSync = candado.withLock {
        var enviadas = 0
        var fallidas = 0
        var aviso: String? = null
        var servidorInalcanzable = false

        for (fila in local.pendientes(MAX_INTENTOS)) {
            when (val r = remoto.enviar(fila)) {
                is ResultadoEnvio.Exito -> {
                    local.confirmar(fila.id, r.producto); enviadas++
                }
                is ResultadoEnvio.Permanente -> {
                    local.marcarFallida(fila.id, r.motivo, MAX_INTENTOS); fallidas++
                }
                is ResultadoEnvio.Transitorio -> {
                    local.sumarIntento(fila.id, r.motivo)
                    aviso = r.motivo
                    if (r.sinRed) {
                        servidorInalcanzable = true
                        break
                    }
                    delay(espera(fila.intentos))
                }
                ResultadoEnvio.SesionVencida ->
                    // La sesión ya se cerró: no se toca la base de ese usuario.
                    return@withLock ResumenSync(enviadas, fallidas, 0, descargado = false,
                        aviso = "Tu sesión terminó. Inicia sesión para enviar los cambios.", sesionVencida = true)
            }
        }

        // Solo después de subir se baja: invertirlo borra lo pendiente.
        var descargado = false
        if (!servidorInalcanzable) {
            try {
                local.conciliar(remoto.listar())
                local.registrarSync(reloj.ahora())
                descargado = true
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (_: ErrorSesion.Expirada) {
                return@withLock ResumenSync(enviadas, fallidas, 0, descargado = false,
                    aviso = "Tu sesión terminó. Inicia sesión para enviar los cambios.", sesionVencida = true)
            } catch (_: Exception) {
                aviso = aviso ?: "No se pudo descargar el inventario. Se muestran los datos guardados."
            }
        }
        ResumenSync(enviadas, fallidas, local.contarPendientes(MAX_INTENTOS), descargado, aviso)
    }

    /** 1 s, 2 s, 4 s, 8 s, 16 s: una red intermitente no castiga la batería. */
    internal fun espera(intentos: Int): Long = 1_000L shl intentos.coerceIn(0, 4)

    companion object {
        const val MAX_INTENTOS = 5
    }
}
