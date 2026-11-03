package pe.edu.upeu.pharmamobil.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

/**
 * shouldShowRequestPermissionRationale devuelve false en dos casos distintos:
 * antes de la primera solicitud y después de «No volver a preguntar». Para
 * distinguirlos se recuerda en preferencias si el permiso ya se pidió.
 *
 * | Situación               | ¿Se pidió? | Explicar | Estado              |
 * |-------------------------|------------|----------|---------------------|
 * | Primera vez             | No         | No       | Denegado            |
 * | Denegó una vez          | Sí         | Sí       | Denegado            |
 * | Denegó definitivamente  | Sí         | No       | DenegadoParaSiempre |
 */
class GestorPermisosAndroid(
    private val contexto: Context,
    private val preferencias: SharedPreferences,
) : GestorPermisos {

    /** La Activity registra aquí su lanzador en onCreate. */
    var puente: PuentePermisos? = null

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        val nombre = nombreDeSistema(permiso) ?: return EstadoPermiso.Concedido
        val concedido = ContextCompat.checkSelfPermission(contexto, nombre) ==
            PackageManager.PERMISSION_GRANTED
        if (concedido) return EstadoPermiso.Concedido
        val actividad = puente?.actividad ?: return EstadoPermiso.Denegado
        val yaSePidio = preferencias.getBoolean("pedido_$nombre", false)
        val procedeExplicar = ActivityCompat.shouldShowRequestPermissionRationale(actividad, nombre)
        return when {
            !yaSePidio -> EstadoPermiso.Denegado
            procedeExplicar -> EstadoPermiso.Denegado
            else -> EstadoPermiso.DenegadoParaSiempre
        }
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        val nombre = nombreDeSistema(permiso) ?: return EstadoPermiso.Concedido
        preferencias.edit().putBoolean("pedido_$nombre", true).apply()
        val concedido = puente?.pedir(nombre) ?: false
        return if (concedido) EstadoPermiso.Concedido else estado(permiso)
    }

    override fun abrirAjustes() {
        val intento = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", contexto.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contexto.startActivity(intento)
    }

    /**
     * null significa que en esta versión de Android no hace falta pedirlo.
     * CAMARA y UBICACION quedan mapeados pero no declarados en el manifiesto:
     * pedir un permiso no declarado se rechaza sin mostrar diálogo.
     */
    private fun nombreDeSistema(permiso: Permiso): String? = when (permiso) {
        Permiso.CAMARA -> Manifest.permission.CAMERA
        Permiso.UBICACION -> Manifest.permission.ACCESS_FINE_LOCATION
        Permiso.NOTIFICACIONES ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS
            else null
    }
}
