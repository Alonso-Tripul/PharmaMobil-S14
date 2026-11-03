package pe.edu.upeu.pharmamobil.platform

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

@Suppress("DEPRECATION")
class AlmacenSeguroAndroid(context: Context) : AlmacenSeguro {
    private val prefs = EncryptedSharedPreferences.create(context, "pharma_sesion",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    override fun guardarToken(token: String) { check(prefs.edit().putString("token", token).commit()) }
    override fun leerToken(): String? = prefs.getString("token", null)
    override fun borrarToken() { check(prefs.edit().remove("token").commit()) }
}
