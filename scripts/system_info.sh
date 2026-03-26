#!/usr/bin/env bash
set -euo pipefail

echo "[counterline] date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo "[counterline] uname:"
uname -a
echo "[counterline] nproc: $(nproc 2>/dev/null || echo unknown)"
echo "[counterline] cpu:"
(lscpu || sysctl -a | grep machdep.cpu) 2>/dev/null || true
echo "[counterline] memory:"
(free -h || vm_stat) 2>/dev/null || true
echo "[counterline] disk:"
df -h .
echo "[counterline] python:"
python3 --version 2>/dev/null || echo "not found"
echo "[counterline] gcc:"
gcc --version 2>&1 | head -1 || echo "not found"
echo "[counterline] clang:"
clang --version 2>&1 | head -1 || echo "not found"
echo "[counterline] make:"
make --version 2>&1 | head -1 || echo "not found"
echo "[counterline] cmake:"
cmake --version 2>&1 | head -1 || echo "not found"
echo "[counterline] pandoc:"
pandoc --version 2>&1 | head -1 || echo "not found"
echo "[counterline] tectonic:"
tectonic --version 2>&1 | head -1 || echo "not found"
echo "[counterline] quarto:"
quarto --version 2>&1 | head -1 || echo "not found"
echo "[counterline] sqlite3:"
sqlite3 --version 2>&1 | head -1 || echo "not found"
echo "[counterline] git:"
git rev-parse --short HEAD 2>/dev/null || echo "not a git repo"

