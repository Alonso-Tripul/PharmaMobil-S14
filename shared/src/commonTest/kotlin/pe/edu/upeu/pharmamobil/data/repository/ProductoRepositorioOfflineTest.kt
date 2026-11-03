package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOfflineTest {
    private val muestra = Producto(0, "Ibuprofeno", 4.5, 20)

    @Test
    fun elAltaQuedaPendienteYVisibleSinTocarLaRed() = runTest {
        val local = AlmacenEnMemoria()
        val remoto = ServidorQueNoDebeLlamarse()
        val repo = ProductoRepositorioOffline(local, remoto)

        val guardado = repo.registrar(muestra)

        assertEquals(-1L, guardado.id)
        assertEquals(listOf(guardado), repo.observar().first())
        assertEquals(listOf(guardado), local.pendientes(5).map { it.producto })
        assertTrue(remoto.llamadas.isEmpty())
    }

    @Test
    fun noSePuedeEditarUnPendienteSinSincronizar() = runTest {
        val local = AlmacenEnMemoria()
        val repo = ProductoRepositorioOffline(local, ServidorQueNoDebeLlamarse())
        val guardado = repo.registrar(muestra)

        assertFailsWith<IllegalArgumentException> { repo.actualizar(guardado.copy(stock = 3)) }
    }

    private class ServidorQueNoDebeLlamarse : ProductoRepository {
        val llamadas = mutableListOf<String>()
        override suspend fun registrar(producto: Producto): Producto {
            llamadas += "registrar"; error("El alta offline no debe llamar a la red")
        }
        override suspend fun listar(): List<Producto> {
            llamadas += "listar"; error("El alta offline no debe llamar a la red")
        }
    }
}
