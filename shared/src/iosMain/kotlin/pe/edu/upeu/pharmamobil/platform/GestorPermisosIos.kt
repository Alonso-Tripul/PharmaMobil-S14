package pe.edu.upeu.pharmamobil.platform

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

/**
 * En iOS el sistema pregunta una sola vez: tras un «No permitir» el estado
 * pasa a Denied y solo queda Ajustes, así que no existe el Denegado intermedio
 * de Android. Las notificaciones no requieren clave en Info.plist; CAMARA y
 * UBICACION sí (NSCameraUsageDescription, NSLocationWhenInUseUsageDescription)
 * y aún no se usan en la app.
 */
class GestorPermisosIos : GestorPermisos {
    private val centro get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        if (permiso != Permiso.NOTIFICACIONES) return EstadoPermiso.Denegado
        return suspendCancellableCoroutine { cont ->
            centro.getNotificationSettingsWithCompletionHandler { ajustes ->
                val estado = when (ajustes?.authorizationStatus) {
                    UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral -> EstadoPermiso.Concedido
                    UNAuthorizationStatusDenied -> EstadoPermiso.DenegadoParaSiempre
                    else -> EstadoPermiso.Denegado // NotDetermined: aún se puede pedir
                }
                cont.resume(estado)
            }
        }
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        if (permiso != Permiso.NOTIFICACIONES) return estado(permiso)
        suspendCancellableCoroutine { cont ->
            centro.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { _, _ -> cont.resume(Unit) }
        }
        return estado(permiso)
    }

    override fun abrirAjustes() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
    }
}
