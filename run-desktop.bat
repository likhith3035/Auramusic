@echo off
title AuraMusic Desktop
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
echo ========================================
echo       Starting AuraMusic Desktop
echo ========================================
call .\gradlew.bat :desktopApp:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Desktop App stopped with error.
    pause
)
