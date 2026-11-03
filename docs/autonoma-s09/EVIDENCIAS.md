# Evidencias reales - Actividad Autónoma 09

Fecha: 06/10/2026. Estudiante: Rony Alonso Ancajima Tripul.

| Archivo | Observación acreditada |
|---|---|
| [01_moneda_android.png](evidencias/01_moneda_android.png) | Producto Paracetamol 500 mg: precio literal `S/ 6.00`, stock 25, en Android. Captura de la práctica reutilizada para esta capacidad. |
| [02_compartir_android.png](evidencias/02_compartir_android.png) | Selector nativo con `Paracetamol 500 mg — S/ 6.00 · Stock: 25`. No acredita un envío. Captura de la práctica. |
| [03_informacion_android.png](evidencias/03_informacion_android.png) | Nueva pantalla Acerca de; sistema `Android`, versión `14`; Pixel 6 API 34. |
| [04_error_falta_actual.png](evidencias/04_error_falta_actual.png) | Compilación deliberadamente fallida al retirar el actual Android del formateador. Captura de la práctica. |
| [05_build_autonoma_android.png](evidencias/05_build_autonoma_android.png) | Tras instalar la capacidad, `:androidApp:assembleDebug` y `:shared:testAndroidHostTest` terminaron con `BUILD SUCCESSFUL in 1m 20s`. Las 70 tareas son tareas de Gradle, no cantidad de pruebas. |
| [06_pruebas_practica_android.png](evidencias/06_pruebas_practica_android.png) | Informe anterior de la práctica: 54 pruebas, 0 fallos, 0 omitidas, 100 %, 0.664 s. No se atribuye esa duración a la ejecución nueva. |

## Mensaje literal del experimento
```text
Expected formatearSoles has no actual declaration in module <commonMain> for JVM
```

El archivo se restauró después. La compilación posterior del módulo y la compilación de la autónoma finalizaron correctamente.

## Pendiente de ejecución iOS
Faltan capturas del mismo producto y precio, de la hoja de compartir con su texto y de Acerca de. No existe todavía comparación lado a lado de ejecuciones Android/iOS. El actual iOS está escrito; no se declara compilado ni probado. Si colabora alguien con Mac, registrar su nombre y el dispositivo/simulador, versión del sistema, fecha y resultado observado.

La URL iOS de desarrollo (`localhost`) solo sirve si el backend corre en el mismo Mac. Para conectarse al backend de Windows debe configurarse la dirección accesible de ese equipo y comprobar la conexión antes de obtener las capturas.

## Repositorio
- Rama: https://github.com/Alonso-Tripul/PharmaMobil-S09/tree/feature/expect-actual-ancajima
- Historial: https://github.com/Alonso-Tripul/PharmaMobil-S09/commits/feature/expect-actual-ancajima

Los hashes específicos de los cuatro productos de la autónoma se incorporarán después de confirmar los commits y su publicación. Los commits anteriores de la práctica no se presentan como nuevos commits de esta actividad.
