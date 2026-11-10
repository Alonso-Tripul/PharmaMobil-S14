@echo off
REM ==== Sesion 14 - Paso 4: AAB + APK firmados, apuntando al backend de esta PC ====
REM Uso (desde la raiz del proyecto): scripts\2_generar_builds.bat 7
setlocal enabledelayedexpansion
set N=%1
if "%N%"=="" set /p N=Numero de equipo: 
if not exist keystore.properties ( echo Falta keystore.properties: ejecuta primero scripts\1_crear_keystore.bat & pause & exit /b 1 )
if exist "C:\Program Files\Android\Android Studio\jbr" if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

REM --- IP de esta PC en la red (la que usa el celular para llegar a PharmaSoft) ---
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1).IPv4Address.IPAddress"`) do set IP=%%i
echo IP detectada de esta PC: %IP%
set /p IPOK=Enter para usarla, o escribe otra IP: 
if not "%IPOK%"=="" set IP=%IPOK%
set URL=http://%IP%:8080/api/v1/
echo URL del backend en el build: %URL%
echo.

call gradlew.bat clean :androidApp:bundleRelease :androidApp:assembleRelease "-Ppharmasoft.url=%URL%"
if errorlevel 1 ( echo. & echo ERROR en la compilacion: copia el mensaje y pasaselo a Claude. & pause & exit /b 1 )

if not exist entrega mkdir entrega
copy /Y androidApp\build\outputs\bundle\release\androidApp-release.aab entrega\Equipo%N%_ProyectoFinal_Android.aab >nul
copy /Y androidApp\build\outputs\apk\release\androidApp-release.apk entrega\Equipo%N%_ProyectoFinal_Android.apk >nul

set SDK=%LOCALAPPDATA%\Android\Sdk
for /f "delims=" %%d in ('dir /b /ad /o:n "%SDK%\build-tools"') do set APKSIGNER=%SDK%\build-tools\%%d\apksigner.bat
echo.
echo ===== VERIFICACION DE FIRMA (toma captura de esto) =====
call "%APKSIGNER%" verify --verbose --print-certs entrega\Equipo%N%_ProyectoFinal_Android.apk > entrega\verificacion_firma.txt 2>&1
type entrega\verificacion_firma.txt
echo URL del backend: %URL% >> entrega\verificacion_firma.txt
echo.
echo Archivos listos en la carpeta entrega\
dir /b entrega
echo.
echo RECUERDA: abre el puerto 8080 en el firewall (una vez, como administrador):
echo   netsh advfirewall firewall add rule name="PharmaSoft 8080" dir=in action=allow protocol=TCP localport=8080
pause
