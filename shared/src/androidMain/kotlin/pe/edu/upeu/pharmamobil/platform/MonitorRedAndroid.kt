package pe.edu.upeu.pharmamobil.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed

/** Requiere ACCESS_NETWORK_STATE (permiso normal: no se pide al usuario). */
class MonitorRedAndroid(contexto: Context) : MonitorRed {
    private val conectividad = contexto.getSystemService(ConnectivityManager::class.java)

    override val conectado: Flow<Boolean> = callbackFlow {
        fun hayInternet(): Boolean = conectividad.getNetworkCapabilities(conectividad.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                trySend(capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        trySend(hayInternet())
        conectividad.registerDefaultNetworkCallback(escucha)
        awaitClose { conectividad.unregisterNetworkCallback(escucha) }
    }.distinctUntilChanged()
}
