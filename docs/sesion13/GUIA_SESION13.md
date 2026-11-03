# Sesión 13: lo que se hizo y cómo probarlo

Rama: `feature/integrador-13`, creada desde `develop`, con un commit por paso.

## 0. Antes de compilar

1. **Backend (IntelliJ).** La app necesita `POST /api/v1/auth/login`. El ZIP `pharmaSoft-master` que adjuntaste **no incluye seguridad** (no tiene `AuthController`, `SecurityConfig` ni las migraciones V2 y V3). Usa la versión del curso con seguridad (`github.com/dreyna/pharmaSoft`) y aplícale el parche `pharmasoft-idempotencia.patch`. El parche toca los mismos archivos en las dos versiones.
   - Agrega la migración `V4__clave_operacion_productos.sql`. Flyway la aplica sola al arrancar.
2. **Firebase (consola).**
   1. Entra a <https://console.firebase.google.com> y crea el proyecto «PharmaMobil». Puedes desactivar Google Analytics del proyecto, pero si lo haces DebugView no mostrará eventos.
   2. Agrega una app Android con el paquete `pe.edu.upeu.pharmamobil`.
   3. Descarga `google-services.json` y colócalo en **`androidApp/`**, junto a `androidApp/build.gradle.kts`.
   4. En la consola abre **Crashlytics** y pulsa «Habilitar».
   - El archivo está en `.gitignore`. Sin ese archivo el proyecto **no compila**: la tarea `processDebugGoogleServices` falla.
3. **Android Studio.** Ejecuta *Sync Project with Gradle Files*. Si Gradle no resuelve `google-services 4.5.0` o `firebase-crashlytics 3.0.6`, acepta la versión que sugiere Android Studio en `gradle/libs.versions.toml`.
4. **Desinstala la app anterior del emulador una sola vez** si quieres empezar limpio. Si no la desinstalas, `1.sqm` migra la base y conserva tus filas.

## 1. Qué cambió, paso por paso

| Paso | Archivos principales | Qué hace |
|---|---|---|
| 1 | `docs/sesion13/HUECOS.md` | Tabla de huecos con gravedad, decisión y fecha (Producto D). |
| 2 | `Producto.sq`, `1.sqm`, `ProductoLocal.kt` | Columnas `intentos`, `ultimoError` y `claveOperacion`; tabla `sync_estado`; consultas `pendientesPorEnviar`, `contarPendientes`, `contarFallidas`, `sumarIntento`, `marcarFallida` y `reintentarFallidas`. |
| 3 | `ResultadoEnvio.kt`, `ErrorApi.kt`, `EnvioRepositorioRest.kt`, `SincronizarUseCase.kt` | Clasifica cada error en transitorio, permanente o sesión vencida. Espera 1, 2, 4, 8 y 16 s, con un máximo de 5 intentos. Siempre sube antes de bajar. |
| 4 | `presentation/sincronizacion/*` | Panel con «Sin conexión · N cambios por enviar», «Actualizado hace…» y las fallidas, con los botones «Reintentar ahora» y «Revisar qué pasó». Se sincroniza sola al abrir la pantalla, al volver la red y al crecer la cola. |
| 5 | `GestorPermisos.kt`, `androidApp/.../permiso/*`, `TarjetaAvisos.kt` | Permiso de notificaciones con sus tres estados. La tarjeta está en Inicio. Avisa cuando PharmaSoft rechaza productos. |
| 6 | `Telemetria.kt`, `androidMain/.../TelemetriaFirebase.kt` | Crashlytics y Analytics detrás de una interfaz. El SDK solo aparece en esa clase. |
| 7 | `commonTest/.../SincronizacionTest.kt` y otras | Pruebas con MockEngine y dobles. No usan red ni dispositivo. |
| Backend | `ProductoController`, `ProductoServiceImpl`, `V4__...sql` | Con la cabecera `Idempotency-Key`, un POST repetido devuelve 200 con el producto ya creado. |

**Pruebas:** en Android Studio, en el panel Gradle, ejecuta `shared > Tasks > verification > testAndroidHostTest`. También puedes usar `./gradlew :shared:testAndroidHostTest`. Esa tarea corre `commonTest` y `androidHostTest`.

## 2. Cómo provocar cada evidencia (Productos A y C)

Ten abierto **App Inspection → Database Inspector → pharma.db → producto** para mostrar `pendiente`, `intentos`, `ultimoError` y `claveOperacion`. Ten también **Logcat** filtrado por `HttpClient` para ver los códigos HTTP.

| Paso A | Cómo se provoca | Qué capturar |
|---|---|---|
| 1 | Desinstala la app y ábrela. | Login y Database Inspector sin filas. |
| 2 | Ingresa como `admin` / `Admin123`. | Inicio con rol y la tarjeta «¿Te avisamos…?». |
| 3 | Abre Productos. | Panel «Todo enviado · Actualizado hace un momento» y tabla `sync_estado`. |
| 4 | Registra un producto con red. | ID positivo en la lista y en la consola de IntelliJ (`insert into productos`). |
| 5 | Activa el modo avión y registra otro. | Etiqueta «Pendiente», «Sin conexión · 1 cambio por enviar» e ID negativo en el Inspector. |
| 6 | Desactiva el modo avión **sin tocar nada más**. | La fila cambia sola a ID positivo y el contador queda en 0. |
| 7 | Modo avión. Registra un producto con un **nombre que ya exista** (409) o con categoría `99` (404). Quita el modo avión. | «1 producto no se pudo enviar» y «Revisar qué pasó» con el motivo del servidor. En el Inspector, `intentos = 5` y `ultimoError`. En Logcat, el 409 o el 404. |
| 8 | Ingresa como `almacen` y abre «Mi perfil → Comprobar acceso a clientes». | Mensaje de 403. Para `vendedor`, el formulario de productos no aparece. |
| 9 | En el backend, baja temporalmente `jwt.expiracion-minutos` a 1. Reinicia, ingresa y espera. | Vuelta al login con «Tu sesión terminó». Las filas pendientes siguen con `intentos` en 0. |
| 10 | Menú → Cerrar sesión. | Login. En el Inspector, las filas siguen allí con su `propietario`. |

**Transitorio con espera (extra para B y D):** detén PharmaSoft en IntelliJ con un producto pendiente y pulsa «Sincronizar». Verás «Sin conexión con el servidor» e `intentos = 1`, y la cola se corta. Un 503 real (por ejemplo, un proxy) suma intentos con espera creciente.

**Producto C: permiso de notificaciones (Android 13 o superior)**

| Estado | Cómo llegar | Qué ve el usuario |
|---|---|---|
| Denegado | Instalación limpia. | Tarjeta explicativa y botón «Permitir avisos». |
| Concedido | Pulsa «Permitir avisos» → Permitir. | «Avisos activados». Provoca el caso 7 y llega la notificación. |
| Denegado para siempre | Ajustes → Apps → PharmaMobil → Almacenamiento → Borrar datos, o reinstala. Deniega **dos veces**. | «Los avisos están bloqueados» y botón «Abrir Ajustes». Al volver con el permiso activado, la tarjeta cambia sola. |

**Firebase:** Crashlytics muestra el no fatal `FilasRechazadasException` del caso 7 **después de cerrar y volver a abrir la app**. Los eventos `sync_completada` y `permiso_respuesta` aparecen en Analytics → DebugView si antes ejecutas:

```
adb shell setprop debug.firebase.analytics.app pe.edu.upeu.pharmamobil
```

## 3. iOS

`GestorPermisosIos`, `NotificadorIos`, `MonitorRedIos` (siempre conectado) y `TelemetriaConsola` están registrados en `PlatformModule.ios.kt`, pero no se compilaron porque no hay macOS. Firebase en iOS exige enlazar los frameworks nativos en Xcode y `GoogleService-Info.plist`. Si no tienes Mac, documenta ese estado con fecha en el Producto B.
