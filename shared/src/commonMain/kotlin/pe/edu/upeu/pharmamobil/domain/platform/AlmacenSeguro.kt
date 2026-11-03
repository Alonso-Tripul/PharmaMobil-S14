package pe.edu.upeu.pharmamobil.domain.platform

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
}
