#!/usr/bin/env bash
# Muestra en GitHub (como anotaciones) las líneas clave de un registro de Gradle que falló.
f="${1:?registro}"
grep -E "^e: |FAILED|What went wrong|AssertionError|Exception:|error:" -A2 "$f" \
  | grep -v -E "^--$" | head -40 | while IFS= read -r line; do
  echo "::error::${line}"
done
