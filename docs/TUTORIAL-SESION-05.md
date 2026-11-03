# Tutorial — PharmaMobil por capas (Sesión 5)

Guía paso a paso de todo lo que se cambió en el proyecto: cómo se arregló el
build que estaba roto, cómo se reorganizó el módulo de Productos en capas
Clean + MVVM con Koin, y cómo se rediseñó la interfaz.

| | |
|---|---|
| **Proyecto** | PharmaMobil (Kotlin Multiplatform + Compose Multiplatform) |
| **Rama** | `feature/clean-mvvm` |
| **Commits** | `947ed3c` (arreglo del build, en `master`) · `3a4bf01` (capas + Koin) · `a64459e` (interfaz) |
| **Plataformas** | Android e iOS desde `shared` |

---

## Índice

1. [De dónde partimos](#1-de-dónde-partimos)
2. [Parte 0 — Arreglar el build](#parte-0--arreglar-el-build)
3. [Parte 1 — La rama de trabajo](#parte-1--la-rama-de-trabajo)
4. [Parte 2 — La capa de dominio](#parte-2--la-capa-de-dominio)
5. [Parte 3 — La capa de datos](#parte-3--la-capa-de-datos)
6. [Parte 4 — La capa de presentación](#parte-4--la-capa-de-presentación)
7. [Parte 5 — Inyección de dependencias con Koin](#parte-5--inyección-de-dependencias-con-koin)
8. [Parte 6 — Las pruebas](#parte-6--las-pruebas)
9. [Parte 7 — El rediseño de la interfaz](#parte-7--el-rediseño-de-la-interfaz)
10. [Cómo verificar todo](#cómo-verificar-todo)
11. [Lista de cotejo: dónde se cumple cada criterio](#lista-de-cotejo-dónde-se-cumple-cada-criterio)
12. [Problemas que te puedes encontrar](#problemas-que-te-puedes-encontrar)

---

## 1. De dónde partimos

La aplicación ya abría, navegaba y se veía bien, pero tenía cuatro problemas
de fondo:

1. `ProductoScreen` guardaba **nueve estados con `remember`** dentro del propio
   composable, así que **el formulario se vaciaba al rotar el teléfono**.
2. Las reglas del negocio estaban repartidas: `ProductoValidator` validaba en
   *presentación*, `DetallePedido` validaba en *dominio*.
3. `ProductoRepository` era una **clase concreta** en `data`, sin interfaz.
4. Nadie ensamblaba nada: cada pantalla construiría su propio repositorio.

El objetivo es dejar el módulo de Productos funcionando de extremo a extremo
con las capas separadas, sin perder ninguna función de la pantalla.

**Estructura final del módulo `shared`:**

```
pe/edu/upeu/pharmamobil/
├── App.kt
├── data/
│   └── repository/
│       └── ProductoRepositorioEnMemoria.kt      ← MOVIDO desde data/
├── di/
│   ├── AppModule.kt                             ← NUEVO
│   ├── PlatformModule.android.kt                ← NUEVO (androidMain)
│   ├── PlatformModule.ios.kt                    ← NUEVO (iosMain)
│   └── KoinIos.kt                               ← NUEVO (iosMain)
├── domain/
│   ├── model/          Producto, Cliente, Pedido, DetallePedido, EstadoPedido
│   ├── repository/
│   │   └── ProductoRepository.kt                ← NUEVO (interfaz)
│   └── usecase/
│       └── RegistrarProductoUseCase.kt          ← NUEVO
├── navigation/         Screen.kt
├── presentation/
│   ├── cliente/        ClienteScreen, ClienteValidator
│   ├── components/     ValidatedTextField, MensajeExito, EstadoVacio
│   ├── inicio/         InicioScreen
│   └── producto/       ProductoScreen, ProductoViewModel, ProductoUiState
└── theme/              PharmaMobilTheme.kt
```

---

## Parte 0 — Arreglar el build

Antes de tocar arquitectura había que poder compilar. El build fallaba así:

```
> Could not find org.jetbrains.compose.material:material-icons-extended:1.11.1.
```

### 0.1 El diagnóstico

El `libs.versions.toml` apuntaba los iconos a la misma versión que Compose
Multiplatform (`1.11.1`). Consultando Maven Central se confirma que
**`material-icons-extended` dejó de publicarse en la versión 1.7.3**: Google
congeló esa librería y JetBrains no la publica desde Compose Multiplatform
1.8.0 en adelante.

Además —y esto es lo importante— **`material-icons-core` tampoco existe para
1.11.x**, y `material3:1.11.0-alpha07` ya no lo arrastra como dependencia
transitiva. Se puede comprobar inspeccionando el `.klib` de iOS: solo contiene
el paquete `androidx.compose.material3`, sin `androidx.compose.material.icons`.

> **Conclusión:** sin esa dependencia no compila **ningún** `Icons.Default.*`,
> no solo el `Medication` que aparecía en el error.

### 0.2 La solución

Dar a los iconos su propia versión, fijada en la última publicada:

```toml
# gradle/libs.versions.toml
[versions]
composeMultiplatform = "1.11.1"
# Ultima version publicada de los iconos Material: Google congelo la libreria
# y no se publica para Compose Multiplatform 1.8.0 en adelante.
composeMaterialIcons = "1.7.3"

[libraries]
compose-material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "composeMaterialIcons" }
```

### 0.3 El segundo error, escondido detrás del primero

`App.kt` tenía un `when (pantallaActual)` que cubría `Inicio`, `Productos` y
`Clientes` pero **no `Screen.Pedidos`**. En Kotlin 2.x un `when` no exhaustivo
sobre una `sealed class` es **error de compilación**, no advertencia. Se
completó con la rama faltante.

> El fallo de *configuration cache* que aparecía en el log era solo un síntoma
> derivado del fallo de resolución; desapareció solo.

---

## Parte 1 — La rama de trabajo

```bash
git checkout -b feature/clean-mvvm
```

Todo el trabajo de la actividad vive aquí; `master` no se toca.

---

## Parte 2 — La capa de dominio

El dominio no conoce a nadie: **ninguna de sus clases importa Compose, Android,
iOS, Ktor ni Koin.** Se puede comprobar con una búsqueda de imports.

### 2.1 Una regla de negocio propia en `Producto`

El modelo era una `data class` sin comportamiento. Ahora sabe cuándo necesita
reposición:

```kotlin
// domain/model/Producto.kt
data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int
) {

    /**
     * Un producto necesita reposicion cuando su stock cae por debajo del
     * minimo que la botica mantiene en gondola.
     */
    val requiereReposicion: Boolean
        get() = stock < STOCK_MINIMO

    companion object {
        const val STOCK_MINIMO = 10
    }
}
```

`DetallePedido` conserva intacta su validación `require(cantidad > 0)`.

### 2.2 El repositorio pasa a ser una interfaz

Se escribe en el lenguaje del negocio: "registrar" y "listar". No nombra
tecnologías, porque mañana la implementación será REST y el dominio no debe
enterarse.

```kotlin
// domain/repository/ProductoRepository.kt
interface ProductoRepository {

    /** Incorpora el producto al inventario y devuelve el producto ya identificado. */
    suspend fun registrar(producto: Producto): Producto

    /** Entrega el inventario completo en el orden en que fue registrado. */
    suspend fun listar(): List<Producto>
}
```

Las dos funciones son `suspend` desde ahora, aunque hoy la implementación sea
en memoria: así la firma no cambia cuando llegue el backend.

### 2.3 El caso de uso

Aquí se concentran las reglas que antes vivían en `ProductoValidator`, que se
eliminó.

**El problema de diseño interesante:** la actividad pide que el caso de uso
devuelva `Result<Producto>`, pero el `UiState` necesita **tres mensajes de
error separados**, uno por campo. Un `Result.failure` carga una sola excepción.

**La solución:** juntar los tres errores en un objeto y hacerlo viajar dentro
de la excepción.

```kotlin
// domain/usecase/RegistrarProductoUseCase.kt

data class ErroresDeProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null
}

class ProductoInvalidoException(
    val errores: ErroresDeProducto
) : IllegalArgumentException("Los datos del producto no cumplen las reglas del negocio")

class RegistrarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(
        nombre: String,
        precio: String,
        stock: String
    ): Result<Producto> {

        val errores = ErroresDeProducto(
            nombre = validarNombre(nombre),
            precio = validarPrecio(precio),
            stock = validarStock(stock)
        )

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return try {
            Result.success(
                productoRepository.registrar(
                    Producto(
                        id = 0L,
                        nombre = nombre.trim(),
                        precio = precio.toDouble(),
                        stock = stock.toInt()
                    )
                )
            )
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (fallo: Throwable) {
            Result.failure(fallo)
        }
    }

    private fun validarNombre(nombre: String): String? { /* ... */ }
    private fun validarPrecio(precio: String): String? { /* ... */ }
    private fun validarStock(stock: String): String? { /* ... */ }
}
```

Con esto el ViewModel pinta los tres casilleros **sin volver a validar nada**.

> ### ⚠️ Detalle que vale la pena entender: `CancellationException`
>
> Lo intuitivo sería envolver la llamada en `runCatching { ... }`. **No lo
> hagas dentro de una función `suspend`.** `runCatching` atrapa *cualquier*
> `Throwable`, incluida la `CancellationException` que las corrutinas usan
> internamente para cancelarse. Si te la tragas, una corrutina cancelada sigue
> como si nada. Por eso el `try/catch` la vuelve a lanzar antes de capturar el
> resto.

---

## Parte 3 — La capa de datos

La clase concreta se mueve de `data/` a `data/repository/`, cambia de nombre e
implementa la interfaz del dominio.

```kotlin
// data/repository/ProductoRepositorioEnMemoria.kt
class ProductoRepositorioEnMemoria : ProductoRepository {

    private val productos = mutableListOf<Producto>()
    private var siguienteId = 1L

    override suspend fun registrar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        val guardado = producto.copy(id = siguienteId++)
        productos.add(guardado)
        return guardado
    }

    override suspend fun listar(): List<Producto> {
        delay(RETARDO_LISTADO_MS)
        return productos.toList()
    }

    private companion object {
        const val RETARDO_REGISTRO_MS = 400L
        const val RETARDO_LISTADO_MS = 600L
    }
}
```

Dos cosas importantes:

- **El id lo sigue asignando el repositorio** (`producto.copy(id = siguienteId++)`),
  nunca la pantalla. La pantalla manda `id = 0L` y el repositorio decide.
- Los `delay()` (400 y 600 ms, dentro del rango pedido de 300–800) simulan la
  latencia del futuro backend REST. Sin ellos el estado *Cargando* pasaría tan
  rápido que sería imposible verlo.

Para mover el archivo conservando el historial de Git:

```bash
git mv shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/ProductoRepository.kt \
       shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioEnMemoria.kt
```

---

## Parte 4 — La capa de presentación

### 4.1 `ProductoUiState`: un solo objeto

Todo lo que la pantalla necesita para dibujarse, junto. Las cuatro fases del
inventario son **excluyentes**, así que se modelan con una `sealed interface`:
es imposible estar cargando y con error a la vez.

```kotlin
// presentation/producto/ProductoUiState.kt
data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val registrando: Boolean = false,
    val mensajeExito: String? = null
) {

    sealed interface Fase {
        data object Cargando : Fase
        data object SinProductos : Fase
        data class ConProductos(val productos: List<Producto>) : Fase
        data class Error(val mensaje: String) : Fase
    }
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null
)
```

### 4.2 `ProductoViewModel`

Recibe el caso de uso y el repositorio **por constructor**, expone un
`StateFlow` de solo lectura y usa `viewModelScope` para lo suspendido.

```kotlin
// presentation/producto/ProductoViewModel.kt
class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())   // privado y mutable
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()  // público y de solo lectura

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {

            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }

            val fase = try {
                val productos = productoRepository.listar()
                if (productos.isEmpty()) ProductoUiState.Fase.SinProductos
                else ProductoUiState.Fase.ConProductos(productos)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (fallo: Throwable) {
                ProductoUiState.Fase.Error(fallo.message ?: "No se pudo cargar el inventario")
            }

            _uiState.update { it.copy(fase = fase) }
        }
    }

    fun onNombreChange(nombre: String) { /* actualiza formulario y limpia su error */ }
    fun onPrecioChange(precio: String) { /* ... */ }
    fun onStockChange(stock: String)  { /* ... */ }

    fun registrar() {
        if (_uiState.value.registrando) return   // evita el doble toque

        viewModelScope.launch {
            _uiState.update { it.copy(registrando = true, mensajeExito = null) }

            val formulario = _uiState.value.formulario

            registrarProducto(formulario.nombre, formulario.precio, formulario.stock).fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            registrando = false,
                            formulario = FormularioProducto(),   // limpia el formulario
                            mensajeExito = "Producto \"${producto.nombre}\" registrado correctamente"
                        )
                    }
                    cargarProductos()                            // refresca el inventario
                },
                onFailure = { fallo ->
                    when (fallo) {
                        is ProductoInvalidoException -> _uiState.update {
                            it.copy(
                                registrando = false,
                                formulario = it.formulario.copy(
                                    nombreError = fallo.errores.nombre,
                                    precioError = fallo.errores.precio,
                                    stockError = fallo.errores.stock
                                )
                            )
                        }
                        else -> _uiState.update { /* fase = Error */ }
                    }
                }
            )
        }
    }
}
```

> **Por qué esto arregla la rotación.** El estado ya no vive en el composable
> sino en el ViewModel, que sobrevive al cambio de configuración. Al rotar, la
> pantalla se vuelve a componer y **lee el mismo `uiState`**: el formulario
> mantiene lo escrito.

### 4.3 `ProductoScreen`: sin `remember` de negocio

La pantalla queda tonta. Se suscribe al estado y delega cada evento:

```kotlin
@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ...

    when (val fase = uiState.fase) {
        ProductoUiState.Fase.Cargando     -> /* indicador de progreso */
        ProductoUiState.Fase.SinProductos -> /* estado vacío */
        is ProductoUiState.Fase.ConProductos -> LazyColumn { items(fase.productos) { ... } }
        is ProductoUiState.Fase.Error     -> /* mensaje + botón Reintentar */
    }
}
```

El `when` es **exhaustivo sobre la fase**: si mañana agregas una quinta fase,
el compilador te obliga a decidir cómo se dibuja.

---

## Parte 5 — Inyección de dependencias con Koin

### 5.1 Las cuatro dependencias

```toml
# gradle/libs.versions.toml
[versions]
koin = "4.2.2"

[libraries]
koin-core              = { module = "io.insert-koin:koin-core",              version.ref = "koin" }
koin-android           = { module = "io.insert-koin:koin-android",           version.ref = "koin" }
koin-compose           = { module = "io.insert-koin:koin-compose",           version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
```

```kotlin
// shared/build.gradle.kts
androidMain.dependencies {
    // api: MainApplication (androidApp) usa androidContext() al arrancar Koin.
    api(libs.koin.android)
}
commonMain.dependencies {
    // api: androidApp llama a initKoin(), cuya firma expone KoinAppDeclaration.
    api(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    // ...
}
```

> **¿Por qué `api` y no `implementation` en dos de ellas?** Con `implementation`
> la dependencia no se propaga al módulo que consume `shared`. Como
> `MainApplication` vive en `androidApp` y necesita ver `initKoin()` (koin-core)
> y `androidContext()` (koin-android), esas dos se declaran `api`. Las otras dos
> solo se usan dentro de `shared`, así que `implementation` basta.

### 5.2 Los tres módulos y `initKoin()`

```kotlin
// di/AppModule.kt (commonMain)

val dataModule = module {
    single<ProductoRepository> { ProductoRepositorioEnMemoria() }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
}

val presentationModule = module {
    viewModel { ProductoViewModel(get(), get()) }
}

expect val platformModule: Module

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(dataModule, domainModule, presentationModule, platformModule)
    }
}
```

Tres decisiones y su motivo:

| Decisión | Por qué |
|---|---|
| El repositorio es **`single`** | Toda la app trabaja sobre el mismo inventario. Con `factory` cada pantalla vería una lista distinta. |
| Se declara **`single<ProductoRepository>`**, no `single<ProductoRepositorioEnMemoria>` | Se registra con el tipo de la **interfaz**. Cambiar a la versión REST será cambiar una línea. |
| El caso de uso es **`factory`** | No guarda estado; crear uno nuevo cada vez es más barato que mantenerlo vivo. |

### 5.3 `expect` / `actual val platformModule`

Es el enganche para lo que solo existe en una plataforma (por ejemplo el motor
HTTP cuando llegue el backend). Hoy están vacíos a propósito:

```kotlin
// di/PlatformModule.android.kt (androidMain)
actual val platformModule: Module = module {
    // Sin dependencias exclusivas de Android por ahora.
}
```

```kotlin
// di/PlatformModule.ios.kt (iosMain)
actual val platformModule: Module = module {
    // Sin dependencias exclusivas de iOS por ahora.
}
```

### 5.4 Arranque en Android

```kotlin
// androidApp/src/main/kotlin/pe/edu/upeu/pharmamobil/MainApplication.kt
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@MainApplication)
        }
    }
}
```

Y hay que declararla en el manifiesto, **si no, nunca se ejecuta**:

```xml
<application
    android:name=".MainApplication"
    android:allowBackup="true"
    ... >
```

### 5.5 Arranque en iOS

```kotlin
// di/KoinIos.kt (iosMain)
fun initKoinIos() {
    initKoin()
}
```

```swift
// iosApp/iosApp/iOSApp.swift
import SwiftUI
import Shared

@main
struct iOSApp: App {

    // Kotlin/Native antepone "do" a las funciones cuyo nombre empieza con
    // "init", por eso initKoinIos() se invoca aqui como doInitKoinIos().
    init() {
        KoinIosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

> **El detalle del `do`:** en Objective-C, los métodos que empiezan con `init`
> están reservados para los inicializadores. Para no chocar, Kotlin/Native
> renombra cualquier función que empiece con `init` anteponiéndole `do`. Por eso
> `initKoinIos()` en Kotlin es `doInitKoinIos()` en Swift. El nombre de la clase
> (`KoinIosKt`) sale del nombre del archivo `KoinIos.kt` + el sufijo `Kt`.

### 5.6 Conectar la pantalla

```kotlin
// App.kt
@Composable
fun App() = KoinContext {          // envuelve la raíz
    // ...
    Screen.Productos -> ProductoScreen(viewModel = koinViewModel())
}
```

---

## Parte 6 — Las pruebas

Las reglas se mudaron al caso de uso, así que **las pruebas se mudaron con
ellas**. No tiene sentido dejar un test apuntando a una clase borrada.

| Antes | Ahora |
|---|---|
| `presentation/producto/ProductoValidatorTest` (7 pruebas) | `domain/usecase/RegistrarProductoUseCaseTest` (10 pruebas) |
| `data/ProductoRepositoryTest` (2 pruebas) | `data/repository/ProductoRepositorioEnMemoriaTest` (2 pruebas) |
| — | `di/AppModuleTest` (3 pruebas) ← nuevo |

Los mensajes de error se conservaron **letra por letra**, así que las
aserciones originales siguen siendo válidas.

### 6.1 Una dependencia más: `kotlinx-coroutines-test`

Como el repositorio y el caso de uso ahora son `suspend`, hace falta para
poder probarlos:

```toml
coroutines = "1.10.2"
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
```

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
}
```

`runTest` trae un beneficio extra: **salta los `delay()` con tiempo virtual**,
así que las pruebas no esperan el segundo real del repositorio.

```kotlin
@Test
fun asignaIdCorrelativoAlRegistrar() = runTest {

    val repositorio = ProductoRepositorioEnMemoria()

    val primero = repositorio.registrar(nuevoProducto("Paracetamol"))
    val segundo = repositorio.registrar(nuevoProducto("Ibuprofeno"))

    assertEquals(1L, primero.id)
    assertEquals(2L, segundo.id)
}
```

### 6.2 Probar el grafo de Koin sin arrancar la app

Un error de Koin no aparece al compilar: revienta en tiempo de ejecución.
`AppModuleTest` lo detecta antes:

```kotlin
class AppModuleTest {

    @AfterTest
    fun detenerKoin() { stopKoin() }

    @Test
    fun resuelveElRepositorioPorSuInterfazDeDominio() {
        val koin = startKoin { modules(dataModule, domainModule, platformModule) }.koin
        assertIs<ProductoRepositorioEnMemoria>(koin.get<ProductoRepository>())
    }

    @Test
    fun elRepositorioEsUnicoEnTodaLaAplicacion() {
        val koin = startKoin { modules(dataModule, domainModule, platformModule) }.koin
        assertSame(koin.get<ProductoRepository>(), koin.get<ProductoRepository>())
    }

    @Test
    fun resuelveElCasoDeUsoConSuRepositorio() {
        val koin = startKoin { modules(dataModule, domainModule, platformModule) }.koin
        koin.get<RegistrarProductoUseCase>()
    }
}
```

**Total: 19 pruebas, 0 fallos.**

---

## Parte 7 — El rediseño de la interfaz

### 7.1 Paleta propia

La app usaba el morado por defecto de Material 3. Se reemplazó por una paleta
verde farmacia completa para claro y oscuro, con los **neutros también teñidos
de verde** para que las tarjetas no se vean grises sobre el fondo.

```kotlin
// theme/PharmaMobilTheme.kt
private val LightColors = lightColorScheme(
    primary = Color(0xFF006C51),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF89F8CC),
    onPrimaryContainer = Color(0xFF002117),
    // ... y el resto de roles, incluidos surfaceContainer*
)
```

> **Ojo:** si solo defines `primary` y dejas el resto por defecto, los roles sin
> especificar se quedan con el morado base y la pantalla queda mezclada. Por eso
> se definen todos los roles, incluidos los `surfaceContainer*`.

### 7.2 Los destinos, en una sola lista

Antes había cuatro bloques de ~20 líneas casi idénticos en el menú, más un
`when` aparte para el título. Ahora hay una sola fuente:

```kotlin
private data class Destino(
    val screen: Screen,
    val titulo: String,
    val icono: ImageVector
)

private val DESTINOS = listOf(
    Destino(Screen.Inicio,    "Inicio",    Icons.Default.Home),
    Destino(Screen.Productos, "Productos", Icons.Default.Medication),
    Destino(Screen.Clientes,  "Clientes",  Icons.Default.Person),
    Destino(Screen.Pedidos,   "Pedidos",   Icons.Default.ShoppingCart)
)
```

El menú hace `DESTINOS.forEach { ... }` y el título sale de
`DESTINOS.first { it.screen == screen }.titulo`.

### 7.3 Un bug de layout que estaba a la vista

El `paddingValues` del `Scaffold` se aplicaba en tres de las cuatro ramas del
`when`. Resultado: **`InicioScreen` quedaba tapada por la barra superior.**

```kotlin
// Antes: cada rama envolvía su pantalla en un Column con padding... menos Inicio.
// Ahora: se aplica una sola vez, para todas.
) { paddingValues ->

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when (pantallaActual) {
            Screen.Inicio    -> InicioScreen()
            Screen.Productos -> ProductoScreen(viewModel = koinViewModel())
            Screen.Clientes  -> ClienteScreen()
            Screen.Pedidos   -> EstadoVacio(/* ... */)
        }
    }
}
```

### 7.4 Componentes reutilizables

| Componente | Qué resuelve |
|---|---|
| `ValidatedTextField` (ampliado) | Ahora acepta **icono**, **texto de ayuda** y **tipo de teclado**. Antes los cuatro campos abrían teclado de texto; ahora precio abre decimal, stock numérico, correo el de email y teléfono el de marcación. |
| `MensajeExito` | Confirmación con icono sobre `primaryContainer`, en vez de un `Text` suelto. |
| `EstadoVacio` | Icono + título + descripción + acción opcional. Se reutiliza en inventario vacío, error de carga y el módulo de Pedidos. |

### 7.5 Las pantallas

- **Inicio** pasó de dos líneas de texto a una portada de marca y tres accesos
  rápidos.
- **Productos** agrupa el formulario en una tarjeta, pone **precio y stock lado
  a lado** y muestra el inventario en tarjetas con el badge **"Reponer"** cuando
  `producto.requiereReposicion` es cierto — la regla de negocio de la Parte 2
  ahora se ve en pantalla.
- **Clientes** recibe el mismo tratamiento visual (su arquitectura no se tocó:
  la actividad solo pedía refactorizar Productos).
- **Pedidos** pasó de un texto suelto a un `EstadoVacio` con explicación.

> **Formatear soles sin `String.format`.** Kotlin común no tiene `String.format`.
> Los dos decimales se arman a mano:
>
> ```kotlin
> private fun Double.enSoles(): String {
>     val centavos = (this * 100).roundToLong()
>     val enteros = centavos / 100
>     val decimales = (centavos % 100).toString().padStart(2, '0')
>     return "S/ $enteros.$decimales"
> }
> ```

---

## Cómo verificar todo

### Compilar y probar

```bash
# Android
./gradlew :androidApp:assembleDebug

# Pruebas del módulo compartido (19 pruebas)
./gradlew :shared:testAndroidHostTest

# El código de iOS SÍ compila desde Windows (solo el enlazado del
# framework necesita macOS)
./gradlew :shared:compileKotlinIosSimulatorArm64
```

### Ejecutar en el emulador

```bash
# Listar y arrancar un emulador
$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe -list-avds
$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe -avd Medium_Phone

# Instalar y lanzar
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n pe.edu.upeu.pharmamobil/.MainActivity

# Comprobar que arrancó (si Koin fallara, aquí no habría proceso)
adb shell pidof pe.edu.upeu.pharmamobil
```

### Forzar los cuatro estados para las capturas

Los estados *Cargando* y *Con productos* salen solos. Para los otros dos,
modifica **temporalmente** `ProductoRepositorioEnMemoria`:

```kotlin
// Estado "Sin productos"
override suspend fun listar(): List<Producto> {
    delay(RETARDO_LISTADO_MS)
    return emptyList()               // ← temporal
}

// Estado "Error"
override suspend fun listar(): List<Producto> {
    delay(RETARDO_LISTADO_MS)
    throw IllegalStateException("Sin conexión con el inventario")   // ← temporal
}
```

**Acuérdate de revertirlo** antes de confirmar los cambios.

---

## Lista de cotejo: dónde se cumple cada criterio

| N.° | Criterio | Dónde |
|---|---|---|
| 1 | Paquetes `domain/repository`, `domain/usecase`, `data/repository`, `di` | Árbol de la sección 1 |
| 2 | Ninguna clase de `domain` importa Compose/Ktor/Android/iOS | Parte 2 |
| 3 | `Producto` con una regla de negocio propia | `requiereReposicion` / `STOCK_MINIMO` |
| 4 | `ProductoRepository` interfaz en `domain`, implementación en `data`, `suspend` | Partes 2.2 y 3 |
| 5 | El id lo asigna el repositorio | Parte 3 + test `elIdLoAsignaElRepositorioNoLaPantalla` |
| 6 | `RegistrarProductoUseCase` con las reglas y `Result` | Parte 2.3 |
| 7 | `ProductoUiState` con cuatro fases + formulario | Parte 4.1 |
| 8 | `StateFlow` de solo lectura con `asStateFlow()` | Parte 4.2 |
| 9 | `ProductoScreen` sin `remember` de negocio ni validador | Parte 4.3 |
| 10 | Koin ensambla todo; repositorio como `single` | Parte 5.2 + `AppModuleTest` |
| 11 | Manifiesto con `.MainApplication` e `initKoinIos()` en Swift | Partes 5.4 y 5.5 |
| 12 | La app arranca y opera en ambas plataformas | Sección *Cómo verificar* |
| 13 | Pruebas sin fallos y cambios versionados | 19 pruebas · rama `feature/clean-mvvm` |

---

## Problemas que te puedes encontrar

### Koin no resuelve una dependencia

Revisa **en este orden**:

1. Que el módulo esté incluido en `initKoin()`.
2. Que la definición use el tipo de la **interfaz** (`single<ProductoRepository>`)
   y no el de la implementación.
3. Que el `AndroidManifest.xml` declare `android:name=".MainApplication"`.

### `KoinContext is deprecated`

```
w: 'fun KoinContext(...)' is deprecated. KoinContext is not needed anymore.
```

Es solo una advertencia: en Koin 4.2 el contexto ya se configura con
`startKoin()`. Se mantiene porque **la actividad lo pide explícitamente** en la
instrucción 11. Funciona igual.

### El `@Preview` de `MainActivity` falla al renderizar

`AppAndroidPreview()` llama a `App()`, que ahora necesita Koin iniciado — y las
vistas previas no pasan por `MainApplication`. Para arreglarlo habría que
envolver la vista previa en `KoinApplication { modules(...) }`. No afecta a la
app ejecutándose.

### El enlazado de iOS no funciona en Windows

```
> Task :shared:linkDebugFrameworkIosSimulatorArm64 SKIPPED
```

Es lo esperado: **compilar** el código de iOS sí funciona en Windows, pero
**enlazar el framework** exige macOS con Xcode. Para las capturas de iOS
necesitas una Mac.

### Git avisa `LF will be replaced by CRLF`

El repositorio guarda LF y Windows usa CRLF. Es inofensivo. Si te molesta,
un `.gitattributes` con `* text=auto eol=crlf` fija el criterio de una vez.
