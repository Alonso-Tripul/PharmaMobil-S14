package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.SincronizacionPanel
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.SincronizacionViewModel

@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    modifier: Modifier = Modifier,
    claveSesion: String = "default",
    puedeEscribir: Boolean = true,
    sincronizacion: SincronizacionViewModel = koinViewModel(key = "sync-$claveSesion")
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val estadoSync by sincronizacion.estado.collectAsStateWithLifecycle()
    val idsFallidos = estadoSync.filasFallidas.map { it.id }.toSet()
    var seleccionado by rememberSaveable { mutableStateOf<Long?>(null) }
    seleccionado?.let { id ->
        DetalleProductoScreen(
            id = id,
            viewModel = koinViewModel(key = "detalle-$claveSesion-$id"),
            onVolver = { seleccionado = null },
            modifier = modifier,
            puedeEscribir = puedeEscribir
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (puedeEscribir) FormularioProductoCard(
            formulario = uiState.formulario,
            registrando = uiState.registrando,
            onNombreChange = viewModel::onNombreChange,
            onPrecioChange = viewModel::onPrecioChange,
            onStockChange = viewModel::onStockChange,
            onCategoriaChange = viewModel::onCategoriaChange,
            onRegistrar = viewModel::registrar
        )

        uiState.mensajeExito?.let {
            MensajeExito(it)
        }

        SincronizacionPanel(
            estado = estadoSync,
            onSincronizar = sincronizacion::sincronizarAhora,
            onReintentarFallidas = sincronizacion::reintentarFallidas,
            onDescartar = sincronizacion::descartar,
            onCerrarAviso = sincronizacion::cerrarAviso
        )

        EncabezadoInventario(uiState.fase)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            when (val fase = uiState.fase) {

                ProductoUiState.Fase.Cargando ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        CircularProgressIndicator()

                        Text(
                            text = "Cargando inventario…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                ProductoUiState.Fase.SinProductos ->
                    EstadoVacio(
                        icono = Icons.Default.Inventory2,
                        titulo = "Todavía no hay productos",
                        descripcion = "Registra el primero con el formulario de arriba.",
                        modifier = Modifier.align(Alignment.Center)
                    )

                is ProductoUiState.Fase.ConProductos ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = fase.productos,
                            key = { it.id }
                        ) { producto ->
                            ProductoItem(
                                producto,
                                noEnviado = producto.id in idsFallidos,
                                onAbrir = { seleccionado = producto.id }
                            )
                        }
                    }

                is ProductoUiState.Fase.Error ->
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "No pudimos cargar el inventario",
                        descripcion = fase.mensaje,
                        colorIcono = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                        accion = {
                            FilledTonalButton(onClick = sincronizacion::sincronizarAhora) {
                                Text("Reintentar")
                            }
                        }
                    )
            }
        }
    }
}


@Composable
private fun FormularioProductoCard(
    formulario: FormularioProducto,
    registrando: Boolean,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onRegistrar: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Registrar producto",
                style = MaterialTheme.typography.titleMedium
            )

            ValidatedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChange,
                label = "Nombre",
                error = formulario.nombreError,
                leadingIcon = Icons.Default.Medication,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ValidatedTextField(
                    value = formulario.precio,
                    onValueChange = onPrecioChange,
                    label = "Precio",
                    error = formulario.precioError,
                    ayuda = "En soles",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                ValidatedTextField(
                    value = formulario.stock,
                    onValueChange = onStockChange,
                    label = "Stock",
                    error = formulario.stockError,
                    ayuda = "Unidades",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            ValidatedTextField(
                value = formulario.categoria,
                onValueChange = onCategoriaChange,
                label = "ID de categoría",
                error = formulario.categoriaError,
                ayuda = "Usa una categoría existente en PharmaSoft",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onRegistrar,
                enabled = !registrando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (registrando) "Registrando…" else "Registrar")
            }
        }
    }
}


@Composable
private fun EncabezadoInventario(
    fase: ProductoUiState.Fase
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "Inventario",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        if (fase is ProductoUiState.Fase.ConProductos) {

            val cantidad = fase.productos.size

            Text(
                text = if (cantidad == 1) "1 producto" else "$cantidad productos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ProductoItem(
    producto: ProductoUi,
    noEnviado: Boolean,
    onAbrir: () -> Unit
) {

    Card(
        onClick = onAbrir,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {

                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "${producto.precio}  ·  ${producto.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!producto.activo) {
                Text("Inactivo", style = MaterialTheme.typography.labelSmall)
            }

            if (noEnviado) {
                Text("No enviado", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            } else if (producto.id < 0) {
                Text("Pendiente", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }

            if (producto.requiereReposicion) {

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {

                    Text(
                        text = "Reponer",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }
        }
    }
}
