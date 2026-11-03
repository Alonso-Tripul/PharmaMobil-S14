package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Las reglas que antes cubria ProductoValidatorTest ahora se verifican aqui,
 * que es donde vive la validacion desde esta sesion.
 *
 * Se prueba contra [FakeProductoRepository] y no contra el repositorio en
 * memoria: una prueba de dominio no debe depender de la capa de datos.
 */
class RegistrarProductoUseCaseTest {

    private fun nuevoCasoDeUso(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = RegistrarProductoUseCase(repositorio)

    private suspend fun erroresAlRegistrar(
        nombre: String = "Paracetamol",
        precio: String = "12.50",
        stock: String = "5"
    ): ErroresDeProducto {

        val resultado = nuevoCasoDeUso().invoke(nombre, precio, stock)
        val fallo = assertIs<ProductoInvalidoException>(resultado.exceptionOrNull())
        return fallo.errores
    }

    @Test
    fun aceptaUnPrecioValido() = runTest {

        val resultado = nuevoCasoDeUso().invoke("Paracetamol", "12.50", "5")

        assertNull(resultado.exceptionOrNull())
        assertEquals(12.50, resultado.getOrThrow().precio, 0.0001)
    }

    @Test
    fun rechazaPrecioNoNumerico() = runTest {
        assertEquals(
            "El precio debe ser un número válido",
            erroresAlRegistrar(precio = "abc").precio
        )
    }

    @Test
    fun rechazaNaNComoPrecio() = runTest {
        assertEquals(
            "El precio debe ser un número válido",
            erroresAlRegistrar(precio = "NaN").precio
        )
    }

    @Test
    fun rechazaInfinityComoPrecio() = runTest {
        assertEquals(
            "El precio debe ser un número válido",
            erroresAlRegistrar(precio = "Infinity").precio
        )
    }

    @Test
    fun rechazaPrecioCero() = runTest {
        assertEquals(
            "El precio debe ser mayor a 0",
            erroresAlRegistrar(precio = "0").precio
        )
    }

    @Test
    fun aceptaStockCero() = runTest {

        val resultado = nuevoCasoDeUso().invoke("Paracetamol", "12.50", "0")

        assertNull(resultado.exceptionOrNull())
    }

    @Test
    fun rechazaStockNegativo() = runTest {
        assertEquals(
            "El stock no puede ser negativo",
            erroresAlRegistrar(stock = "-1").stock
        )
    }

    @Test
    fun rechazaNombreVacio() = runTest {
        assertEquals(
            "El nombre es obligatorio",
            erroresAlRegistrar(nombre = "   ").nombre
        )
    }

    @Test
    fun reportaLosTresErroresALaVez() = runTest {

        val errores = erroresAlRegistrar(nombre = "", precio = "", stock = "")

        assertEquals("El nombre es obligatorio", errores.nombre)
        assertEquals("El precio es obligatorio", errores.precio)
        assertEquals("El stock es obligatorio", errores.stock)
    }

    @Test
    fun elIdLoAsignaElRepositorioNoLaPantalla() = runTest {

        val casoDeUso = nuevoCasoDeUso()

        val primero = casoDeUso.invoke("Paracetamol", "12.50", "5").getOrThrow()
        val segundo = casoDeUso.invoke("Ibuprofeno", "8.90", "3").getOrThrow()

        assertEquals(1L, primero.id)
        assertEquals(2L, segundo.id)
    }

    /** El camino que antes quedaba sin cubrir: el repositorio revienta. */
    @Test
    fun devuelveFailureCuandoElRepositorioFalla() = runTest {

        val repositorio = FakeProductoRepository().apply {
            fallaAlRegistrar = IllegalStateException("Sin conexión")
        }

        val resultado = nuevoCasoDeUso(repositorio)
            .invoke("Paracetamol", "12.50", "5")

        assertEquals("Sin conexión", resultado.exceptionOrNull()?.message)
    }
}
