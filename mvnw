#!/bin/sh
# Apache Maven Wrapper startup script

set -e

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPS="$WRAPPER_DIR/maven-wrapper.properties"

# Read wrapperUrl from properties
getProp() {
  grep "^$1=" "$WRAPPER_PROPS" | cut -d'=' -f2-
}

if [ ! -f "$WRAPPER_JAR" ]; then
  mkdir -p "$WRAPPER_DIR"
  WRAPPER_URL=$(getProp wrapperUrl)
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "Error: Neither curl nor wget is available to download Maven Wrapper." >&2
    exit 1
  fi
fi

JAVA_EXEC="java"

# Pass through Maven Wrapper main
exec "$JAVA_EXEC" \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -cp "$WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
