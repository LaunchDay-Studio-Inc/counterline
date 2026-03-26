#!/usr/bin/env bash
# check_toolchain.sh — verify every tool the project needs is available.
# Exits non-zero and prints diagnostics if anything critical is missing.
set -euo pipefail

fail=0

check() {
  local name="$1" cmd="$2"
  if command -v "$cmd" >/dev/null 2>&1; then
    printf "  ✓  %-16s %s\n" "$name" "$("$cmd" --version 2>&1 | head -1)"
  else
    printf "  ✗  %-16s MISSING\n" "$name"
    fail=1
  fi
}

echo "CounterLine toolchain check"
echo "──────────────────────────────────────────────"

echo ""
echo "Build tools:"
check "gcc"       gcc
check "g++"       g++
check "clang"     clang
check "make"      make
check "cmake"     cmake
check "ninja"     ninja

echo ""
echo "System utilities:"
check "git"       git
check "curl"      curl
check "wget"      wget
check "jq"        jq
check "rsync"     rsync
check "sqlite3"   sqlite3
check "shellcheck" shellcheck
check "dot"       dot          # graphviz

echo ""
echo "Python:"
check "python3"   python3
check "pip"       pip3

echo ""
echo "Document toolchain:"
check "pandoc"    pandoc
check "tectonic"  tectonic
check "quarto"    quarto

echo ""
echo "Optional (fetched by scripts):"
if [[ -x tools/fastchess/fastchess ]]; then
  printf "  ✓  %-16s %s\n" "fastchess" "$(tools/fastchess/fastchess --version 2>&1 | head -1 || echo 'present')"
else
  printf "  ○  %-16s not yet fetched (run: make fetch-fastchess)\n" "fastchess"
fi

echo ""
if [[ "$fail" -ne 0 ]]; then
  echo "FAIL: one or more critical tools are missing."
  exit 1
else
  echo "All critical tools present."
fi
