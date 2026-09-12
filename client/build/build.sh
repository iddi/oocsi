#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Compiling OOCSI client for JDK 17 compatibility..."
rm -rf "$CLIENT_DIR/bin"
mkdir -p "$CLIENT_DIR/bin" "$CLIENT_DIR/dist"

SOURCES=$(find "$CLIENT_DIR/src" -name "*.java")

javac --release 17 -d "$CLIENT_DIR/bin" $SOURCES

MANIFEST_FILE="$SCRIPT_DIR/MANIFEST.MF"
cat << 'MANIFEST_EOF' > "$MANIFEST_FILE"
Manifest-Version: 1.0
Class-Path: .
Rsrc-Class-Path: ./

MANIFEST_EOF

jar --create \
    --file "$CLIENT_DIR/dist/oocsi-client.jar" \
    --manifest "$MANIFEST_FILE" \
    -C "$CLIENT_DIR/bin" nl

rm -f "$MANIFEST_FILE"

echo "Successfully built $CLIENT_DIR/dist/oocsi-client.jar (target JDK 17)"
