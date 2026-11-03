# PharmaMobil — Sesión 13: proyecto integrador (offline-first refinado, Firebase y permisos)

Rama `feature/integrador-13`, creada desde `develop`, con un commit por paso de la guía práctica N.º 13.

## Sesión 13: qué se integró

| Paso | Qué se hizo |
|---|---|
| 1 | Recorrido completo y tabla de huecos con gravedad, decisión y fecha de cierre: [`docs/sesion13/HUECOS.md`](docs/sesion13/HUECOS.md). |
| 2 | La cola local guarda `intentos`, `ultimoError` y `claveOperacion` por fila. La migración `1.sqm` conserva los datos de la sesión 11. |
| 3 | `SincronizarUseCase` distingue fallo transitorio (sin red, tiempo agotado, 5xx) de permanente (400, 404, 409, 403). Espera 1, 2, 4, 8 y 16 s y admite como máximo 5 intentos. Siempre sube antes de bajar. Un 401 detiene la cola sin marcar filas. |
| 4 | Panel de sincronización en Productos con «Sin conexión · N cambios por enviar», «Actualizado hace…» y las filas fallidas, con «Reintentar ahora» y «Revisar qué pasó». Se sincroniza sola al abrir la pantalla, al volver la red y al crecer la cola. |
| 5 | `GestorPermisos` en el código común, con sus tres estados (concedido, denegado y denegado para siempre). En Android, `PuentePermisos` conecta con la Activity. El permiso de notificaciones se resuelve desde la tarjeta de Inicio. |
| 6 | Interfaz `Telemetria` en el dominio, implementada con Firebase Crashlytics y Analytics (GitLive 2.7.0) solo en la capa de datos de Android. |
| 7 | Pruebas en `commonTest` con MockEngine y dobles: sincronización, ViewModel de estado y gestor de permisos falso. |

En el backend, `POST /api/v1/productos` acepta la cabecera `Idempotency-Key` (migración `V4__clave_operacion_productos.sql`). Un reintento devuelve el producto ya creado en lugar de duplicarlo.

**Antes de compilar:** coloca `google-services.json` (de la consola de Firebase, paquete `pe.edu.upeu.pharmamobil`) en `androidApp/`.

Cómo ejecutar, probar y sacar cada evidencia: [`docs/sesion13/GUIA_SESION13.md`](docs/sesion13/GUIA_SESION13.md).

---

# Historial: Sesión 11 — autenticación JWT y persistencia offline-first

## Sesión 11: persistencia offline-first

La pantalla Productos se suscribe al inventario de SQLDelight en `pharma.db`.
Al abrir la app con un token todavía vigente, se recupera la sesión ya verificada
desde los metadatos locales si PharmaSoft no responde. El JWT continúa únicamente
en EncryptedSharedPreferences (Android) o Keychain (iOS), nunca en SQLite.
El inventario local separa sus filas por usuario, incluidas las altas pendientes.

Un producto registrado sin red aparece enseguida con la etiqueta **Pendiente** y
un ID negativo. Al pulsar **Sincronizar**, se envían primero los pendientes y
después se trae la lista REST usando el `HttpClient` autenticado de la sesión 10.
La edición y baja de productos sincronizados siguen requiriendo conexión. No
hay cola offline para modificaciones o bajas.

La guía indica SQLDelight 2.1.0; se usa 2.4.0 para compatibilidad con AGP 9
ya presente en este proyecto. Se conservan `categoriaId` y `estado` en la
tabla, además de los campos mínimos de la guía.

### Comprobación manual requerida

1. Con PharmaSoft activo, inicia sesión y abre Productos para cargar el inventario.
2. Cierra completamente la app; apaga PharmaSoft o activa modo avión.
3. Abre de nuevo. Comprueba que la lista guardada aparece con un aviso de red.
4. Registra un producto y confirma que aparece como Pendiente.
5. Reconecta, pulsa Sincronizar y verifica en Swagger un ID positivo para ese producto.
6. Ejecuta `.\gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest --console=plain`
   desde la carpeta `pharmaMobil`. iOS requiere macOS y Xcode.

Se añadieron tres pruebas de sincronización en `commonTest` y pruebas SQL con
SQLite en memoria en `androidHostTest`, incluidas separación por usuario y
metadatos de sesión sin JWT. No se adjuntan capturas ni resultados de emulador
que no se hayan ejecutado. La compilación no se pudo correr en el entorno de
preparación porque la descarga de Gradle 9.1.0 estuvo bloqueada por red.

La API no ofrece una clave de idempotencia: si acepta un POST y la app se cierra
antes de guardar el ID de respuesta, un reintento puede duplicar ese producto.
El repositorio persiste cada ID recibido antes de descargar la lista para
reducir esa ventana.

**Estudiante:** Rony Alonso Ancajima Tripul. Proyecto continuado desde la sesión 9.

## Abrir en Windows

1. Extrae este ZIP en `C:\Proyectos\Sesion10`.
2. En Android Studio, elige **Open** y abre `C:\Proyectos\Sesion10\pharmaMobil`.
3. Espera **Gradle Sync**, con el JDK integrado y Android SDK 36.
4. Mantén PharmaSoft ejecutándose en el puerto 8080. Oracle debe estar activo; sus migraciones deben llegar a V3.
5. Selecciona **androidApp**, el emulador y pulsa **Run**.
6. Aparecerá el login. Ingresa con uno de los usuarios de abajo.

## Interfaz y roles visibles

El login tiene identidad visual propia, contraseña visible/oculta y estados de carga/error. El inicio presenta un saludo, la etiqueta del rol y tarjetas de módulos disponibles. El menú muestra avatar con iniciales, nombre, usuario y roles obtenidos de PharmaSoft. La barra superior también identifica el rol.

**Mi perfil y permisos** detalla los permisos reales de la cuenta. El acceso a clientes se comprueba desde ese panel y conserva la sesión ante un 403. Cerrar sesión pide confirmar. Se retiró el acceso a Pedidos porque el módulo todavía no está implementado. Productos y Clientes aparecen según sus permisos de lectura; las acciones de escritura mantienen sus restricciones. No hay un selector que permita cambiar el rol de la cuenta.

Al actualizar: cierra la sesión anterior e ingresa nuevamente para comprobar la etiqueta del rol. Se mantienen ADMIN, VENDEDOR y ALMACENERO del backend actual.

## Autenticación

| Usuario | Contraseña de desarrollo | Acceso esperado |
| --- | --- | --- |
| admin | Admin123 | Catálogo y clientes; escritura de productos |
| vendedor | Vendedor123 | Lectura de catálogo y clientes; sin escritura de productos |
| almacen | Almacen123 | Lectura y escritura de catálogo; sin acceso a clientes |

El emulador usa `http://10.0.2.2:8080/api/v1/`. Para un teléfono físico cambia la URL de `PlatformModule.android.kt` por la IP de tu PC. HTTP local está habilitado únicamente en debug. Para publicar, usa HTTPS.

`Credenciales`, `Sesion`, `AuthRepository` y `AlmacenSeguro` son contratos comunes. Android almacena exclusivamente el token en EncryptedSharedPreferences con MasterKey AES256; iOS usa Keychain con accesibilidad WhenUnlockedThisDeviceOnly. Las contraseñas no se guardan.

El login llama a `POST /auth/login`. Ktor agrega Bearer a las peticiones de la API, excluye el login y restringe el token al origen configurado. No sigue redirecciones. Al reiniciar, recupera el token y consulta `GET /auth/me`; PharmaSoft valida la firma y vigencia. El campo `exp` agenda el cierre local, sin reemplazar la validación del servidor. Cerrar sesión borra el token y limpia el proveedor Bearer.

Un **401 del login** indica credenciales incorrectas. Un **401 de un recurso protegido** borra la sesión y vuelve al login con un aviso. Un **403** muestra un mensaje de falta de permiso y conserva la sesión. `CATALOGO_ESCRIBIR` controla registro, edición y baja. `CLIENTES_LEER` controla el menú Clientes. La API sigue siendo quien autoriza cada petición.

El panel **Mi perfil y permisos** incluye **Comprobar acceso a clientes**: consulta realmente `GET /clientes` y permite observar el 403 con `almacen`. El CRUD de productos es REST. El formulario/listado de clientes heredado de sesión 9 sigue siendo local; no presentarlo como CRUD REST. Esta comprobación sí utiliza el backend.

Los logs son HEADERS en debug, NONE en release. Authorization se oculta, el login se excluye y no se registran cuerpos, contraseñas ni respuestas con tokens. Android desactiva la copia de seguridad de la app.

## Verificación en tu equipo

Desde la terminal de la carpeta del proyecto:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :shared:testAndroidHostTest
```

Las pruebas nuevas con MockEngine cubren login sin Bearer, petición posterior con Bearer, restauración con /me, logout, 401, 403, permisos, expiración y exclusión de otro servidor. Son pruebas del cliente con respuestas simuladas; no sustituyen las pruebas con Oracle/PharmaSoft.

Prueba manual y captura evidencia propia:

1. Contraseña incorrecta: permanece en login y muestra un aviso.
2. `admin`: lista productos, permite registrar/editar/baja y muestra Clientes.
3. Cierra sesión e ingresa como `vendedor`: el formulario y botones de escritura desaparecen.
4. `almacen`: Clientes no aparece en el menú. **Comprobar acceso a clientes** muestra falta de permiso; sigue conectado.
5. Con sesión activa, cierra completamente y reabre la app: debe recuperar la sesión con /me.
6. Reduce temporalmente la duración JWT en el backend, reinícialo y vuelve a ingresar: al vencer, la app vuelve al login. Restablece la duración después.
7. Abre el menú, pulsa **Cerrar sesión**, confirma y reabre: debe mostrar login.

**Estado de validación de esta actualización visual:** revisión de cambios y `git diff --check` realizadas. La compilación se intentó pero la descarga de Gradle 9.1.0 falló por falta de acceso de red en el entorno de preparación; no se ejecutaron las pruebas nuevas ni la app Android/iOS aquí. No se incluyen capturas inventadas de sesión 10. Las evidencias heredadas de sesión 9 no acreditan estas funciones nuevas.

## iOS

Se requiere macOS y Xcode. Abre `iosApp/iosApp.xcodeproj`, elige un simulador y ejecuta. El backend debe estar en ese Mac o debes cambiar la URL de `PlatformModule.ios.kt` por la dirección accesible del servidor. `localhost` en el simulador no es tu PC Windows.

```bash
bash gradlew :shared:iosSimulatorArm64Test
```

## Historial

La rama `feature/auth-jwt-ancajima` continúa el historial incluido en el ZIP original. Los commits de esta integración describen cambios reales de esta preparación; no simulan trabajo distribuido durante la clase. Conserva el historial al extraer el ZIP.

---

## Documentación heredada de sesión 9

# PharmaMobil — Sesión 09: capacidades nativas

**Estudiante:** Rony Alonso Ancajima Tripul  
**Curso:** Desarrollo de Aplicaciones Móviles — UPeU, ciclo VI  
**Docente:** Mg. Reyna Barreto Benjamin David  
**Fecha de la guía:** 6 de octubre de 2026

Implementación de la Guía Práctica de Laboratorio N.º 09 adjunta: moneda peruana mediante `expect/actual` y compartir mediante un contrato de dominio e inyección por plataforma. Incluye detalle de producto en Compose común.

## Abrir y ejecutar Android

1. Descomprime todo el ZIP en una carpeta corta, por ejemplo `C:\Proyectos\Sesion09`.
2. En Android Studio, abre **la carpeta `pharmaMobil`**, donde están `settings.gradle.kts`, `androidApp` y `shared`. No abras el ZIP ni el backend.
3. Usa el JDK integrado de Android Studio compatible con AGP 9 (JDK 17 o superior), instala Android SDK 36 y espera la sincronización de Gradle.
4. Ejecuta PharmaSoft y prepara una categoría y productos con las instrucciones de `../LEEME_PRIMERO.md`.
5. Selecciona la configuración/módulo **`androidApp`** y un emulador Android. Pulsa Run.
6. En la app, abre el menú → Productos. Pulsa un producto → Compartir.

Alternativa en la terminal del proyecto:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :shared:testAndroidHostTest
```

La dirección del backend del emulador es `http://10.0.2.2:8080/api/v1/`. Se configura en `PlatformModule.android.kt`. En un teléfono físico cambia esa dirección por la IP del equipo que ejecuta PharmaSoft. El permiso de Internet está en el manifiesto principal; HTTP local se permite en la variante **debug**.

## Ejecutar iOS

Se necesita macOS, Xcode y un simulador. Abre `iosApp/iosApp.xcodeproj`, selecciona el esquema `iosApp` y ejecuta. El backend predeterminado es `http://localhost:8080/api/v1/`, pensado para el simulador y PharmaSoft en el mismo Mac. La dirección está en `PlatformModule.ios.kt`; una API remota requiere adaptar la dirección y la política de transporte de iOS. `Info.plist` permite conexiones de red locales.

```bash
chmod +x gradlew
./gradlew :shared:iosSimulatorArm64Test
```

## Capacidades nativas

Las rutas siguientes están bajo `shared/src/<sourceSet>/kotlin/pe/edu/upeu/pharmamobil/`.

| Responsabilidad | Código común | Android | iOS |
|---|---|---|---|
| Contrato de moneda | `commonMain/platform/Formato.kt` | `androidMain/platform/Formato.android.kt` | `iosMain/platform/Formato.ios.kt` |
| Contrato de compartir | `commonMain/domain/platform/Compartidor.kt` | `androidMain/platform/CompartidorAndroid.kt` | `iosMain/platform/CompartidorIos.kt` |
| Registro de dependencias | `commonMain/di/AppModule.kt` | `androidMain/di/PlatformModule.android.kt` | `iosMain/di/PlatformModule.ios.kt` |
| Texto compartido | `commonMain/domain/usecase/TextoParaCompartir.kt` | Se recibe el mismo texto común | Se recibe el mismo texto común |
| Formato en presentación | `commonMain/presentation/producto/ProductoUi.kt` | Invoca su `actual` | Invoca su `actual` |
| Detalle y acción | `commonMain/presentation/detalle/DetalleProductoViewModel.kt` y `DetalleProductoScreen.kt` | Selector del sistema | Hoja del sistema |

### 1. Formato de soles

`expect fun formatearSoles(valor: Double): String` define el contrato. Los dos `actual` conservan paquete, nombre, parámetro y retorno. Android usa `NumberFormat` y `Locale("es", "PE")`; iOS usa `NSNumberFormatter`, estilo de moneda y `NSLocale("es_PE")`.

El precio de pantalla se transforma en `Producto.aUi()`. El dominio conserva `precio: Double` y el composable recibe una cadena lista para mostrar. El texto compartido utiliza el formateador, siguiendo el paso 3 de la guía.

El formato exacto lo define cada sistema. No se impone un espacio o símbolo idéntico entre plataformas ni se reconstruyen los decimales a mano.

### 2. Compartir

El contrato `Compartidor` vive en `domain` y solo conoce `String`. Android implementa `ACTION_SEND`, `text/plain`, `EXTRA_TEXT` y un chooser con `FLAG_ACTIVITY_NEW_TASK`, necesario al usar el contexto de aplicación. `MainApplication` suministra ese contexto al iniciar Koin.

iOS crea `UIActivityViewController` en la cola principal. Busca la ventana activa, recorre controladores presentados y configura el popover para iPad. Se conserva el acceso a `keyWindow` como alternativa de compatibilidad. Si no existe una ventana activa no se presenta la hoja; debe comprobarse en ejecución.

Cada `platformModule` registra `single<Compartidor>`. Koin lo inyecta en `DetalleProductoViewModel`. El botón de Compose llama a `compartir()`; la pantalla no usa APIs de Android ni UIKit. Se comparte el producto cargado desde el repositorio; editar campos sin guardarlos no cambia el texto compartido.

### Diferencias por plataforma

| Aspecto | Android | iOS | Decisión |
|---|---|---|---|
| Formateador | `java.text.NumberFormat` | Foundation `NSNumberFormatter` | `expect/actual` para utilidad sin estado |
| Configuración regional | `es`, `PE` | `es_PE` | Usar moneda peruana en ambas |
| Símbolos y espacios | Dependen de los datos regionales de Android | Dependen de Foundation y versión del sistema | Comparar capturas reales, sin asumir igualdad literal |
| Compartir texto | Intent y selector | `UIActivityViewController` | Contrato e inyección para sustituir la capacidad en pruebas |
| Dependencia nativa | `Context` de aplicación | Ventana/controlador activo | Mantener las dependencias en sus source sets |
| Condición de presentación | Bandera `NEW_TASK` | Cola principal y popover en iPad | Evitar fallos de presentación |
| Motor HTTP | Ktor Android (HttpURLConnection) | Ktor Darwin | Red común con motor por plataforma |
| Equipo de compilación | Android Studio en Windows/macOS/Linux | macOS y Xcode | Verificación independiente |

### Interoperabilidad Kotlin–Swift

`iOSApp.swift` importa `Shared` y llama a `KoinIosKt.doInitKoinIos()`. El archivo Kotlin `KoinIos.kt` agrupa su función superior bajo el sufijo `Kt`; la exportación adapta el nombre que empieza por `init`. `ContentView.swift` llama a `MainViewControllerKt.MainViewController()` y aloja la UI Compose mediante `UIViewControllerRepresentable`.

Los tipos Kotlin anulables se utilizan como opcionales del lado Swift. Las clases Kotlin no se convierten automáticamente en estructuras de valor de Swift; no debe suponerse que su sintaxis de `copy` y desestructuración se conserva. Una jerarquía sellada no aporta la misma comprobación exhaustiva al `switch` de Swift. Las funciones `suspend` se exportan mediante puentes de finalización y pueden usarse como `async` en Swift; la propagación de excepciones debe diseñarse con `@Throws` cuando corresponda. Esta práctica mantiene estados y decisiones en Kotlin y no modifica la entrada Swift existente.

## Conexión con PharmaSoft

El proyecto adjunto originalmente tenía repositorios en memoria. Se añadió `ProductoRepositorioRest` para los productos y se registró como repositorio de ejecución. Consume el contrato real incluido: `GET /productos` devuelve `contenido` y `ultima`; el cliente recorre todas las páginas. Se usan también `GET /productos/{id}`, `POST`, `PUT` y `DELETE`.

Crear/editar envía `nombre`, `precio`, `stock`, `estado` y `categoriaId`. El ID de categoría debe existir en PharmaSoft. La baja es **lógica**: el backend cambia `estado=false` y el producto puede seguir en el listado con la etiqueta Inactivo. El módulo Clientes conserva el repositorio en memoria de la base adjunta. JWT y sincronización offline pertenecen al producto de la unidad y no se presentan como implementados en esta sesión.

### Backend con el esquema existente usado en la prueba

El primer arranque conectó a Oracle, pero Flyway detectó una diferencia entre la migración V1 local y la que ya estaba registrada. Para esta sesión se inició el backend desde la carpeta `pharmaSoft` con:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=sesion09" "-Dspring-boot.run.arguments=--spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=validate"
```

Este comando usa el esquema existente y mantiene la validación de Hibernate. La discrepancia del historial de Flyway sigue pendiente de revisar contra la migración que creó esas tablas; no se hizo `repair`, ni se recreó la base. En una base nueva debe usarse una migración compatible con su versión de Oracle.

## Evidencias de la práctica

Consulta `docs/EVIDENCIAS_S09.md`. Deben adjuntarse cinco capturas auténticas:

1. Error por falta de `actual` del punto de control 1.
2. Listado Android con precios en soles.
3. Listado iOS con precios en soles.
4. Selector de compartir Android con el texto del producto.
5. Hoja de compartir iOS con el mismo producto.

**Resultados verificados el 06/10/2026 en el equipo del estudiante:**

- `:androidApp:assembleDebug` y `:shared:testAndroidHostTest`: **BUILD SUCCESSFUL**.
- Informe de Gradle: **54 pruebas, 0 fallos, 0 omitidas y 100 % de éxito**; duración de las pruebas **0,664 s**. Son pruebas de host; no son 54 pruebas de interfaz en el emulador.
- Pixel 6, Android API 34: inventario obtenido de PharmaSoft, con Paracetamol 500 mg a **S/ 6.00** y stock **25**.
- Detalle y selector nativo Android verificados. Texto observado: `Paracetamol 500 mg — S/ 6.00 · Stock: 25`.
- Punto de control: al retirar temporalmente `Formato.android.kt`, el compilador informó `Expected formatearSoles has no actual declaration`. Después de restaurarlo, `:shared:compileAndroidMain` terminó con **BUILD SUCCESSFUL**; la tarea de compilación se recuperó de caché.
- PharmaSoft inició en el puerto 8080 con Java 21 y Oracle **21.3 / XEPDB1**, usando las tablas existentes y Flyway desactivado solo mediante un argumento de ejecución.
- **iOS: compilación, ejecución, listado y hoja de compartir pendientes de una Mac con Xcode.** Hay implementación en el código, pero aún no hay evidencia de ejecución iOS. Las capturas Android no sustituyen las dos capturas iOS exigidas.

Las capturas originales están incluidas y comentadas en [docs/EVIDENCIAS_S09.md](docs/EVIDENCIAS_S09.md). La prueba visual acredita carga, formato, detalle y apertura del selector; no se declara verificado todo el CRUD ni el envío a un destinatario.

La revisión de source sets encontró las tres firmas de `formatearSoles`, los dos registros de `Compartidor`, el botón en el detalle y ninguna importación `android.*` o `platform.UIKit.*` en `presentation`.

Se conservaron las pruebas originales y se añadieron cinco casos para compartir el producto persistido, evitar compartir un producto anterior después de un error, mostrar errores del selector, recorrer la paginación de PharmaSoft y propagar un 404. Las pruebas usan dobles de dominio y `MockEngine`; no sustituyen la evidencia de los selectores nativos.

## Rama y commits exigidos

Trabajo individual de Rony Alonso Ancajima Tripul.

- Repositorio: [Alonso-Tripul/PharmaMobil-S09](https://github.com/Alonso-Tripul/PharmaMobil-S09).
- Rama publicada: [feature/expect-actual-ancajima](https://github.com/Alonso-Tripul/PharmaMobil-S09/tree/feature/expect-actual-ancajima).
- Historial de la rama: [commits](https://github.com/Alonso-Tripul/PharmaMobil-S09/commits/feature/expect-actual-ancajima).
- Commit de corrección de la prueba REST mostrado en consola: `c9abcbb`.

La consola confirmó el envío de la rama y de las correcciones. El historial completo debe comprobarse para acreditar los tres commits propios que exige la guía; no se deduce su cantidad del número de tareas de Gradle.

## Referencias técnicas

- Guía Práctica de Laboratorio N.º 09 adjunta, páginas 5–11.
- JetBrains: https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html
- JetBrains: https://kotlinlang.org/docs/native-objc-interop.html
- Ktor: https://ktor.io/docs/client-dependencies.html
- Ktor: https://ktor.io/docs/client-serialization.html
- Koin: https://insert-koin.io/docs/reference/koin-core/dsl/
- Apple: https://developer.apple.com/documentation/uikit/uipopoverpresentationcontroller/sourceview

## Código específico de plataforma

Actividad Autónoma N.º 09. Inventario completo: [INVENTARIO.md](docs/autonoma-s09/INVENTARIO.md). Análisis comparativo: [ANALISIS_COMPARATIVO.md](docs/autonoma-s09/ANALISIS_COMPARATIVO.md). Evidencias: [EVIDENCIAS.md](docs/autonoma-s09/EVIDENCIAS.md).

| Capacidad | Contrato | Android | iOS |
|---|---|---|---|
| Moneda peruana | `expect fun formatearSoles(valor: Double): String` | `NumberFormat` y `Locale("es", "PE")` | `NSNumberFormatter` y `NSLocale("es_PE")` |
| Compartir | Interfaz común `Compartidor`; proveedor mediante `actual platformModule` | `Context`, `Intent.ACTION_SEND` y chooser | `UIActivityViewController` y controlador activo |
| Inyección | `expect val platformModule: Module` | Koin `module`, `androidContext()`, motor Ktor Android | Koin `module`, motor Ktor Darwin |
| Información del dispositivo | `expect class InfoDispositivo()`; `sistema` y `version` | `Build.VERSION.RELEASE` | `UIDevice.currentDevice.systemName` y `systemVersion` |

En la app, abrir **menú → Acerca de**. La pantalla común está en `presentation/acerca/AcercaDeScreen.kt`; las rutas completas de los contratos e implementaciones figuran en el inventario. No hay importaciones `android.` ni `platform.` en todo commonMain.

**Verificado en Android:** la compilación y la tarea de pruebas finalizaron correctamente; Acerca de mostró **Android 14**. La captura y el log se incluyen en las evidencias. **iOS:** código implementado, compilación y evidencia visual pendientes. La actividad no se presenta como verificada en ambas plataformas hasta completar esa ejecución.
