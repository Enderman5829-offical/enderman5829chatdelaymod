#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
VERSION="$(awk -F= '/^mod_version=/{print $2}' gradle.properties | tr -d '[:space:]')"
SOURCE_JAR="build/devlibs/chatdelaymod-${VERSION}-dev.jar"
OUT_DIR="build/download"
OUT_JAR="${OUT_DIR}/chatdelaymod-${VERSION}.jar"
[[ -f "$SOURCE_JAR" ]] || { echo "Missing $SOURCE_JAR. Run './gradlew clean build' first." >&2; exit 1; }
mkdir -p "$OUT_DIR"
cp "$SOURCE_JAR" "$OUT_JAR"
echo "Exported: $OUT_JAR"
sha256sum "$OUT_JAR"
