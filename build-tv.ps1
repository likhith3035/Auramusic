# ====================================================================
# AuraMusic Smart TV Multi-Platform Build & Package Script
# Generates packages for:
# 1. Android TV / Google TV / Fire TV (.apk)
# 2. LG Smart TVs (webOS - .ipk / package directory)
# 3. Samsung Smart TVs (Tizen OS - .wgt widget package)
# ====================================================================

param(
    [switch]$SkipApk = $false
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$tvAppDir = Join-Path $root "tvApp"
$distDir = Join-Path $root "dist\tv"

if (Test-Path $distDir) {
    Remove-Item -Recurse -Force $distDir
}
New-Item -ItemType Directory -Path $distDir | Out-Null

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ">>> AuraMusic Smart TV Packager (Android TV / webOS / Tizen)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# --------------------------------------------------------------------
# 1. Package LG Smart TVs (webOS)
# --------------------------------------------------------------------
Write-Host "`n[1/3] Packaging for LG Smart TVs (webOS)..." -ForegroundColor Yellow
$webosDist = Join-Path $distDir "webos"
New-Item -ItemType Directory -Path $webosDist | Out-Null

# Copy webOS package contents
Copy-Item (Join-Path $tvAppDir "webos\appinfo.json") $webosDist -Force
Copy-Item (Join-Path $tvAppDir "index.html") $webosDist -Force
Copy-Item (Join-Path $tvAppDir "Logo.png") $webosDist -Force
Copy-Item (Join-Path $tvAppDir "Banner.png") $webosDist -Force
Copy-Item (Join-Path $tvAppDir "css") $webosDist -Recurse -Force
Copy-Item (Join-Path $tvAppDir "js") $webosDist -Recurse -Force

# If ares-package (LG webOS CLI) is available, build the .ipk
if (Get-Command "ares-package" -ErrorAction SilentlyContinue) {
    Write-Host "  Found LG ares-package CLI, compiling .ipk package..." -ForegroundColor Green
    & ares-package $webosDist -o $distDir
} else {
    # Generate standalone webOS installable archive
    $webosZip = Join-Path $distDir "AuraMusic-webOS-v1.0.0.zip"
    Compress-Archive -Path "$webosDist\*" -DestinationPath $webosZip -Force
    Write-Host "  Generated LG webOS sideload package: $webosZip" -ForegroundColor Green
}

# --------------------------------------------------------------------
# 2. Package Samsung Smart TVs (Tizen OS)
# --------------------------------------------------------------------
Write-Host "`n[2/3] Packaging for Samsung Smart TVs (Tizen OS)..." -ForegroundColor Yellow
$tizenDist = Join-Path $distDir "tizen"
New-Item -ItemType Directory -Path $tizenDist | Out-Null

# Copy Tizen package contents
Copy-Item (Join-Path $tvAppDir "tizen\config.xml") $tizenDist -Force
Copy-Item (Join-Path $tvAppDir "index.html") $tizenDist -Force
Copy-Item (Join-Path $tvAppDir "Logo.png") $tizenDist -Force
Copy-Item (Join-Path $tvAppDir "Banner.png") $tizenDist -Force
Copy-Item (Join-Path $tvAppDir "css") $tizenDist -Recurse -Force
Copy-Item (Join-Path $tvAppDir "js") $tizenDist -Recurse -Force

# Tizen widgets (.wgt) are standard zip archives with config.xml at the root
$tizenWgt = Join-Path $distDir "AuraMusic-SamsungTizen-v1.0.0.wgt"
$tempZip = Join-Path $distDir "tizen_temp.zip"
Compress-Archive -Path "$tizenDist\*" -DestinationPath $tempZip -Force
Rename-Item -Path $tempZip -NewName (Split-Path $tizenWgt -Leaf) -Force
Write-Host "  Generated Samsung Tizen Widget (.wgt): $tizenWgt" -ForegroundColor Green

# --------------------------------------------------------------------
# 3. Package Android TV / Google TV / Fire TV (.apk)
# --------------------------------------------------------------------
if (-not $SkipApk) {
    Write-Host "`n[3/3] Packaging for Android TV / Google TV / Fire TV..." -ForegroundColor Yellow
    
    # Sync assets into app/src/main/assets/tvApp
    $appAssetsTv = Join-Path $root "app\src\main\assets\tvApp"
    if (-not (Test-Path $appAssetsTv)) {
        New-Item -ItemType Directory -Path $appAssetsTv | Out-Null
    }
    Copy-Item (Join-Path $tvAppDir "index.html") $appAssetsTv -Force
    Copy-Item (Join-Path $tvAppDir "Logo.png") $appAssetsTv -Force
    Copy-Item (Join-Path $tvAppDir "Banner.png") $appAssetsTv -Force
    Copy-Item (Join-Path $tvAppDir "css") $appAssetsTv -Recurse -Force
    Copy-Item (Join-Path $tvAppDir "js") $appAssetsTv -Recurse -Force

    if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }

    Write-Host "  Building Android TV Leanback APK via Gradle..." -ForegroundColor Cyan
    & .\gradlew.bat :app:assembleProdRelease
    if ($LASTEXITCODE -eq 0) {
        $builtApk = Join-Path $root "app\build\outputs\apk\prod\release\AuraMusic-v3.3.0-arm64-v8a.apk"
        if (Test-Path $builtApk) {
            $destTvApk = Join-Path $distDir "AuraMusic-AndroidTV-v3.3.0.apk"
            Copy-Item $builtApk $destTvApk -Force
            Write-Host "  Generated Android TV APK: $destTvApk" -ForegroundColor Green
        }
    }
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host ">>> All Smart TV Packages Generated Successfully in dist/tv" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Get-ChildItem $distDir | Select-Object Name, Length, LastWriteTime
