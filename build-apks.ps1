# ====================================================================
# AuraMusic - All-in-One APK Build & Sign Script
# ====================================================================
param(
    [string]$KeystorePath = $env:RELEASE_KEYSTORE_PATH,
    [string]$KeystorePass = $env:RELEASE_KEYSTORE_PASSWORD,
    [string]$KeyAlias = $env:RELEASE_KEY_ALIAS,
    [string]$KeyPass = $env:RELEASE_KEY_PASSWORD,
    [switch]$AllowDebugSignForLocalTest
)

$version = "3.3.0"

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    $userJbr = Get-ChildItem "$env:USERPROFILE\.jdks" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    if (Test-Path "$jbr\bin\java.exe") {
        $env:JAVA_HOME = $jbr
    } elseif ($userJbr -and (Test-Path "$userJbr\bin\java.exe")) {
        $env:JAVA_HOME = $userJbr
    }
    if ($env:JAVA_HOME) {
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
}

$buildToolsDir = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Where-Object { Test-Path (Join-Path $_.FullName "apksigner.bat") } |
    Select-Object -First 1
if (-not $buildToolsDir) {
    Write-Host "Android SDK build-tools with apksigner.bat not found under $env:LOCALAPPDATA\Android\Sdk\build-tools" -ForegroundColor Red
    exit 1
}
$buildTools = Join-Path $buildToolsDir.FullName "apksigner.bat"

if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    if ($AllowDebugSignForLocalTest) {
        $KeystorePath = "$env:USERPROFILE\.android\debug.keystore"
        if ([string]::IsNullOrWhiteSpace($KeystorePass)) { $KeystorePass = "android" }
        if ([string]::IsNullOrWhiteSpace($KeyPass)) { $KeyPass = "android" }
        Write-Host "WARNING: Using local debug keystore for unsigned local test packaging only." -ForegroundColor Yellow
    } else {
        Write-Host "Refusing to sign release APKs without a release keystore. Supply -KeystorePath / RELEASE_KEYSTORE_PATH (or pass -AllowDebugSignForLocalTest for local-only testing)." -ForegroundColor Red
        exit 1
    }
}
if (-not (Test-Path $KeystorePath)) {
    Write-Host "Keystore not found at $KeystorePath" -ForegroundColor Red
    exit 1
}
if ([string]::IsNullOrWhiteSpace($KeystorePass)) {
    Write-Host "Missing keystore password (-KeystorePass or RELEASE_KEYSTORE_PASSWORD)." -ForegroundColor Red
    exit 1
}
if ([string]::IsNullOrWhiteSpace($KeyPass)) { $KeyPass = $KeystorePass }

$releaseDir = Join-Path $PSScriptRoot "release"
if (-not (Test-Path $releaseDir)) {
    New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

function Sign-Apk($src, $dest) {
    if (-not (Test-Path $src)) {
        Write-Host "Source APK not found: $src" -ForegroundColor Red
        exit 1
    }
    Write-Host "Copying to $dest ..." -ForegroundColor Cyan
    Copy-Item $src $dest -Force
    Write-Host "Signing $dest ..." -ForegroundColor Cyan
    $signArgs = @("sign", "--ks", $KeystorePath, "--ks-pass", "pass:$KeystorePass", "--key-pass", "pass:$KeyPass")
    if (-not [string]::IsNullOrWhiteSpace($KeyAlias)) {
        $signArgs += @("--ks-key-alias", $KeyAlias)
    }
    $signArgs += $dest
    & $buildTools @signArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Host "apksigner sign failed for $dest (exit $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    & $buildTools verify $dest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "apksigner verify failed for $dest (exit $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "Verified signature for $dest" -ForegroundColor Green
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

# Copy desktop portable zip to release directory if present
$desktopZip = Join-Path $PSScriptRoot "desktopApp\build\distributions\AuraMusic-$version-windows-portable.zip"
if (Test-Path $desktopZip) {
    Copy-Item $desktopZip (Join-Path $releaseDir "AuraMusic-$version-windows-portable.zip") -Force
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "Production APKs updated in $releaseDir" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Get-ChildItem -Path $releaseDir -Filter "AuraMusic-v$version*.apk" | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
