package pe.edu.upeu.pharmamobil.platform

import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/** Si el usuario no autorizó, iOS descarta la solicitud sin error. */
class NotificadorIos : Notificador {
    override fun avisar(titulo: String, texto: String) {
        val contenido = UNMutableNotificationContent().apply {
            title = titulo
            body = texto
        }
        val solicitud = UNNotificationRequest.requestWithIdentifier("sincronizacion", contenido, null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(solicitud, null)
    }
}
