#!/usr/bin/env bash
# fetch_fastchess.sh — download the fastchess binary or build from source.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools/fastchess"
FASTCHESS_BIN="$TOOLS_DIR/fastchess"
FASTCHESS_VERSION="${FASTCHESS_VERSION:-v1.8.0-alpha}"
FASTCHESS_URL="${FASTCHESS_URL:-https://github.com/Disservin/fastchess/releases/download/${FASTCHESS_VERSION}/fastchess-linux-x86-64.tar}"

mkdir -p "$TOOLS_DIR"

if [[ -x "$FASTCHESS_BIN" ]]; then
  echo "[counterline] fastchess already present: $FASTCHESS_BIN"
  "$FASTCHESS_BIN" --version 2>/dev/null || true
  exit 0
fi

# ── Try binary download first ────────────────────────────────────────────
echo "[counterline] downloading fastchess from $FASTCHESS_URL"
tmptar="$(mktemp /tmp/fastchess.XXXXXX.tar)"
if curl -fsSL "$FASTCHESS_URL" -o "$tmptar" 2>/dev/null; then
  tar xf "$tmptar" -C "$TOOLS_DIR"
  rm -f "$tmptar"
  # The tar may extract to a subdirectory or directly
  if [[ ! -x "$FASTCHESS_BIN" ]]; then
    found="$(find "$TOOLS_DIR" -name 'fastchess' -type f | head -1)"
    if [[ -n "$found" && "$found" != "$FASTCHESS_BIN" ]]; then
      mv "$found" "$FASTCHESS_BIN"
    fi
  fi
  chmod +x "$FASTCHESS_BIN"
else
  rm -f "$tmptar"
  echo "[counterline] binary download failed — building from source"

  builddir="$(mktemp -d /tmp/fastchess-build.XXXXXX)"
  trap 'rm -rf "$builddir"' EXIT
  git clone --depth 1 https://github.com/Disservin/fastchess.git "$builddir"
  make -C "$builddir" -j"$(nproc)"
  cp "$builddir/fastchess" "$FASTCHESS_BIN"
  chmod +x "$FASTCHESS_BIN"
fi

# ── Verify execution ────────────────────────────────────────────────────
if "$FASTCHESS_BIN" --help >/dev/null 2>&1; then
  echo "[counterline] installed fastchess to $FASTCHESS_BIN"
else
  echo "[counterline] ERROR: fastchess binary is not executable" >&2
  exit 1
fi

