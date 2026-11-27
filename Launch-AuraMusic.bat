@echo off
setlocal
title AuraMusic Desktop
echo Starting AuraMusic Desktop with full-length streaming engine...
if not defined JAVA_HOME (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*" "%USERPROFILE%\.jdks\*21*") do if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
)
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined JAVA_HOME if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0desktopApp\build\windows-portable\AuraMusic"
if not exist "AuraMusic.bat" cd /d "%~dp0desktopApp\build\windows-portable\BitChord"
call AuraMusic.bat %*
endlocal
