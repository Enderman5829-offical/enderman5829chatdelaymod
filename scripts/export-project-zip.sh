#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
VERSION="$(awk -F= '/^mod_version=/{print $2}' gradle.properties | tr -d '[:space:]')"
OUT_DIR="build/download"
OUT_ZIP="${OUT_DIR}/chatdelaymod-project-${VERSION}.zip"
mkdir -p "$OUT_DIR"
rm -f "$OUT_ZIP"
while IFS= read -r -d '' file; do zip -q "$OUT_ZIP" "$file"; done < <(git ls-files -z)
if [[ -f "build/download/chatdelaymod-${VERSION}.jar" ]]; then zip -q "$OUT_ZIP" "build/download/chatdelaymod-${VERSION}.jar"; fi
echo "Exported: $OUT_ZIP"
sha256sum "$OUT_ZIP"
