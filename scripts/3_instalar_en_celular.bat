@echo off
REM ==== Sesion 14 - Paso 7: instalar el APK firmado en el celular conectado por USB ====
REM Uso: scripts\3_instalar_en_celular.bat 7
set N=%1
if "%N%"=="" set /p N=Numero de equipo: 
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
"%ADB%" devices -l
echo Si tenias la version de depuracion instalada, se desinstala primero (firma distinta).
"%ADB%" uninstall pe.edu.upeu.pharmamobil >nul 2>&1
"%ADB%" install entrega\Equipo%N%_ProyectoFinal_Android.apk
echo.
echo Datos del dispositivo para la matriz de pruebas:
"%ADB%" shell getprop ro.product.manufacturer
"%ADB%" shell getprop ro.product.model
"%ADB%" shell getprop ro.build.version.release
pause
