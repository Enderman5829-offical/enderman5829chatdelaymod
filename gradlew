#!/bin/sh
set -eu
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
