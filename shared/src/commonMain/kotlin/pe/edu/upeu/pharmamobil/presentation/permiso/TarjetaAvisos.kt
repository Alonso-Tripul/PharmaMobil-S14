package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso

/**
 * Explica antes de pedir y, si el usuario denegó para siempre, ofrece Ajustes:
 * el sistema ya no mostrará el diálogo.
 */
@Composable
fun TarjetaAvisos(viewModel: PermisoViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Al volver de Ajustes el estado pudo cambiar.
    LifecycleResumeEffect(Unit) {
        viewModel.refrescar()
        onPauseOrDispose { }
    }

    val actual = estado ?: return
    OutlinedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    when (actual) {
                        EstadoPermiso.Concedido -> Icons.Default.NotificationsActive
                        EstadoPermiso.Denegado -> Icons.Default.NotificationsNone
                        EstadoPermiso.DenegadoParaSiempre -> Icons.Default.NotificationsOff
                    },
                    contentDescription = null,
                    tint = if (actual == EstadoPermiso.DenegadoParaSiempre) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    when (actual) {
                        EstadoPermiso.Concedido -> "Avisos activados"
                        EstadoPermiso.Denegado -> "¿Te avisamos si algo no se envía?"
                        EstadoPermiso.DenegadoParaSiempre -> "Los avisos están bloqueados"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                when (actual) {
                    EstadoPermiso.Concedido ->
                        "Te avisaremos cuando un producto registrado sin conexión no se pueda enviar a PharmaSoft."
                    EstadoPermiso.Denegado ->
                        "PharmaMobil usa las notificaciones solo para avisarte cuando un producto que registraste " +
                            "sin conexión no se pudo enviar al servidor. No enviamos publicidad."
                    EstadoPermiso.DenegadoParaSiempre ->
                        "Rechazaste los avisos y el sistema ya no volverá a preguntar. Puedes activarlos en " +
                            "Ajustes → Notificaciones."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (actual) {
                EstadoPermiso.Concedido -> Unit
                EstadoPermiso.Denegado -> Button(onClick = viewModel::solicitar) { Text("Permitir avisos") }
                EstadoPermiso.DenegadoParaSiempre ->
                    OutlinedButton(onClick = viewModel::abrirAjustes) { Text("Abrir Ajustes") }
            }
        }
    }
}
