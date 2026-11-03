package pe.edu.upeu.pharmamobil.notificacion

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pe.edu.upeu.pharmamobil.MainActivity
import pe.edu.upeu.pharmamobil.domain.port.Notificador

class NotificadorAndroid(private val contexto: Context) : Notificador {

    override fun avisar(titulo: String, texto: String) {
        if (!puedeNotificar()) return
        crearCanal()
        val abrir = PendingIntent.getActivity(
            contexto, 0,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(contexto).notify(ID_SINCRONIZACION, aviso)
        } catch (_: SecurityException) {
            // El usuario revocó el permiso entre la comprobación y el envío.
        }
    }

    private fun puedeNotificar(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(contexto).areNotificationsEnabled()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Sincronización", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Productos que no se pudieron enviar a PharmaSoft" }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private companion object {
        const val CANAL = "sincronizacion"
        const val ID_SINCRONIZACION = 1301
    }
}
