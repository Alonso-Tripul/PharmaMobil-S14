package pe.edu.upeu.pharmamobil.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * El "No registrado" del telefono ausente ya no se prueba aqui: era texto de
 * pantalla dentro de un modelo de dominio y ahora vive en ClienteUi. Lo que
 * si le corresponde al modelo son sus invariantes.
 */
class ClienteTest {

    @Test
    fun aceptaUnClienteConTelefono() {

        val cliente = Cliente(
            id = 1L,
            nombre = "Farmacia Nueva Vida",
            correo = "ventas@central.pe",
            telefono = "989789123"
        )

        assertEquals("989789123", cliente.telefono)
    }

    @Test
    fun elTelefonoPuedeFaltar() {

        val cliente = Cliente(
            id = 1L,
            nombre = "Farmacia Nueva Vida",
            correo = "ventas@central.pe",
            telefono = null
        )

        assertNull(cliente.telefono)
    }

    @Test
    fun noSeConstruyeSinNombre() {

        assertFailsWith<IllegalArgumentException> {
            Cliente(
                id = 1L,
                nombre = "   ",
                correo = "ventas@central.pe",
                telefono = null
            )
        }
    }

    @Test
    fun noSeConstruyeConTelefonoVacio() {

        assertFailsWith<IllegalArgumentException> {
            Cliente(
                id = 1L,
                nombre = "Farmacia Nueva Vida",
                correo = "ventas@central.pe",
                telefono = ""
            )
        }
    }
}
