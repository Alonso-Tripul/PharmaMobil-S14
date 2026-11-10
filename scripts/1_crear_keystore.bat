@echo off
REM ==== Sesion 14 - Paso 2: crear el almacen de claves (UNA sola vez) ====
REM Uso: scripts\1_crear_keystore.bat 7     (7 = numero de tu equipo)
setlocal
set N=%1
if "%N%"=="" set /p N=Numero de equipo: 
set KT=keytool
if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" set KT="C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\keytool.exe" set KT="%JAVA_HOME%\bin\keytool.exe"
if not exist "%USERPROFILE%\claves" mkdir "%USERPROFILE%\claves"
if exist "%USERPROFILE%\claves\equipo%N%.jks" ( echo Ya existe %USERPROFILE%\claves\equipo%N%.jks - no se sobrescribe. & goto props )
echo.
echo Te pedira: contrasena del almacen, tu nombre, unidad (ej. EP Ing. Sistemas),
echo organizacion (UPeU), ciudad, region y pais (PE). Luego confirma con "si".
echo.
%KT% -genkeypair -v -keystore "%USERPROFILE%\claves\equipo%N%.jks" -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 -alias equipo%N%
if errorlevel 1 ( echo ERROR: no se pudo crear el almacen. & exit /b 1 )
:props
if exist keystore.properties ( echo keystore.properties ya existe. & goto fin )
set /p SP=Escribe otra vez la contrasena del ALMACEN (para keystore.properties): 
set /p KP=Contrasena de la CLAVE (si diste Enter al crearla, es la misma): 
if "%KP%"=="" set KP=%SP%
(
echo storeFile=%USERPROFILE:\=/%/claves/equipo%N%.jks
echo storePassword=%SP%
echo keyAlias=equipo%N%
echo keyPassword=%KP%
) > keystore.properties
echo keystore.properties creado (esta en .gitignore, no se sube a Git).
:fin
echo.
echo IMPORTANTE: copia %USERPROFILE%\claves\equipo%N%.jks al Drive del equipo o a un USB.
pause
