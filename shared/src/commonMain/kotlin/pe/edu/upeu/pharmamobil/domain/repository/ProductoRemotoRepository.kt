package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.EnvioPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio

/** Lo que la sincronización necesita del servidor. */
interface ProductoRemotoRepository {
    /** Nunca lanza por un error HTTP: lo devuelve clasificado. */
    suspend fun enviar(fila: EnvioPendiente): ResultadoEnvio
    suspend fun listar(): List<Producto>
}
