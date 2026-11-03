package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            local.conciliar(listOf(Producto(7, "Remoto", 3.0, 9)))

            assertTrue(pendiente.id < 0)
            assertEquals(2, local.observar().first().size)
            assertEquals(listOf(pendiente), local.pendientes(5).map { it.producto })

            local.confirmar(pendiente.id, pendiente.copy(id = 8))
            assertTrue(local.pendientes(5).isEmpty())
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
            assertTrue(local.pendientes(5).isEmpty())
            usuario = "admin"
            assertEquals(1, local.pendientes(5).size)
        } finally {
            driver.close()
        }
    }

    @Test
    fun laColaRecuerdaIntentosYPasaAFallidasAlAgotarlos() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver), object : ProveedorUsuario {
                override fun usuarioActual() = "admin"
            })
            val fila = local.guardarPendiente(Producto(0, "Amoxicilina", 8.0, 30))
            val clave = local.pendientes(5).single().claveOperacion
            assertTrue(clave != null && clave.length == 36)

            local.sumarIntento(fila.id, "Servidor no disponible")
            val enCola = local.pendientes(5).single()
            assertEquals(1, enCola.intentos)
            assertEquals("Servidor no disponible", enCola.ultimoError)
            assertEquals(clave, enCola.claveOperacion)

            local.marcarFallida(fila.id, "Ya existe un producto con ese nombre", 5)
            assertEquals(0, local.contarPendientes(5))
            assertEquals(1, local.observarFallidas(5).first().size)
            assertEquals(1, local.listar().size)

            local.reintentarFallidas(5)
            assertEquals(1, local.observarPendientes(5).first())
            assertNull(local.pendientes(5).single().ultimoError)
        } finally {
            driver.close()
        }
    }

    @Test
    fun laUltimaSincronizacionSeGuardaPorUsuario() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            PharmaDb.Schema.create(driver)
            var usuario = "admin"
            val local = ProductoLocal(PharmaDb(driver), object : ProveedorUsuario {
                override fun usuarioActual() = usuario
            })
            assertNull(local.observarUltimaSync().first())
            local.registrarSync(1_000L)
            assertEquals(1_000L, local.observarUltimaSync().first())
            usuario = "vendedor"
            assertNull(local.observarUltimaSync().first())
        } finally {
            driver.close()
        }
    }

    @Test
    fun laMigracionUnoConservaLasFilasDeLaVersionAnterior() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            // Esquema tal como lo dejó la sesión 11 (versión 1).
            driver.execute(null, """
                CREATE TABLE producto (propietario TEXT NOT NULL, id INTEGER NOT NULL, nombre TEXT NOT NULL,
                precio REAL NOT NULL, stock INTEGER NOT NULL, categoriaId INTEGER NOT NULL,
                estado INTEGER NOT NULL, pendiente INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(propietario, id))
            """.trimIndent(), 0)
            driver.execute(null, """
                CREATE TABLE sesion_cache (clave INTEGER NOT NULL PRIMARY KEY CHECK(clave = 1), usuario TEXT NOT NULL,
                nombre TEXT NOT NULL, permisos_json TEXT NOT NULL, roles_json TEXT NOT NULL)
            """.trimIndent(), 0)
            driver.execute(null, "INSERT INTO producto VALUES ('admin', -1, 'Viejo', 2.0, 3, 1, 1, 1)", 0)

            PharmaDb.Schema.migrate(driver, 1, PharmaDb.Schema.version)

            val local = ProductoLocal(PharmaDb(driver), object : ProveedorUsuario {
                override fun usuarioActual() = "admin"
            })
            val fila = local.pendientes(5).single()
            assertEquals("Viejo", fila.producto.nombre)
            assertEquals(0, fila.intentos)
        } finally {
            driver.close()
        }
    }
}
