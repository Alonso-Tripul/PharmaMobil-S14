package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Cliente
import kotlin.test.Test
import kotlin.test.assertEquals

class ClienteRepositorioEnMemoriaTest {

    private fun nuevoCliente(nombre: String) = Cliente(
        id = 0L,
        nombre = nombre,
        correo = "ventas@central.pe",
        telefono = "989789123"
    )

    @Test
    fun asignaIdCorrelativoAlRegistrar() = runTest {

        val repositorio = ClienteRepositorioEnMemoria()

        val primero = repositorio.registrar(nuevoCliente("Botica A"))
        val segundo = repositorio.registrar(nuevoCliente("Botica B"))

        assertEquals(1L, primero.id)
        assertEquals(2L, segundo.id)
    }

    @Test
    fun listarDevuelveLosClientesRegistrados() = runTest {

        val repositorio = ClienteRepositorioEnMemoria()
        repositorio.registrar(nuevoCliente("Botica A"))
        repositorio.registrar(nuevoCliente("Botica B"))

        val resultado = repositorio.listar()

        assertEquals(2, resultado.size)
        assertEquals("Botica A", resultado[0].nombre)
    }
}
