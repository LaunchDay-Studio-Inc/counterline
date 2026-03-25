#!/usr/bin/env bash
set -euo pipefail

echo "[counterline] date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo "[counterline] uname:"
uname -a
echo "[counterline] cpu:"
(lscpu || sysctl -a | grep machdep.cpu) 2>/dev/null || true
echo "[counterline] memory:"
(free -h || vm_stat) 2>/dev/null || true
echo "[counterline] disk:"
df -h .
echo "[counterline] python:"
python3 --version
echo "[counterline] git:"
git rev-parse --short HEAD

