# PharmaMobil 1.0.0 · Guía de instalación (Android)

## 1. Levantar el backend (en la PC)
1. Abre **Docker Desktop** y arranca el contenedor de Oracle (puerto 1522).
2. Ejecuta **PharmaSoft** (`mvnw spring-boot:run`) y comprueba `http://localhost:8080/swagger-ui.html`.
3. Una sola vez, en CMD como administrador, abre el puerto:
   `netsh advfirewall firewall add rule name="PharmaSoft 8080" dir=in action=allow protocol=TCP localport=8080`
4. Conecta la PC y el celular a la **misma red Wi-Fi**. En el navegador del celular abre
   `http://<IP-de-la-PC>:8080/swagger-ui.html`: si carga, la app también conectará.

## 2. Instalar la app
- **Con cable:** activa *Opciones de desarrollador › Depuración USB*, conecta y ejecuta `scripts\3_instalar_en_celular.bat N`.
- **Sin cable:** envía `EquipoN_ProyectoFinal_Android.apk` por Drive/WhatsApp (como documento), ábrelo y permite «Instalar apps desconocidas».
- Si ya tenías la versión de depuración, **desinstálala antes**: la firma es distinta y Android rechaza la actualización.

## 3. Credenciales de prueba
| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | | |
| Sin privilegios | | |

## 4. Problemas frecuentes
| Síntoma | Solución |
|---|---|
| «App no instalada» | Desinstala la versión anterior (firma distinta) |
| «No se pudo conectar al servidor» | Misma Wi-Fi, firewall abierto, IP de la PC no cambió (si cambió, vuelve a generar el build) |
| La app se cierra al abrir una pantalla | Falta una regla R8: ver `androidApp/proguard-rules.pro` |
| El .aab no instala | Es normal: el .aab es para la tienda; se instala el .apk |
