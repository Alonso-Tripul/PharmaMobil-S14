package pe.edu.upeu.pharmamobil.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobil.data.network.GestorSesion
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

@Composable
fun SesionScreen(contenido: @Composable (Sesion, () -> Unit) -> Unit) {
    val repo: AuthRepository = koinInject()
    val gestor: GestorSesion = koinInject()
    val sesion by gestor.sesion.collectAsState()
    val aviso by gestor.aviso.collectAsState()
    val scope = rememberCoroutineScope()
    var iniciando by remember { mutableStateOf(true) }
    var ocupado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try { repo.sesionGuardada() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "No se pudo recuperar la sesión. Comprueba la conexión e inicia sesión." }
        finally { iniciando = false }
    }
    if (iniciando) {
        PharmaMobilTheme { Box(Modifier.fillMaxSize().padding(24.dp)) { CircularProgressIndicator() } }
        return
    }
    val actual = sesion
    if (actual != null) {
        LaunchedEffect(actual.token) {
            try {
                val restantes = segundosRestantes(actual.token)
                delay(restantes.coerceAtMost(Long.MAX_VALUE / 1000) * 1000)
                gestor.cerrar("Tu sesión terminó. Vuelve a iniciar sesión.")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { gestor.cerrar("La sesión no es válida. Inicia sesión otra vez.") }
        }
        key(actual.token) {
            contenido(actual) {
                scope.launch {
                    try { repo.cerrarSesion(); clave = ""; error = null }
                    catch (_: Exception) { error = "No se pudo cerrar la sesión. Inténtalo otra vez." }
                }
            }
        }
    } else PharmaMobilTheme {
        LoginPanel(usuario = usuario, clave = clave, ocupado = ocupado, mensaje = error ?: aviso,
            onUsuario = { usuario = it }, onClave = { clave = it }, onIngresar = {
                ocupado = true; error = null
                scope.launch {
                    try { repo.iniciarSesion(Credenciales(usuario, clave)); clave = "" }
                    catch (e: CancellationException) { throw e }
                    catch (e: ErrorSesion) { error = e.message }
                    catch (_: Exception) { error = "No se pudo conectar al servidor. Comprueba que PharmaSoft esté ejecutándose." }
                    finally { ocupado = false }
                }
            })
    }
}
