package pe.edu.upeu.pharmamobil.platform

import pe.edu.upeu.pharmamobil.domain.platform.Reloj

expect fun ahoraEpochMs(): Long

object RelojSistema : Reloj {
    override fun ahora(): Long = ahoraEpochMs()
}
