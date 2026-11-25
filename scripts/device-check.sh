#!/usr/bin/env bash
# Se ejecuta dentro del emulador de GitHub Actions:
#  1. Pruebas de interfaz (Compose) sobre la versión debug.
#  2. Prueba de humo del APK final (release, con R8): se instala, se elige como pantalla de inicio,
#     se recorre y se guardan capturas. Falla si la app se cae.
set -uo pipefail

OUT="device-output"
PKG="app.ordena"
mkdir -p "$OUT"
status=0

shot() {
  adb exec-out screencap -p > "$OUT/$1.png" || echo "No se pudo capturar $1"
}

# Busca un elemento por texto o descripción (exacto o contenido) y lo toca. Con "long" mantiene presionado.
tap() {
  local label="$1" mode="${2:-tap}"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1
  local coords
  coords=$(python3 - "$label" "$OUT/ui.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
label, path = sys.argv[1], sys.argv[2]
try:
    nodes = list(ET.parse(path).iter("node"))
except Exception:
    sys.exit(0)
def center(n):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return f"{(x1 + x2) // 2} {(y1 + y2) // 2}"
for exact in (True, False):
    for n in nodes:
        for attr in ("text", "content-desc"):
            value = n.get(attr) or ""
            if (value == label) if exact else (label in value):
                c = center(n)
                if c:
                    print(c)
                    sys.exit(0)
PY
)
  if [ -z "$coords" ]; then
    echo "No encontré «$label» en pantalla"
    return 1
  fi
  if [ "$mode" = "long" ]; then
    # shellcheck disable=SC2086
    adb shell input swipe $coords $coords 900
  else
    # shellcheck disable=SC2086
    adb shell input tap $coords
  fi
}

echo "::group::Pruebas de interfaz (debug)"
if ! ./gradlew --no-daemon --stacktrace connectedDebugAndroidTest; then
  echo "::error::Fallaron las pruebas de interfaz"
  status=1
fi
cp -r app/build/reports/androidTests "$OUT/reporte-pruebas-interfaz" 2>/dev/null || true
echo "::endgroup::"

echo "::group::Prueba del APK final (release)"
adb uninstall "$PKG" >/dev/null 2>&1 || true
if ! adb install -r app/build/outputs/apk/release/app-release.apk; then
  echo "::error::No se pudo instalar el APK release"
  exit 1
fi
adb shell cmd role add-role-holder android.app.role.HOME "$PKG" || echo "No se pudo fijar como inicio"
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 6
shot 01-bienvenida
tap "Empezar" && sleep 3
shot 02-inicio

# El botón de inicio debe traer a Ordena (es la app de inicio).
adb shell input keyevent KEYCODE_HOME && sleep 3
shot 03-boton-inicio
adb shell dumpsys activity activities | grep -m1 -E "topResumedActivity|mResumedActivity" | tee "$OUT/actividad-arriba.txt"
if ! grep -q "$PKG" "$OUT/actividad-arriba.txt"; then
  echo "::error::El botón de inicio no llevó a Ordena"
  status=1
fi

tap "Ajustes" && sleep 2
shot 04-ajustes
tap "Categorías" && sleep 2
tap "Escribe una categoría" && sleep 1
adb shell input text "bancos" && sleep 2
shot 05-categorias-bancos
adb shell input keyevent KEYCODE_BACK && sleep 1
adb shell input keyevent KEYCODE_HOME && sleep 2

tap "Herramientas" && sleep 2
shot 06-carpeta
tap "Settings" long && sleep 2
shot 07-mover
adb shell input keyevent KEYCODE_BACK && sleep 1
adb shell input keyevent KEYCODE_HOME && sleep 2

tap "Busca una app o una categoría" && sleep 1
adb shell input text "juegos" && sleep 2
shot 08-busqueda
adb shell input keyevent KEYCODE_BACK && sleep 1
adb shell input keyevent KEYCODE_HOME && sleep 2

adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|ANR in $PKG" "$OUT/logcat.txt"; then
  echo "::error::La app se cayó o se congeló (ver logcat.txt)"
  status=1
fi
echo "::endgroup::"

exit $status
