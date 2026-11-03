package pe.edu.upeu.pharmamobil.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

@Composable
fun LoginPanel(usuario: String, clave: String, ocupado: Boolean, mensaje: String?,
    onUsuario: (String) -> Unit, onClave: (String) -> Unit, onIngresar: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val puedeIngresar = !ocupado && usuario.isNotBlank() && clave.isNotEmpty()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF073E37), Color(0xFF087C69))), RoundedCornerShape(26.dp)).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFCBF3E4)) {
                    Icon(Icons.Default.LocalPharmacy, null, Modifier.padding(14.dp).size(30.dp), tint = Color(0xFF075743))
                }
                Text("PharmaMobil", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Tu farmacia, organizada.", color = Color(0xFFD1F0E3), style = MaterialTheme.typography.bodyLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bienvenido", style = MaterialTheme.typography.headlineMedium)
                Text("Ingresa a tu espacio de trabajo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = usuario, onValueChange = onUsuario, label = { Text("Usuario") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.PersonOutline, null) }, enabled = !ocupado,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = clave, onValueChange = onClave, label = { Text("Contraseña") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = { IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            if (visible) "Ocultar contraseña" else "Mostrar contraseña")
                    } }, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), enabled = !ocupado,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (puedeIngresar) onIngresar() }),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                mensaje?.let {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Button(onClick = onIngresar, enabled = puedeIngresar, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    if (ocupado) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (ocupado) "Ingresando…" else "Iniciar sesión")
                    if (!ocupado) { Spacer(Modifier.width(10.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                Text("Las opciones disponibles corresponden al rol asignado a tu cuenta.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
