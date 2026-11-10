## Sesión 14 · Firma y builds de entrega

La firma lee las credenciales de `keystore.properties` (raíz). **Ese archivo y el `.jks` no están en Git**
(ver `.gitignore`). Sin ese archivo el proyecto abre y compila `debug` normalmente.

| Clave de `keystore.properties` | Contenido |
|---|---|
| `storeFile` | Ruta absoluta al `.jks`, fuera del repo |
| `storePassword` | Contraseña del almacén (la tiene el responsable; no se escribe aquí) |
| `keyAlias` | `equipoN` |
| `keyPassword` | Contraseña de la clave |

Plantilla: `keystore.properties.example`.

**Generar la entrega (Windows):**
1. `scripts\1_crear_keystore.bat N`: solo la primera vez.
2. `scripts\2_generar_builds.bat N`: genera `entrega\EquipoN_ProyectoFinal_Android.aab` y `.apk`, y `verificacion_firma.txt`.
3. `scripts\3_instalar_en_celular.bat N`: instala en el celular por USB.

**URL del backend:** se pasa en la compilación con `-Ppharmasoft.url=http://IP:8080/api/v1/`, o con
`pharmasoft.url=` en `local.properties`. Sin ella se usa `http://10.0.2.2:8080/api/v1/` (emulador).

| Versión | Valor |
|---|---|
| applicationId | `pe.edu.upeu.pharmamobil` |
| versionCode / versionName | 1 / 1.0.0 |
| Etiqueta | `v1.0-final` |
