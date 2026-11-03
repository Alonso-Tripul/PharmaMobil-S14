package pe.edu.upeu.pharmamobil.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

/**
 * Única clase de la app que conoce el SDK de Firebase (GitLive 2.7.0).
 * Errores → Crashlytics como «no fatales» (aparecen tras reabrir la app).
 * Eventos → Analytics (en vivo en DebugView) y como rastro en Crashlytics.
 *
 * La telemetría nunca debe tumbar la app: cualquier fallo del SDK se ignora.
 */
class TelemetriaFirebase : Telemetria {

    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        try {
            Firebase.crashlytics.recordException(e, contexto)
        } catch (_: Throwable) {
        }
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        try {
            Firebase.analytics.logEvent(nombre, datos)
            Firebase.crashlytics.log("$nombre $datos")
        } catch (_: Throwable) {
        }
    }
}
