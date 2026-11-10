# Sesión 14 · Mejoras finales, firma y builds (Android)

Proyecto: PharmaMobil (KMP) · Rama: `feature/builds-14` · Fecha: 10/11/2026 · Equipo: 7

---
## Paso 1 · Ficha de integración cerrada (desde `docs/sesion13/HUECOS.md`)

| N.º | Hueco | Decisión final S14 | Motivo / qué haría falta |
|---|---|---|---|
| 1 | Primera apertura sin sesión | **Cerrado** (sin hueco) | — |
| 2 | Login con credenciales incorrectas | **Cerrado** (sin hueco) | Distingue 401 de falta de red |
| 3 | Listado con base vacía | **Cerrado** (sin hueco) | Estado vacío propio |
| 4 | Creación en modo avión sin `intentos`/`ultimoError` | **Cerrado** 03/11 | Columnas añadidas en S13 |
| 5a | No sincroniza sola al volver la red | **Cerrado** 03/11 | `MonitorRed` |
| 5b | Fila inválida bloqueaba la cola | **Cerrado** 03/11 | Fallo permanente marca y sigue |
| 5c | Reintento duplicaba la creación | **Cerrado** 03/11 | `Idempotency-Key` |
| 6 | Error del servidor no llega a cada campo | **Se declara como limitación** | El motivo se muestra en «Revisar»; llevarlo a cada campo exige cambiar el formulario y sus pruebas, lo que no se puede probar dentro de la congelación. |
| 7 | Borrador del formulario se pierde con token vencido | **Se declara como limitación** | La cola ya no marca filas como fallidas (`SesionVencida`); guardar el borrador requiere persistencia nueva del formulario. |
| 8 | Filas locales tras cerrar sesión | **Descartado** con justificación | Separadas por `propietario`; borrarlas perdería pendientes. |
| 9 | Fallos sin registro | **Cerrado** 03/11 | Crashlytics + Analytics |
| 10 | Avisos al usuario | **Cerrado** 03/11 | Notificaciones con tres estados |
| S14-a | URL fija `10.0.2.2` (solo emulador) | **Cerrado hoy** | URL inyectada desde Gradle (`pharmasoft.url`) por `<meta-data>`; el script usa la IP de la PC. |
| S14-b | Release sin permiso de HTTP (solo debug lo tenía) | **Cerrado hoy** | `network_security_config.xml` en `main`. |
| S14-c | HTTP sin cifrar hacia PharmaSoft | **Se declara como limitación** | El servicio corre en HTTP en la red local; en producción iría por HTTPS y se quitaría el permiso. |
| S14-d | iOS: firma y archivo | **Se declara como limitación** | El equipo no dispone de macOS/Xcode durante la sesión. |

**Congelamiento del alcance.** Desde el commit `_______` del 10/11/2026 no entra funcionalidad nueva,
cambios de arquitectura o librerías, rediseños ni migraciones del esquema. Solo se corrigen defectos del Paso 7.

**Trabajo futuro (fuera de alcance):** HTTPS en PharmaSoft, error por campo, borrador persistente, publicación en Play Store.

---
## Paso 2 · Almacén de claves

| Dato | Valor |
|---|---|
| Archivo / alias | `equipo7.jks` / `equipo7` |
| Ubicación | `C:\Users\Rony\claves\` (fuera del repo) |
| Titular (CN, OU, O, L, C) | CN=Rony Ancajima, OU=Ep Ingenieria Sistemas, O=UpeU, L=Lima, ST=Lima, C=PE |
| Algoritmo / validez | RSA 2048, firma APK v2 / 10000 días |
| Copia de respaldo | _(Drive del equipo / USB — sin contraseñas)_ |
| Responsable de la entrega | |

---
## Paso 4 · Ficha de versión

| Dato | Valor |
|---|---|
| applicationId | `pe.edu.upeu.pharmamobil` |
| versionCode / versionName | 1 / 1.0.0 |
| URL del backend en el build | `http://10.40.28.19:8080/api/v1/` |
| Huella SHA-256 del certificado | `8e9e77a9f03a551fd0663f9579de5cb972dddb2bcb337fc209c747e82abd9942` |
| AAB | `Equipo7_ProyectoFinal_Android.aab` |
| APK | `Equipo7_ProyectoFinal_Android.apk` |
| Etiqueta Git | `v1.0-final` → commit `_______` |

---
## Paso 5 · Minificación (R8 activo)

| Prueba en la versión release | ¿Funciona? | Regla añadida si falló |
|---|---|---|
| Login contra PharmaSoft (Ktor + JSON) | | |
| Lista de productos (`JsonObject` → `Producto`) | | |
| Pantallas con ViewModel por Koin | | |
| Base local SQLDelight / modo avión | | |
| Crashlytics registra (consola de Firebase) | | |

Decisión: ☐ Minificación activa  ☐ Desactivada (motivo: ________________)

---
## Paso 7 · Matriz de pruebas en dispositivos reales

| ID | Marca y modelo | Android | Gama | Integrante |
|---|---|---|---|---|
| DV-1 | | | | |
| DV-2 | | | | |
| DV-3 (autónoma) | | | | |

| # | Paso del recorrido | Qué se observa | DV-1 | DV-2 | DV-3 |
|---|---|---|---|---|---|
| 1 | Instalación desde el APK firmado | Instala sin Android Studio; nombre e ícono definitivos | | | |
| 2 | Login correcto e incorrecto | Distingue credencial inválida de falta de conexión | | | |
| 3 | CRUD completo + un rechazo de validación | El motivo del servidor llega al usuario (ver limitación 6) | | | |
| 4 | Operación sin privilegio | Informa el rechazo (403) y no cierra la sesión | | | |
| 5 | Modo avión: abrir, leer, crear | Sigue mostrando datos; lo creado queda «Pendiente» | | | |
| 6 | Volver la red | Sincroniza sola, sin duplicados, refresca la lista | | | |
| 7 | Notificaciones: conceder, denegar, denegar siempre | Tres estados; el tercero ofrece Ajustes | | | |
| 8 | Cerrar sesión y reabrir | Token borrado; pide login | | | |

**Registro de defectos**

| ID | Dispositivo y versión | Pasos para reproducir | Esperado | Obtenido | Severidad | Estado |
|---|---|---|---|---|---|---|
| D-01 | | 1. Abrir la app… | | | Alta/Media/Baja | Corregido/Pendiente/Descartado |

---
## Lista de cotejo

| N.º | Criterio | Sí/No |
|---|---|---|
| 1 | Ficha de integración cerrada y alcance congelado | Sí |
| 2 | Almacén creado y con copia fuera de un solo equipo | |
| 3 | Ni almacén ni contraseñas en el repo; .gitignore lo impide | Sí |
| 4 | El proyecto compila sin keystore.properties | Sí (firma condicional `hayFirma`) |
| 5 | AAB firmado generado sin errores | Sí |
| 6 | APK de entrega instalado en dispositivo real | |
| 7 | Verificación muestra titular y huella | Sí |
| 8 | Release recorrida completa; serialización e inyección OK | |
| 9 | Si se desactivó la minificación, motivo escrito | |
| 10 | iOS: límite documentado (S14-d) | Sí |
| 11 | Probado en 2 dispositivos reales distintos | |
| 12 | Cada defecto con ID, pasos, severidad y estado | |
| 13 | versionCode, versionName e ID coinciden con el archivo | |
| 14 | Commits con convención y distribuidos | |
