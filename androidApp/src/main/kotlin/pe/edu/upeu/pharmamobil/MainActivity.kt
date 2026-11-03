package pe.edu.upeu.pharmamobil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.android.ext.android.inject
import pe.edu.upeu.pharmamobil.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobil.permiso.PuentePermisos

class MainActivity : ComponentActivity() {
    private val gestorPermisos: GestorPermisosAndroid by inject()
    private var puente: PuentePermisos? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Debe registrarse antes de STARTED; al rotar se crea uno nuevo.
        puente = PuentePermisos(this).also { gestorPermisos.puente = it }

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        puente?.let { propio ->
            if (gestorPermisos.puente === propio) gestorPermisos.puente = null
            propio.liberar()
        }
        puente = null
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
