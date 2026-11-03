package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

@Composable
fun DetalleProductoScreen(
    id: Long,
    viewModel: DetalleProductoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    puedeEscribir: Boolean = true
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmarBorrado by rememberSaveable(id) { mutableStateOf(false) }
    LaunchedEffect(id) { viewModel.cargar(id) }
    LaunchedEffect(estado.eliminado) { if (estado.eliminado) onVolver() }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onVolver, enabled = !estado.ocupado) { Text("Volver al listado") }
        Text("Detalle del producto", style = MaterialTheme.typography.headlineSmall)
        if (estado.cargando) {
            CircularProgressIndicator()
        }
        estado.producto?.let { producto ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(producto.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(producto.precio, style = MaterialTheme.typography.headlineSmall)
                    Text("Stock: ${producto.stock}")
                    Button(onClick = viewModel::compartir, enabled = !estado.ocupado) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir")
                    }
                }
            }
            if (puedeEscribir) {
            Text("Editar producto", style = MaterialTheme.typography.titleMedium)
            ValidatedTextField(estado.nombre, viewModel::onNombreChange, "Nombre", error = null, modifier = Modifier.fillMaxWidth())
            ValidatedTextField(estado.precio, viewModel::onPrecioChange, "Precio", error = null, keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
            ValidatedTextField(estado.stock, viewModel::onStockChange, "Stock", error = null, keyboardType = KeyboardType.Number, modifier = Modifier.fillMaxWidth())
            ValidatedTextField(estado.categoria, viewModel::onCategoriaChange, "ID de categoría", error = null, keyboardType = KeyboardType.Number, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Activo", modifier = Modifier.weight(1f))
                Switch(estado.activo, viewModel::onActivoChange, enabled = !estado.ocupado)
            }
            Button(onClick = viewModel::guardar, enabled = !estado.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (estado.ocupado) "Procesando…" else "Guardar cambios")
            }
            OutlinedButton(onClick = { confirmarBorrado = true }, enabled = !estado.ocupado && estado.producto?.activo == true, modifier = Modifier.fillMaxWidth()) {
                Text("Dar de baja")
            }
            }
        }
        estado.mensaje?.let { Text(it) }
        if (!estado.cargando && estado.producto == null && !estado.eliminado) {
            Button(onClick = { viewModel.cargar(id) }) { Text("Reintentar") }
        }
    }
    if (confirmarBorrado && puedeEscribir) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("Dar de baja") },
            text = { Text("El producto quedará inactivo y seguirá visible en el historial de PharmaSoft.") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; viewModel.eliminar() }) { Text("Dar de baja") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") }
            }
        )
    }
}
