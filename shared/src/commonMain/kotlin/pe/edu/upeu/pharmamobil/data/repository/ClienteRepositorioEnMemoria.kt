package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository


class ClienteRepositorioEnMemoria : ClienteRepository {

    private val candado = Mutex()
    private val clientes = mutableListOf<Cliente>()
    private var siguienteId = 1L

    override suspend fun registrar(cliente: Cliente): Cliente {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val guardado = cliente.copy(id = siguienteId++)
            clientes.add(guardado)
            guardado
        }
    }

    override suspend fun listar(): List<Cliente> {
        delay(RETARDO_LISTADO_MS)
        return candado.withLock {
            clientes.toList()
        }
    }

    private companion object {
        const val RETARDO_REGISTRO_MS = 400L
        const val RETARDO_LISTADO_MS = 600L
    }
}
