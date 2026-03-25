#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BIN_DIR="$ROOT_DIR/bin"
MASTER_OUT="$BIN_DIR/stockfish-master"
SF18_OUT="$BIN_DIR/stockfish-sf_18"
JOBS="${JOBS:-$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)}"
ARCH="${ARCH:-native}"

mkdir -p "$BIN_DIR"

build_tree() {
  local tree_root="$1"
  local output="$2"
  make -C "$tree_root/src" -j"$JOBS" profile-build ARCH="$ARCH" >/dev/null
  cp "$tree_root/src/stockfish" "$output"
  chmod +x "$output"
}

echo "[counterline] building current tree -> $MASTER_OUT"
build_tree "$ROOT_DIR" "$MASTER_OUT"

tmpdir="$(mktemp -d "${TMPDIR:-/tmp}/counterline-sf18.XXXXXX")"
cleanup() {
  git -C "$ROOT_DIR" worktree remove --force "$tmpdir" >/dev/null 2>&1 || true
  rm -rf "$tmpdir"
}
trap cleanup EXIT

echo "[counterline] building tag sf_18 -> $SF18_OUT"
git -C "$ROOT_DIR" worktree add --detach "$tmpdir" sf_18 >/dev/null
build_tree "$tmpdir" "$SF18_OUT"

echo "[counterline] built:"
echo "  $MASTER_OUT"
echo "  $SF18_OUT"

