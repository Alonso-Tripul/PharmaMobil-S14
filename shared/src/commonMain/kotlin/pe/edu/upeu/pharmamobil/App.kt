package pe.edu.upeu.pharmamobil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.presentation.acerca.AcercaDeScreen
import pe.edu.upeu.pharmamobil.presentation.auth.SesionScreen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.permiso.TarjetaAvisos
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

private data class Opcion(val screen: Screen, val titulo: String, val descripcion: String, val icono: ImageVector)
private fun opciones(sesion: Sesion): List<Opcion> = buildList {
    add(Opcion(Screen.Inicio, "Inicio", "Tu espacio de trabajo", Icons.Default.Home))
    if (sesion.puede("CATALOGO_LEER")) add(Opcion(Screen.Productos, "Productos",
        if (sesion.puede("CATALOGO_ESCRIBIR")) "Consulta y gestiona el inventario" else "Consulta el catálogo disponible", Icons.Default.Medication))
    if (sesion.puede("CLIENTES_LEER")) add(Opcion(Screen.Clientes, "Clientes", "Consulta tu cartera de clientes", Icons.Default.People))
    add(Opcion(Screen.AcercaDe, "Acerca de", "Información de PharmaMobil", Icons.Default.Info))
}
private fun nombreRol(rol: String): String = when (rol) {
    "ADMIN" -> "Administrador"
    "VENDEDOR" -> "Vendedor"
    "ALMACENERO" -> "Almacenero"
    else -> rol.lowercase().replaceFirstChar { it.uppercase() }
}
private fun rolesDe(sesion: Sesion) = sesion.roles.joinToString(" · ", transform = ::nombreRol).ifBlank { "Sin rol asignado" }

@Composable
fun App() = KoinContext { SesionScreen { sesion, cerrar -> AppAutenticada(sesion, cerrar) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppAutenticada(sesion: Sesion, cerrarSesion: () -> Unit) {
    val destinos = remember(sesion) { opciones(sesion) }
    var pantalla by rememberSaveable { mutableStateOf("Inicio") }
    val actual = destinos.firstOrNull { it.titulo == pantalla } ?: destinos.first()
    var oscuro by rememberSaveable { mutableStateOf(false) }
    var perfil by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val repo: AuthRepository = koinInject()
    var comprobando by remember { mutableStateOf(false) }
    PharmaMobilTheme(darkTheme = oscuro) {
        ModalNavigationDrawer(drawerState = drawer, drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 340.dp)) {
                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF073E37), Color(0xFF087C69)))).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.LocalPharmacy, null, tint = Color.White)
                            Text("PharmaMobil", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        Avatar(sesion, Modifier.size(54.dp))
                        Text(sesion.nombreCompleto, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("@${sesion.usuario}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBCE9D9))
                        Surface(color = Color(0xFFCBF3E4), shape = RoundedCornerShape(50)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.VerifiedUser, null, Modifier.size(16.dp), tint = Color(0xFF075743))
                                Text(rolesDe(sesion), style = MaterialTheme.typography.labelMedium, color = Color(0xFF075743))
                            }
                        }
                    }
                    Text("ESPACIO DE TRABAJO", Modifier.padding(start = 24.dp, top = 22.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    destinos.forEach { destino ->
                        NavigationDrawerItem(label = { Text(destino.titulo) }, selected = actual == destino,
                            onClick = { pantalla = destino.titulo; scope.launch { drawer.close() } },
                            icon = { Icon(destino.icono, null) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                    }
                    HorizontalDivider(Modifier.padding(20.dp))
                    TextButton(onClick = { perfil = true }, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Icon(Icons.Default.AccountCircle, null); Spacer(Modifier.width(10.dp)); Text("Mi perfil y permisos")
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, null, Modifier.size(20.dp))
                        Text("Modo oscuro", Modifier.weight(1f).padding(start = 10.dp))
                        Switch(oscuro, { oscuro = it })
                    }
                    TextButton(onClick = { confirmarSalida = true }, modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Logout, null); Spacer(Modifier.width(10.dp)); Text("Cerrar sesión")
                    }
                }
            }
        }) {
            Scaffold(topBar = {
                TopAppBar(title = {
                    Column { Text(actual.titulo, style = MaterialTheme.typography.titleLarge)
                        Text(rolesDe(sesion), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Abrir menú") } },
                    actions = { IconButton(onClick = { perfil = true }) { Avatar(sesion, Modifier.size(34.dp)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
            }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (actual.screen) {
                        Screen.Inicio -> InicioProfesional(sesion, destinos.filter { it.screen != Screen.Inicio && it.screen != Screen.AcercaDe },
                            onNavegar = { pantalla = it.titulo }, onPerfil = { perfil = true })
                        Screen.Productos -> ProductoScreen(viewModel = koinViewModel(key = "productos-${sesion.token.hashCode()}"),
                            claveSesion = sesion.token.hashCode().toString(), puedeEscribir = sesion.puede("CATALOGO_ESCRIBIR"))
                        Screen.Clientes -> ClienteScreen(viewModel = koinViewModel(key = "clientes-${sesion.token.hashCode()}"))
                        Screen.AcercaDe -> AcercaDeScreen()
                        Screen.Pedidos -> Unit
                    }
                }
            }
        }
        if (perfil) AlertDialog(onDismissRequest = { perfil = false }, icon = { Icon(Icons.Default.VerifiedUser, null) },
            title = { Text("Mi perfil") }, text = {
                Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(sesion.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                    Text("Usuario: ${sesion.usuario}")
                    Text("Rol: ${rolesDe(sesion)}", color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    Text("Tus permisos", style = MaterialTheme.typography.titleSmall)
                    if (sesion.permisos.isEmpty()) Text("No tienes permisos asignados.")
                    sesion.permisos.sorted().forEach { permiso ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(describirPermiso(permiso), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedButton(enabled = !comprobando, onClick = {
                        comprobando = true
                        scope.launch {
                            val mensaje = try { repo.comprobarClientes(); "Tienes acceso a clientes." }
                            catch (e: CancellationException) { throw e }
                            catch (e: ErrorSesion) { e.message ?: "Acceso no disponible" }
                            catch (_: Exception) { "No se pudo conectar al servidor." }
                            finally { comprobando = false }
                            perfil = false; snackbar.showSnackbar(mensaje)
                        }
                    }) { Text(if (comprobando) "Comprobando…" else "Comprobar acceso a clientes") }
                }
            }, confirmButton = { TextButton(onClick = { perfil = false }) { Text("Listo") } })
        if (confirmarSalida) AlertDialog(onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") }, text = { Text("Volverás a la pantalla de acceso.") },
            confirmButton = { TextButton(onClick = { confirmarSalida = false; cerrarSesion() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } })
    }
}

@Composable
private fun Avatar(sesion: Sesion, modifier: Modifier = Modifier) {
    Surface(modifier, shape = CircleShape, color = Color(0xFFD6F3E8), contentColor = Color(0xFF075743)) {
        Box(contentAlignment = Alignment.Center) {
            Text(sesion.nombreCompleto.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(),
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InicioProfesional(sesion: Sesion, accesos: List<Opcion>, onNavegar: (Opcion) -> Unit, onPerfil: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF073E37), Color(0xFF087C69))), RoundedCornerShape(24.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TU FARMACIA, ORGANIZADA", color = Color(0xFFBCE9D9), style = MaterialTheme.typography.labelSmall)
            Text("Hola, ${sesion.nombreCompleto.substringBefore(' ')}", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("Todo lo que necesitas para tu jornada, en un solo lugar.", color = Color(0xFFE3F7EF), style = MaterialTheme.typography.bodyMedium)
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFCBF3E4)) {
                Text(rolesDe(sesion), Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = Color(0xFF075743), style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("Accesos disponibles", style = MaterialTheme.typography.titleLarge)
        accesos.forEach { opcion ->
            Card(onClick = { onNavegar(opcion) }, shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(opcion.icono, null, Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(opcion.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(opcion.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // Sesión 13: permiso nativo resuelto desde el código común.
        TarjetaAvisos(viewModel = koinViewModel(key = "avisos-${sesion.usuario}"))
        OutlinedCard(onClick = onPerfil, shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Mi perfil y permisos", style = MaterialTheme.typography.titleMedium)
                    Text("Conoce las opciones de tu cuenta", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
}
private fun describirPermiso(permiso: String): String = when (permiso) {
    "CATALOGO_LEER" -> "Consultar productos y categorías"
    "CATALOGO_ESCRIBIR" -> "Registrar, editar y dar de baja productos"
    "CLIENTES_LEER" -> "Consultar clientes"
    "CLIENTES_ESCRIBIR" -> "Registrar y actualizar clientes"
    "VENTAS_LEER" -> "Consultar ventas"
    "VENTAS_ESCRIBIR" -> "Registrar ventas"
    "REPORTES_LEER" -> "Consultar reportes"
    "SEGURIDAD_ADMIN" -> "Administrar usuarios y roles"
    else -> permiso.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
