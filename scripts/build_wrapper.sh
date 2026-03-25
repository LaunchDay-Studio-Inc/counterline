#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -d .venv ]]; then
  python3 -m venv .venv
fi

. .venv/bin/activate
python -m pip install --upgrade pip >/dev/null || true
if ! python -m pip install -e ".[dev]" >/dev/null; then
  echo "[counterline] dependency install failed; falling back to local editable install without deps" >&2
  if ! python -m pip install --no-build-isolation --no-deps -e . >/dev/null; then
    echo "[counterline] pip editable install failed; using setup.py develop" >&2
    python setup.py develop >/dev/null
  fi
fi

echo "[counterline] wrapper installed in .venv"
