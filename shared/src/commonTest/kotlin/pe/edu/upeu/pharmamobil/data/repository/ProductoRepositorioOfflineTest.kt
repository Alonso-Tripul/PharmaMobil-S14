package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobil.data.local.AlmacenProductos
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOfflineTest {
    private val muestra = Producto(0, "Ibuprofeno", 4.5, 20)

    @Test
    fun sinRedElAltaQuedaPendienteYEsVisible() = runTest {
        val local = Memoria()
        val remoto = Servidor(online = false)
        val repo = ProductoRepositorioOffline(local, remoto)

        val guardado = repo.registrar(muestra)

        assertEquals(-1L, guardado.id)
        assertEquals(listOf(guardado), repo.observar().first())
        assertEquals(listOf(guardado), local.pendientes())
    }

    @Test
    fun alReconectarSubeAntesDeBajarYAsignaIdReal() = runTest {
        val local = Memoria()
        val remoto = Servidor(online = false)
        val repo = ProductoRepositorioOffline(local, remoto)
        repo.registrar(muestra)

        remoto.online = true
        repo.sincronizar()

        assertEquals(listOf("registrar", "listar"), remoto.llamadas.takeLast(2))
        assertEquals(1, repo.observar().first().size)
        assertTrue(repo.observar().first().single().id > 0)
        assertTrue(local.pendientes().isEmpty())
    }

    @Test
    fun siFallaLaSubidaLaDescargaNoBorraElPendiente() = runTest {
        val local = Memoria()
        local.guardarSincronizado(muestra.copy(id = 3))
        val remoto = Servidor(online = false)
        val repo = ProductoRepositorioOffline(local, remoto)
        repo.registrar(muestra)

        assertFailsWith<IllegalStateException> { repo.sincronizar() }

        assertEquals(1, local.pendientes().size)
        assertEquals(2, repo.observar().first().size)
        assertTrue("listar" !in remoto.llamadas)
    }

    private class Memoria : AlmacenProductos {
        private val filas = MutableStateFlow<List<Producto>>(emptyList())
        private val pendientes = mutableSetOf<Long>()
        override fun observar(): Flow<List<Producto>> = filas
        override fun listar(): List<Producto> = filas.value
        override fun buscar(id: Long): Producto? = filas.value.firstOrNull { it.id == id }
        override fun pendientes(): List<Producto> = filas.value.filter { it.id in pendientes }
        override fun guardarPendiente(producto: Producto): Producto {
            val id = minOf(filas.value.minOfOrNull { it.id } ?: 0L, 0L) - 1
            val guardado = producto.copy(id = id)
            pendientes.add(id)
            filas.value = filas.value + guardado
            return guardado
        }
        override fun confirmar(idTemporal: Long, producto: Producto) {
            pendientes.remove(idTemporal)
            filas.value = filas.value.filterNot { it.id == idTemporal } + producto
        }
        override fun guardarSincronizado(producto: Producto) {
            filas.value = filas.value.filterNot { it.id == producto.id } + producto
        }
        override fun borrar(id: Long) {
            pendientes.remove(id)
            filas.value = filas.value.filterNot { it.id == id }
        }
        override fun reemplazarSincronizados(productos: List<Producto>) {
            filas.value = filas.value.filter { it.id in pendientes } + productos
        }
    }

    private class Servidor(var online: Boolean) : ProductoRepository {
        val llamadas = mutableListOf<String>()
        private val productos = mutableListOf<Producto>()
        override suspend fun registrar(producto: Producto): Producto {
            llamadas += "registrar"
            check(online) { "Sin conexión" }
            return producto.copy(id = (productos.size + 1).toLong()).also { productos += it }
        }
        override suspend fun listar(): List<Producto> {
            llamadas += "listar"
            check(online) { "Sin conexión" }
            return productos.toList()
        }
    }
}
