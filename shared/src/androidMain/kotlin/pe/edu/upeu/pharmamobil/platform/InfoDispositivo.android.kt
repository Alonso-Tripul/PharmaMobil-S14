package pe.edu.upeu.pharmamobil.platform

import android.os.Build

actual class InfoDispositivo actual constructor() {
    actual val sistema: String = "Android"
    actual val version: String
        get() = Build.VERSION.RELEASE
}
