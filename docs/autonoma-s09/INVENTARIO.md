# Inventario de capacidades nativas - Actividad Autónoma 09

Autor: Rony Alonso Ancajima Tripul. Fecha: 06/10/2026.

La revisión identifica tres declaraciones `expect`: una función, una propiedad y una clase. `Compartidor` es una interfaz común ordinaria; se incluye como capacidad nativa, pero no se presenta como una declaración `expect` que no existe en el código.

| Capacidad | Contrato común | Android | iOS |
|---|---|---|---|
| Moneda peruana | `expect fun formatearSoles(valor: Double): String` | `java.text.NumberFormat.getCurrencyInstance(Locale("es", "PE"))` | Foundation `NSNumberFormatter`, `NSNumberFormatterCurrencyStyle`, `NSLocale("es_PE")` |
| Compartir producto | `interface Compartidor { fun compartir(texto: String) }` | `Context`, `Intent.ACTION_SEND`, `EXTRA_TEXT`, `Intent.createChooser`, `FLAG_ACTIVITY_NEW_TASK` | UIKit `UIActivityViewController`, ventana y controlador activos; cola principal de Dispatch |
| Inyección por plataforma | `expect val platformModule: Module` | Koin `module`, `androidContext()`, Ktor `HttpClient(Android)` | Koin `module`, `CompartidorIos()`, Ktor `HttpClient(Darwin)` |
| Información del dispositivo | `expect class InfoDispositivo() { val sistema: String; val version: String }` | `"Android"` y `Build.VERSION.RELEASE` | `UIDevice.currentDevice.systemName` y `UIDevice.currentDevice.systemVersion` |

## Archivos y source sets

### Moneda
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.kt`.
- actual Android, androidMain: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.android.kt`.
- actual iOS, iosMain: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.ios.kt`.

### Compartir
- Interfaz, commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/platform/Compartidor.kt`.
- Implementación ordinaria Android, androidMain: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorAndroid.kt`.
- Implementación ordinaria iOS, iosMain: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorIos.kt`.
- Las dos clases implementan la interfaz. La selección ocurre mediante los dos `actual` de `platformModule`, no mediante `actual class Compartidor`.

### Inyección
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`.
- actual Android, androidMain: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt`.
- actual iOS, iosMain: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.ios.kt`.
- Koin es una biblioteca compartida. Las dependencias nativas que selecciona el módulo son el motor HTTP y el servicio de compartir.

### Información del dispositivo
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.kt`.
- actual Android, androidMain: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.android.kt`.
- actual iOS, iosMain: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.ios.kt`.
- UI común: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/acerca/AcercaDeScreen.kt`.
- Menú y navegación: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/App.kt` y `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/navigation/Screen.kt`.

## Aislamiento y ejecución
Se revisó todo `shared/src/commonMain`: no se encontraron importaciones que comiencen por `android.` o `platform.`. Android compiló y mostró sistema **Android**, versión **14**, en Pixel 6 API 34. Los archivos iOS están implementados, pero su compilación y ejecución aún no están acreditadas con evidencia real.
