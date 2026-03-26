#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools/fastchess"
FASTCHESS_BIN="$TOOLS_DIR/fastchess"
FASTCHESS_VERSION="${FASTCHESS_VERSION:-v1.8.0-alpha}"
FASTCHESS_URL="${FASTCHESS_URL:-https://github.com/Disservin/fastchess/releases/download/${FASTCHESS_VERSION}/fastchess-linux-x86-64.tar}"

mkdir -p "$TOOLS_DIR"

if [[ -x "$FASTCHESS_BIN" ]]; then
  echo "[counterline] fastchess already present: $FASTCHESS_BIN"
  exit 0
fi

echo "[counterline] downloading fastchess from $FASTCHESS_URL"
tmptar="$(mktemp /tmp/fastchess.XXXXXX.tar)"
curl -fsSL "$FASTCHESS_URL" -o "$tmptar"
tar xf "$tmptar" -C "$TOOLS_DIR"
rm -f "$tmptar"
# The tar may extract to a subdirectory or directly
if [[ ! -x "$FASTCHESS_BIN" ]]; then
  # Try finding it
  found="$(find "$TOOLS_DIR" -name 'fastchess' -type f | head -1)"
  if [[ -n "$found" && "$found" != "$FASTCHESS_BIN" ]]; then
    mv "$found" "$FASTCHESS_BIN"
  fi
fi
chmod +x "$FASTCHESS_BIN"
echo "[counterline] installed fastchess to $FASTCHESS_BIN"

