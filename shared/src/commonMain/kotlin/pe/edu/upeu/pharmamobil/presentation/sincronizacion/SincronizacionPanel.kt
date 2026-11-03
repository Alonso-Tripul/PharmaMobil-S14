package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Responde sin abrir ningún menú: si hay conexión, cuántos cambios faltan y
 * de cuándo son los datos. Cuando hay fallidas, ofrece una acción.
 */
@Composable
fun SincronizacionPanel(
    estado: SincronizacionUiState,
    onSincronizar: () -> Unit,
    onReintentarFallidas: () -> Unit,
    onDescartar: (Long) -> Unit,
    onCerrarAviso: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revisando by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!estado.conectado) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (estado.enCurso) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = when {
                            !estado.conectado -> Icons.Default.CloudOff
                            estado.pendientes > 0 -> Icons.Default.CloudUpload
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(estado.titulo, style = MaterialTheme.typography.titleSmall)
                    Text(
                        estado.antiguedad,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onSincronizar, enabled = !estado.enCurso) {
                    Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                    Text("Sincronizar", Modifier.padding(start = 6.dp))
                }
            }

            estado.aviso?.let { aviso ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(aviso, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onCerrarAviso) { Text("Entendido") }
                }
            }

            estado.textoFallidas?.let { texto ->
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Text(texto, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onReintentarFallidas, enabled = !estado.enCurso) { Text("Reintentar ahora") }
                    TextButton(onClick = { revisando = true }) { Text("Revisar qué pasó") }
                }
            }
        }
    }

    if (revisando) {
        AlertDialog(
            onDismissRequest = { revisando = false },
            icon = { Icon(Icons.Default.ErrorOutline, null) },
            title = { Text("Productos no enviados") },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (estado.filasFallidas.isEmpty()) Text("No quedan productos con error.")
                    estado.filasFallidas.forEach { fila ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(fila.nombre, style = MaterialTheme.typography.titleSmall)
                            Text("Motivo: ${fila.motivo}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { onDescartar(fila.id) }) { Text("Descartar este producto") }
                        }
                        HorizontalDivider()
                    }
                    Text(
                        "Si el motivo es un dato inválido o un nombre repetido, descarta el producto y regístralo otra vez corregido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { revisando = false }) { Text("Cerrar") } }
        )
    }
}
