package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.*
interface AuthRepository {
    suspend fun iniciarSesion(credenciales: Credenciales): Sesion
    suspend fun sesionGuardada(): Sesion?
    suspend fun cerrarSesion()
    suspend fun comprobarClientes()
}
