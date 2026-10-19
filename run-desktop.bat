@echo off
title AuraMusic Desktop
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
