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

# Busca un elemento por texto o descripción (exacto o contenido) en la pantalla actual e imprime su centro.
find_center() {
  local label="$1"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1
  python3 - "$label" "$OUT/ui.xml" <<'PY'
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
}

# Si OTRA app muestra "no responde" o "se detuvo" (pasa con apps del emulador), se cierra el aviso.
# Los avisos de Ordena no se tocan: los detecta el control de logcat al final.
dismiss_foreign_dialogs() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1
  local button=""
  if grep -q "isn't responding" "$OUT/ui.xml" 2>/dev/null && ! grep -q "Ordena isn't responding" "$OUT/ui.xml"; then
    button="Wait"
  elif grep -q "keeps stopping\|has stopped" "$OUT/ui.xml" 2>/dev/null && ! grep -q "Ordena keeps stopping\|Ordena has stopped" "$OUT/ui.xml"; then
    button="Close app"
  fi
  if [ -n "$button" ]; then
    echo "Se cerró un aviso del sistema de otra app ($button)" | tee -a "$OUT/avisos.txt"
    local c
    c=$(find_center "$button")
    # shellcheck disable=SC2086
    [ -n "$c" ] && adb shell input tap $c
    sleep 2
  fi
}

# Toca un elemento (con reintentos). Con "long" lo mantiene presionado.
tap() {
  local label="$1" mode="${2:-tap}" coords="" attempt
  for attempt in 1 2 3 4; do
    coords=$(find_center "$label")
    [ -n "$coords" ] && break
    dismiss_foreign_dialogs
    sleep 2
  done
  if [ -z "$coords" ]; then
    echo "No encontré «$label» en pantalla" | tee -a "$OUT/avisos.txt"
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

# Toca un botón hasta que desaparezca de la pantalla (confirma que el toque hizo efecto).
tap_until_gone() {
  local label="$1" attempt
  for attempt in 1 2 3; do
    dismiss_foreign_dialogs
    tap "$label" || return 1
    sleep 3
    if [ -z "$(find_center "$label")" ]; then
      return 0
    fi
    echo "«$label» sigue en pantalla tras el intento $attempt" | tee -a "$OUT/avisos.txt"
  done
  return 1
}

echo "::group::Pruebas de interfaz (debug)"
if ! ./gradlew --no-daemon --stacktrace connectedDebugAndroidTest 2>&1 | tee "$OUT/pruebas-interfaz.txt"; then
  echo "::error::Fallaron las pruebas de interfaz"
  bash scripts/report-errors.sh "$OUT/pruebas-interfaz.txt"
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
sleep 5
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 6
dismiss_foreign_dialogs
shot 01-bienvenida
if ! tap_until_gone "Empezar"; then
  echo "::error::En el APK final, el botón Empezar no llevó al inicio"
  status=1
fi
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
# Solo cuentan las caídas y congelamientos de Ordena (no los de otras apps del emulador).
if grep -A1 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep -q "Process: $PKG,"; then
  echo "::error::Ordena se cayó (ver logcat.txt)"
  status=1
fi
if grep -q "ANR in $PKG" "$OUT/logcat.txt"; then
  echo "::error::Ordena se congeló (ver logcat.txt)"
  status=1
fi
grep -A1 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep "Process:" | sed 's/^/Caída de otra app en el emulador: /' >> "$OUT/avisos.txt" || true
echo "::endgroup::"

exit $status
