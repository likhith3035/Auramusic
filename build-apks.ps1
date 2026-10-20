# ====================================================================
# AuraMusic - All-in-One APK Build & Sign Script
# ====================================================================
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

Write-Host "`n>>> Building Production Release APKs..." -ForegroundColor Cyan
.\gradlew.bat :app:assembleProdRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

$arm64Src = Join-Path $PSScriptRoot "app\build\outputs\apk\prod\release\app-prod-arm64-v8a-release-unsigned.apk"
$univSrc = Join-Path $PSScriptRoot "app\build\outputs\apk\prod\release\app-prod-universal-release-unsigned.apk"
Sign-Apk $arm64Src (Join-Path $releaseDir "AuraMusic-v3.2.0-arm64-v8a.apk")
Sign-Apk $univSrc (Join-Path $releaseDir "AuraMusic-v3.2.0-universal.apk")

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "Production APKs updated in $releaseDir" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Get-ChildItem -Path $releaseDir -Filter "AuraMusic-v3.2.0*.apk" | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
