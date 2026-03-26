#!/usr/bin/env bash
# bootstrap_codespace.sh — one-shot setup run by devcontainer postCreateCommand.
# Creates .venv, installs Python deps, verifies toolchain, and does a quick
# Stockfish compile test.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "──────────────────────────────────────────────"
echo " CounterLine — bootstrap"
echo "──────────────────────────────────────────────"

# ── Python venv + deps ────────────────────────────────────────────────────
if [[ ! -d .venv ]]; then
  python3 -m venv .venv
fi
# shellcheck disable=SC1091
. .venv/bin/activate
python -m pip install --upgrade pip -q
python -m pip install -e ".[dev]" -q
echo "[bootstrap] Python venv ready"

# ── Verify key tools ─────────────────────────────────────────────────────
echo ""
echo "[bootstrap] tool versions:"
printf "  %-14s %s\n" "python"   "$(python3 --version 2>&1)"
printf "  %-14s %s\n" "pip"      "$(python -m pip --version 2>&1 | head -1)"
printf "  %-14s %s\n" "gcc"      "$(gcc --version 2>&1 | head -1)"
printf "  %-14s %s\n" "clang"    "$(clang --version 2>&1 | head -1)"
printf "  %-14s %s\n" "make"     "$(make --version 2>&1 | head -1)"
printf "  %-14s %s\n" "cmake"    "$(cmake --version 2>&1 | head -1)"
printf "  %-14s %s\n" "pandoc"   "$(pandoc --version 2>&1 | head -1)"
printf "  %-14s %s\n" "tectonic" "$(tectonic --version 2>&1 | head -1)"
printf "  %-14s %s\n" "quarto"   "$(quarto --version 2>&1 | head -1)"
printf "  %-14s %s\n" "sqlite3"  "$(sqlite3 --version 2>&1 | head -1)"
printf "  %-14s %s\n" "jq"       "$(jq --version 2>&1 | head -1)"

# ── Verify Stockfish can build (tiny incremental build test) ─────────────
echo ""
echo "[bootstrap] verifying Stockfish build system..."
if make -C src -j"$(nproc)" build ARCH=x86-64 >/dev/null 2>&1; then
  echo "[bootstrap] Stockfish compiles OK"
  rm -f src/stockfish
else
  echo "[bootstrap] WARNING: Stockfish build failed — check compiler / src/"
fi

# ── Fetch fastchess ──────────────────────────────────────────────────────
echo ""
"$ROOT_DIR/scripts/fetch_fastchess.sh" || echo "[bootstrap] WARNING: fastchess fetch failed (non-fatal)"

echo ""
echo "──────────────────────────────────────────────"
echo " Bootstrap complete.  Run 'make build-all' to"
echo " build Stockfish binaries."
echo "──────────────────────────────────────────────"
