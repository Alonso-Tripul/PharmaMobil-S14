package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.platform.MonitorRed
import pe.edu.upeu.pharmamobil.domain.platform.Reloj
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase.Companion.MAX_INTENTOS

/**
 * El estado se arma con consultas observables de la base local, nunca
 * preguntando a la red. La red solo decide cuándo vale la pena sincronizar.
 */
class SincronizacionViewModel(
    private val sincronizar: SincronizarUseCase,
    private val local: ProductoLocalRepository,
    red: MonitorRed,
    reloj: Reloj,
    private val notificador: Notificador,
) : ViewModel() {

    private val enCurso = MutableStateFlow(false)
    private val aviso = MutableStateFlow<String?>(null)

    /** Hace avanzar «Actualizado hace…» sin que nadie toque la pantalla. */
    private val ahora = flow {
        while (true) { emit(reloj.ahora()); delay(60_000) }
    }

    private val datosLocales = combine(
        local.observarPendientes(MAX_INTENTOS),
        local.observarFallidas(MAX_INTENTOS),
        local.observarUltimaSync(),
    ) { pend, fall, ultima -> Triple(pend, fall, ultima) }

    val estado: StateFlow<SincronizacionUiState> = combine(
        datosLocales, enCurso, red.conectado, aviso, ahora,
    ) { (pend, fall, ultima), curso, conectado, mensaje, momento ->
        SincronizacionUiState(
            enCurso = curso,
            pendientes = pend,
            fallidas = fall.size,
            ultimaSync = ultima,
            conectado = conectado,
            filasFallidas = fall.map {
                FilaFallidaUi(it.id, it.producto.nombre, it.ultimoError ?: "Sin detalle", it.intentos)
            },
            aviso = mensaje,
            ahora = momento,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SincronizacionUiState())

    init {
        // Sincroniza al abrir la pantalla, al volver la red y cuando la cola crece.
        viewModelScope.launch {
            var anterior: Pair<Boolean, Int>? = null
            combine(red.conectado, local.observarPendientes(MAX_INTENTOS)) { c, p -> c to p }
                .collect { actual ->
                    val previo = anterior
                    anterior = actual
                    val (conectado, pendientes) = actual
                    val debe = previo == null ||
                        (conectado && !previo.first) ||
                        pendientes > previo.second
                    if (conectado && debe) sincronizarAhora()
                }
        }
    }

    fun sincronizarAhora() {
        if (enCurso.value) return
        enCurso.value = true
        viewModelScope.launch {
            try {
                val resumen = sincronizar()
                if (resumen.fallidas > 0) {
                    notificador.avisar(
                        if (resumen.fallidas == 1) "1 producto no se envió" else "${resumen.fallidas} productos no se enviaron",
                        "PharmaSoft los rechazó. Abre Productos → «Revisar qué pasó» para corregirlos."
                    )
                }
                aviso.value = when {
                    resumen.sesionVencida -> resumen.aviso
                    !resumen.descargado -> resumen.aviso
                    resumen.aviso != null && resumen.pendientes > 0 -> resumen.aviso
                    else -> null
                }
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (_: Exception) {
                aviso.value = "No se pudo sincronizar. Se muestran los datos guardados."
            } finally {
                enCurso.value = false
            }
        }
    }

    /** «Reintentar ahora»: las fallidas vuelven a la cola con cero intentos. */
    fun reintentarFallidas() {
        local.reintentarFallidas(MAX_INTENTOS)
        sincronizarAhora()
    }

    /** El usuario decide no enviar una fila que el servidor rechaza. */
    fun descartar(id: Long) {
        local.descartar(id)
    }

    fun cerrarAviso() {
        aviso.value = null
    }
}
