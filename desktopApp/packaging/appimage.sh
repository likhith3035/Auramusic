#!/usr/bin/env bash
#
# Wraps the app image jpackage produced into an AppImage, the one Linux format
# Gradle cannot make itself.
#
# Usage: desktopApp/packaging/appimage.sh <app-image-dir> <out.AppImage> [appimagetool]
set -euo pipefail

APP_DIR_SRC=${1:?usage: appimage.sh <app-image-dir> <out.AppImage> [appimagetool]}
OUT=${2:?usage: appimage.sh <app-image-dir> <out.AppImage> [appimagetool]}
TOOL=${3:-appimagetool}

LAUNCHER_NAME=""
if [ -x "$APP_DIR_SRC/bin/AuraMusic" ]; then
  LAUNCHER_NAME="AuraMusic"
elif [ -x "$APP_DIR_SRC/bin/BitChord" ]; then
  LAUNCHER_NAME="BitChord"
else
  echo "no launcher in $APP_DIR_SRC" >&2
  exit 1
fi

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
APPDIR="$STAGE/AuraMusic.AppDir"

mkdir -p "$APPDIR/usr"
cp -a "$APP_DIR_SRC/." "$APPDIR/usr/"

# The mount point differs every run, so nothing may be hard-coded.
cat > "$APPDIR/AppRun" <<APPRUN
#!/bin/sh
HERE="\$(dirname "\$(readlink -f "\$0")")"
exec "\$HERE/usr/bin/$LAUNCHER_NAME" "\$@"
APPRUN
chmod +x "$APPDIR/AppRun"

cat > "$APPDIR/auramusic.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=AuraMusic
GenericName=Music Player
Comment=Aesthetic YouTube Music client
Exec=$LAUNCHER_NAME
Icon=auramusic
Categories=AudioVideo;Audio;Player;
Terminal=false
StartupWMClass=com-music-bitchord-desktop-MainKt
DESKTOP

# appimagetool reads the icon by the desktop entry; the desktop reads .DirIcon.
cp "$ROOT/desktopApp/packaging/icons/AppIcon.png" "$APPDIR/auramusic.png"
cp "$ROOT/desktopApp/packaging/icons/AppIcon.png" "$APPDIR/.DirIcon"

mkdir -p "$(dirname "$OUT")"
# APPIMAGE_EXTRACT_AND_RUN rather than the flag of the same name: it is read by the AppImage
# runtime, so it covers a CI runner with no FUSE, and a natively built appimagetool — which has
# no such flag and would refuse to start — simply ignores it.
ARCH=x86_64 APPIMAGE_EXTRACT_AND_RUN=1 "$TOOL" "$APPDIR" "$OUT"
echo "appimage: $OUT"
