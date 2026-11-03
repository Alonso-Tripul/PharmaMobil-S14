package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.network.ErrorApi
import pe.edu.upeu.pharmamobil.data.network.aErrorApi
import pe.edu.upeu.pharmamobil.domain.model.EnvioPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRemotoRepository

/** Envía una fila de la cola y clasifica el resultado. */
class EnvioRepositorioRest(
    private val rest: ProductoRepositorioRest
) : ProductoRemotoRepository {

    override suspend fun enviar(fila: EnvioPendiente): ResultadoEnvio = try {
        // La clave de operación hace idempotente el reintento de una creación:
        // si el primer POST llegó y se perdió la respuesta, PharmaSoft devuelve
        // el producto ya creado en lugar de duplicarlo.
        ResultadoEnvio.Exito(rest.registrar(fila.producto, fila.claveOperacion))
    } catch (fallo: Throwable) {
        fallo.aErrorApi().aResultado()
    }

    override suspend fun listar(): List<Producto> = rest.listar()

    private fun ErrorApi.aResultado(): ResultadoEnvio = when (this) {
        is ErrorApi.SinConexion -> ResultadoEnvio.Transitorio(mensaje, sinRed = true)
        is ErrorApi.TiempoAgotado, is ErrorApi.Servidor -> ResultadoEnvio.Transitorio(mensaje)
        is ErrorApi.Validacion, is ErrorApi.NoEncontrado,
        is ErrorApi.Conflicto, is ErrorApi.SinPrivilegio -> ResultadoEnvio.Permanente(mensaje)
        is ErrorApi.NoAutorizado -> ResultadoEnvio.SesionVencida
    }
}
