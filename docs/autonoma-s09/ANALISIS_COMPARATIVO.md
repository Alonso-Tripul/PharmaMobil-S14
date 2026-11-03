# Informe comparativo - Actividad Autónoma 09

Autor: Rony Alonso Ancajima Tripul. Fecha: 06/10/2026.

## 1. Formato de moneda y resultado observado

En PharmaMobil, `platform/Formato.kt` declara `formatearSoles(valor: Double): String`. Su actual Android utiliza `java.text.NumberFormat` con `Locale("es", "PE")`, mientras que el actual iOS utiliza `NSNumberFormatter`, estilo monetario y `NSLocale("es_PE")`. Las dos configuraciones representan español de Perú, pero pertenecen a bibliotecas y sistemas distintos. El locale expresa una preferencia regional; no obliga a que ambas implementaciones produzcan cadenas idénticas. Los datos regionales y las versiones del sistema pueden afectar los símbolos y espacios. Por ello, la equivalencia funcional consiste en expresar correctamente el precio en soles, no en imponer manualmente una cadena universal.

En el Pixel 6 con Android 14 observé el producto Paracetamol 500 mg con el precio literal `S/ 6.00` y stock 25. La captura de compartir conserva ese precio. No dispongo todavía del resultado literal de iOS: su código está escrito, pero no se ha acreditado una ejecución en simulador. Esta limitación impide afirmar que Foundation coloca o elimina un espacio concreto. La comparación literal debe completarse con el mismo producto y precio en iOS.

## 2. Context de Android y presentación en UIKit

En `platform/CompartidorAndroid.kt`, el constructor recibe `Context`. El método crea un `Intent.ACTION_SEND`, agrega el texto mediante `EXTRA_TEXT` y abre `Intent.createChooser`. El Context permite iniciar la actividad que presenta el selector. En `di/PlatformModule.android.kt`, Koin entrega `androidContext()`; al utilizar contexto de aplicación, el selector incorpora `FLAG_ACTIVITY_NEW_TASK`.

En cambio, `CompartidorIos.kt` no recibe un Context de Android. Busca una ventana activa de `UIApplication`, obtiene su controlador raíz y localiza el controlador visible. Desde allí presenta `UIActivityViewController` en la cola principal y configura el popover para iPad. La ausencia de un parámetro Context no significa independencia del entorno: depende del estado de UIKit y necesita un controlador válido. `PlatformModule.ios.kt` registra `CompartidorIos()` sin argumentos. En Android comprobé que se abre el selector y aparece el texto del producto; esto no acredita un envío a otra persona ni el funcionamiento de la hoja iOS.

## 3. Expect/actual frente a interfaz e inyección

El proyecto combina ambas estrategias. Para una utilidad pequeña como `formatearSoles`, expect/actual mantiene una llamada común y permite que el compilador vincule la implementación del destino. La nueva clase `InfoDispositivo` sigue el patrón exigido por la actividad: el commonMain conoce únicamente dos cadenas; Android consulta `Build.VERSION.RELEASE` e iOS consulta `UIDevice`.

Una interfaz inyectada permitiría entregar implementaciones falsas con facilidad y cambiar el servicio sin modificar el destino de compilación. A cambio, exigiría declarar el contrato, construir la implementación y registrar su proveedor. Para compartir elegí precisamente esa alternativa: `domain/platform/Compartidor.kt` es una interfaz ordinaria y el ViewModel recibe el servicio mediante Koin. Así, las pruebas del detalle pueden verificar el texto o simular un fallo sin abrir un selector nativo. El acoplamiento con Context y UIKit queda en los source sets específicos. La legibilidad mejora cuando cada mecanismo corresponde a su responsabilidad, aunque las pruebas con dobles no sustituyen las capturas del sistema.

## 4. Ausencia deliberada de un actual

Para comprobar el enlace de declaraciones, retiré temporalmente el archivo `Formato.android.kt` y compilé Android. El mensaje capturado fue: `Expected formatearSoles has no actual declaration in module <commonMain> for JVM`. El compilador no encontró la implementación para ese destino y la compilación falló; no existe una elección automática del archivo iOS como reemplazo. Después restauré el archivo y ejecuté `:shared:compileAndroidMain`, que terminó con `BUILD SUCCESSFUL`, recuperando la compilación de caché. La posterior ejecución de `:androidApp:assembleDebug :shared:testAndroidHostTest` también terminó correctamente. Este experimento muestra que nombre, paquete y firma deben corresponder en los archivos del destino y del código común.

## 5. Capacidad que debe permanecer común

La validación de productos no debería bajar a Android ni a iOS. En `domain/usecase/RegistrarProductoUseCase.kt`, el nombre debe tener entre 3 y 150 caracteres, el precio debe ser finito y positivo con mínimo 0.01, el stock debe ser entero no negativo y la categoría debe ser un identificador positivo. Estas reglas pertenecen al negocio farmacéutico, no al sistema operativo.

Duplicarlas permitiría aceptar un producto en una plataforma y rechazarlo en la otra. Mantenerlas en commonMain conserva una sola decisión y permite comprobarla con repositorios falsos. El criterio aplicado es separar lo que necesita una API del sistema de aquello que representa reglas del dominio. Formato monetario, presentación de compartir e información del dispositivo usan mecanismos nativos; las validaciones, los modelos y la construcción del texto compartido permanecen comunes.

## Referencias técnicas
Las rutas del análisis son relativas a `shared/src/<sourceSet>/kotlin/pe/edu/upeu/pharmamobil/`.

- Código propio y capturas originales en este repositorio.
- JetBrains, Expected and actual declarations: https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html
- Android, NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
- Apple, NumberFormatter: https://developer.apple.com/documentation/foundation/numberformatter
- Android, compartir datos: https://developer.android.com/training/sharing/send
- Apple, UIActivityViewController: https://developer.apple.com/documentation/uikit/uiactivityviewcontroller
- Android, Build.VERSION: https://developer.android.com/reference/android/os/Build.VERSION
- Apple, UIDevice: https://developer.apple.com/documentation/uikit/uidevice

## Estado de la comparación
El cuerpo del análisis cumple el intervalo de 600 a 900 palabras. Falta sustituir la limitación de la pregunta 1 por el resultado literal observado en iOS cuando se obtenga una ejecución real. También faltan las tres capturas iOS y la identidad de quien ejecute en Mac, si colabora otra persona.
