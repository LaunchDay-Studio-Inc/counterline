#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools/fastchess"
FASTCHESS_BIN="$TOOLS_DIR/fastchess"
FASTCHESS_VERSION="${FASTCHESS_VERSION:-v0.8.1}"
FASTCHESS_URL="${FASTCHESS_URL:-https://github.com/Disservin/fastchess/releases/download/${FASTCHESS_VERSION}/fastchess-linux-x86_64}"

mkdir -p "$TOOLS_DIR"

if [[ -x "$FASTCHESS_BIN" ]]; then
  echo "[counterline] fastchess already present: $FASTCHESS_BIN"
  exit 0
fi

echo "[counterline] downloading fastchess from $FASTCHESS_URL"
curl -fsSL "$FASTCHESS_URL" -o "$FASTCHESS_BIN"
chmod +x "$FASTCHESS_BIN"
echo "[counterline] installed fastchess to $FASTCHESS_BIN"

