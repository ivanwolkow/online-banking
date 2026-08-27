#!/bin/sh
# Minimal Maven Wrapper bootstrapper. The wrapper JAR is downloaded once from Maven Central.
set -e
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  mkdir -p "$WRAPPER_DIR"
  WRAPPER_URL=$(sed -n 's/^wrapperUrl=//p' "$WRAPPER_DIR/maven-wrapper.properties")
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "curl or wget is required to bootstrap Maven Wrapper" >&2
    exit 1
  fi
fi
exec java ${JAVA_OPTS:-} -classpath "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$BASE_DIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
