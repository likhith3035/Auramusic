# ====================================================================
# AuraMusic - All-in-One APK Build & Sign Script
# ====================================================================
$version = "3.2.1"

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

$buildTools = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat"
$keystore = "$env:USERPROFILE\.android\debug.keystore"
$releaseDir = Join-Path $PSScriptRoot "release"

if (-not (Test-Path $releaseDir)) {
    New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

function Sign-Apk($src, $dest) {
    if (Test-Path $src) {
        Write-Host "Copying to $dest ..." -ForegroundColor Cyan
        Copy-Item $src $dest -Force
        Write-Host "Signing $dest ..." -ForegroundColor Cyan
        & $buildTools sign --ks $keystore --ks-pass pass:android --key-pass pass:android $dest
        & $buildTools verify $dest
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Verified signature for $dest" -ForegroundColor Green
        }
    } else {
        Write-Host "Source APK not found: $src" -ForegroundColor Red
    }
}

Write-Host "`n>>> Building Production Release APKs for v$version..." -ForegroundColor Cyan
.\gradlew.bat :app:assembleProdRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

$arm64Src = Join-Path $PSScriptRoot "app\build\outputs\apk\prod\release\AuraMusic-v$version-arm64-v8a.apk"
$armv7Src = Join-Path $PSScriptRoot "app\build\outputs\apk\prod\release\AuraMusic-v$version-armeabi-v7a.apk"
$x8664Src = Join-Path $PSScriptRoot "app\build\outputs\apk\prod\release\AuraMusic-v$version-x86_64.apk"
$univSrc = Join-Path $PSScriptRoot "app\build\outputs\apk\prod\release\AuraMusic-v$version-universal.apk"

Sign-Apk $arm64Src (Join-Path $releaseDir "AuraMusic-v$version-arm64-v8a.apk")
Sign-Apk $armv7Src (Join-Path $releaseDir "AuraMusic-v$version-armeabi-v7a.apk")
Sign-Apk $x8664Src (Join-Path $releaseDir "AuraMusic-v$version-x86_64.apk")
Sign-Apk $univSrc (Join-Path $releaseDir "AuraMusic-v$version-universal.apk")

# Copy signed universal APK to repository root
Copy-Item (Join-Path $releaseDir "AuraMusic-v$version-universal.apk") (Join-Path $PSScriptRoot "AuraMusic-v$version-universal.apk") -Force

# Copy desktop portable zip to release directory if present
$desktopZip = Join-Path $PSScriptRoot "desktopApp\build\distributions\AuraMusic-$version-windows-portable.zip"
if (Test-Path $desktopZip) {
    Copy-Item $desktopZip (Join-Path $releaseDir "AuraMusic-$version-windows-portable.zip") -Force
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "Production APKs updated in $releaseDir" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Get-ChildItem -Path $releaseDir -Filter "AuraMusic-v$version*.apk" | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
