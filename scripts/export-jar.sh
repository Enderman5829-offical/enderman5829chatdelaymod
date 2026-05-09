#!/usr/bin/env bash
set -euo pipefail

VERSION="$(awk -F= '/^mod_version=/{print $2}' gradle.properties | tr -d '[:space:]')"
MINECRAFT_VERSION="$(awk -F= '/^minecraft_version=/{print $2}' gradle.properties | tr -d '[:space:]')"
mapfile -t RUNTIME_JARS < <(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-sources.jar" | sort)

if [[ ${#RUNTIME_JARS[@]} -eq 0 ]]; then
  echo "Missing runtime jar in build/libs. Run './gradlew clean build' first." >&2
  exit 1
fi

SOURCE_JAR="${RUNTIME_JARS[0]}"
OUT_DIR="build/download"
OUT_JAR="${OUT_DIR}/chatdelaymod-${VERSION}-mc${MINECRAFT_VERSION}.jar"

mkdir -p "$OUT_DIR"
cp "$SOURCE_JAR" "$OUT_JAR"

echo "Exported: $OUT_JAR"
sha256sum "$OUT_JAR"
