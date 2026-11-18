@echo off
setlocal
title AuraMusic Desktop
echo Starting AuraMusic Desktop with full-length streaming engine...
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0desktopApp\build\windows-portable\BitChord"
call AuraMusic.bat %*
endlocal
