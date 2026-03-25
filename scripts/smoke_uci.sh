#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENGINE="${ENGINE:-$ROOT_DIR/.venv/bin/counterline-uci}"
CONFIG="${CONFIG:-$ROOT_DIR/configs/engines.yml}"

[[ -x "$ENGINE" ]] || { echo "missing wrapper executable: $ENGINE" >&2; exit 1; }

output="$(
  printf 'uci\nisready\nposition startpos moves d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5 c1g5 c7c6 e2e3 f8e7 f1d3 b8d7 d1c2 e8g8 g1e2 f8e8 e1g1 d7f8\ngo movetime 1\nquit\n' \
  | "$ENGINE" --config "$CONFIG"
)"

grep -q '^uciok$' <<<"$output"
grep -q '^readyok$' <<<"$output"
grep -q '^bestmove ' <<<"$output"

echo "$output"

