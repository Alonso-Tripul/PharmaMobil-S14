package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.Sesion

class ProductoLocalSqlTest {
    @Test
    fun sesionCacheGuardaMetadatosPeroNoElToken() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            PharmaDb.Schema.create(driver)
            val base = PharmaDb(driver)
            val cache = SesionLocal(base)
            cache.guardar(Sesion("jwt-privado", "admin", "Administrador", setOf("CATALOGO_LEER"), setOf("ADMIN")))

            val restaurada = cache.leer("jwt-desde-almacen-seguro")
            assertEquals("jwt-desde-almacen-seguro", restaurada?.token)
            assertEquals(setOf("CATALOGO_LEER"), restaurada?.permisos)
            assertEquals(setOf("ADMIN"), restaurada?.roles)
        } finally {
            driver.close()
        }
    }
    @Test
    fun reemplazarServidorConservaPendientesYEmiteLaLista() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver), object : ProveedorUsuario {
                override fun usuarioActual() = "admin"
            })
            val pendiente = local.guardarPendiente(Producto(0, "Local", 2.0, 5))
            local.reemplazarSincronizados(listOf(Producto(7, "Remoto", 3.0, 9)))

            assertTrue(pendiente.id < 0)
            assertEquals(2, local.observar().first().size)
            assertEquals(listOf(pendiente), local.pendientes())

            local.confirmar(pendiente.id, pendiente.copy(id = 8))
            assertTrue(local.pendientes().isEmpty())
            assertEquals(setOf(7L, 8L), local.listar().map { it.id }.toSet())
        } finally {
            driver.close()
        }
    }

    @Test
    fun usuariosDistintosNoVenLosPendientesDelOtro() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            PharmaDb.Schema.create(driver)
            var usuario = "admin"
            val local = ProductoLocal(PharmaDb(driver), object : ProveedorUsuario {
                override fun usuarioActual() = usuario
            })
            local.guardarPendiente(Producto(0, "Producto privado", 2.0, 5))
            usuario = "vendedor"
            assertTrue(local.listar().isEmpty())
            assertTrue(local.pendientes().isEmpty())
            usuario = "admin"
            assertEquals(1, local.pendientes().size)
        } finally {
            driver.close()
        }
    }
}
