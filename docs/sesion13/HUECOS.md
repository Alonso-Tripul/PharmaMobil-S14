# Sesión 13 · Paso 1 · Huecos del flujo completo

Hallazgos de la revisión del código de la sesión 11. Al recorrer la app en el
emulador, confirma cada fila y completa la columna **Evidencia** con el número
de la captura del Producto A.

Gravedad: **Alta** deja al usuario sin salida o pierde datos · **Media** confunde
o da un mensaje incompleto · **Baja** es cosmética.

| N.º | Momento | Hueco encontrado | Gravedad | Decisión | Evidencia |
|---|---|---|---|---|---|
| 1 | Primera apertura sin sesión | La app abre el login y no deja pasar sin credenciales. Desde ahí se puede intentar el ingreso o cerrar la app. **Sin hueco.** | — | Sin cambios | |
| 2 | Login con credenciales incorrectas | El mensaje ya distingue el 401 («Usuario o contraseña incorrectos») de la falta de red («No se pudo conectar al servidor…»). **Sin hueco.** | — | Sin cambios | |
| 3 | Listado con la base vacía | Ya existe un estado vacío propio: «Todavía no hay productos». **Sin hueco.** | — | Sin cambios | |
| 4 | Creación en modo avión | Se guarda y aparece como «Pendiente», pero la fila no recuerda cuántas veces se intentó enviar ni por qué falló. | Media | **Se cierra hoy** (paso 2: columnas `intentos` y `ultimoError`) | |
| 5a | Sincronización al recuperar señal | No sincroniza sola al volver la red. El usuario tiene que pulsar «Sincronizar». | Media | **Se cierra hoy** (`MonitorRed` dispara la sincronización al reconectar) | |
| 5b | Sincronización con una fila inválida | El bucle anterior se detenía en la primera fila que fallaba. Una fila con 409 (nombre repetido) bloqueaba para siempre las demás y la descarga. | **Alta** | **Se cierra hoy** (paso 3: fallo permanente marca la fila y sigue con la siguiente) | |
| 5c | Reintento de una creación | Si el POST se envió pero la respuesta se perdió (tiempo agotado), reintentar duplica el producto o devuelve 409. | **Alta** | **Se cierra hoy** (clave de operación `Idempotency-Key` en la app y en PharmaSoft) | |
| 6 | Error de validación del servidor | El aviso decía solo «Sin conexión o error del servidor». No se distinguía el 400 ni el 409 y el motivo no llegaba al usuario. | Media | **Se cierra hoy en parte**: el motivo del servidor se guarda en `ultimoError` y se muestra en «Revisar». Llevar el error a cada campo del formulario queda para la **sesión 14**. | |
| 7 | Token vencido a mitad de un formulario | El 401 cierra la sesión y vuelve al login. Lo escrito en el formulario se pierde. En la sincronización, el 401 no debe marcar las filas como fallidas. | Media | **Se cierra hoy** la parte de sincronización: `ResultadoEnvio.SesionVencida` detiene la cola sin tocar las filas. Conservar el borrador del formulario queda para la **sesión 14**. | |
| 8 | Cierre de sesión | El token se borra del almacén cifrado y la caché Bearer se limpia. Las filas locales quedan en el equipo, separadas por usuario (`propietario`). | Baja | **Se descarta con justificación**: las filas de otro usuario no se muestran, y borrarlas perdería pendientes que aún no se enviaron. | |
| 9 | Fallos sin registro | Ningún error llega a un servicio de telemetría. Un fallo en el teléfono de un usuario es invisible para el equipo. | Media | **Se cierra hoy** (paso 6: `Telemetria` con Firebase Crashlytics y Analytics) | |
| 10 | Avisos al usuario | No hay manera de avisar que hay productos que no se enviaron si el usuario no abre la pantalla. | Baja | **Se cierra hoy** (paso 5: permiso de notificaciones con sus tres estados) | |
