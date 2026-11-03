package pe.edu.upeu.pharmamobil.presentation.acerca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.platform.InfoDispositivo

@Composable
fun AcercaDeScreen() {
    val dispositivo = remember { InfoDispositivo() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("PharmaMobil", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Gestión farmacéutica para tu cadena de boticas.",
            style = MaterialTheme.typography.bodyLarge
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Este dispositivo", style = MaterialTheme.typography.titleLarge)
                Text("Sistema operativo", style = MaterialTheme.typography.labelLarge)
                Text(dispositivo.sistema, style = MaterialTheme.typography.bodyLarge)
                Text("Versión del sistema", style = MaterialTheme.typography.labelLarge)
                Text(dispositivo.version, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
