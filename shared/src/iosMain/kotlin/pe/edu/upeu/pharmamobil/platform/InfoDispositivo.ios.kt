package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
actual class InfoDispositivo actual constructor() {
    actual val sistema: String
        get() = UIDevice.currentDevice.systemName
    actual val version: String
        get() = UIDevice.currentDevice.systemVersion
}
