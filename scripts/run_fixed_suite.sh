#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FASTCHESS_BIN="${FASTCHESS_PATH:-$ROOT_DIR/tools/fastchess/fastchess}"
WRAPPER_BIN="${WRAPPER_BIN:-$ROOT_DIR/.venv/bin/counterline-uci}"
WRAPPER_CONFIG="${WRAPPER_CONFIG:-$ROOT_DIR/configs/engines.yml}"
OPPONENT_CMD="${OPPONENT_CMD:-$ROOT_DIR/bin/stockfish-master}"
SUITE_FILE="${SUITE_FILE:-$ROOT_DIR/opening_suites/combined/suite_fixed.epd}"
RESULT_PGN="${RESULT_PGN:-$ROOT_DIR/results/fixed-suite.pgn}"
TC="${TC:-10+0.1}"
GAMES="${GAMES:-20}"
THREADS="${THREADS:-1}"
HASH="${HASH:-64}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --games)
      GAMES="$2"
      shift 2
      ;;
    *)
      echo "unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

[[ -x "$FASTCHESS_BIN" ]] || { echo "missing fastchess: $FASTCHESS_BIN" >&2; exit 1; }
[[ -x "$ROOT_DIR/.venv/bin/counterline-uci" ]] || { echo "build wrapper first" >&2; exit 1; }
[[ -x "$OPPONENT_CMD" ]] || { echo "missing opponent engine: $OPPONENT_CMD" >&2; exit 1; }

mkdir -p "$ROOT_DIR/results"
python3 "$ROOT_DIR/opening_suites/generate_suite.py"

"$FASTCHESS_BIN" \
  -engine cmd="$WRAPPER_BIN" name=CounterLine arg=--config arg="$WRAPPER_CONFIG" \
  -engine cmd="$OPPONENT_CMD" name=StockfishMaster \
  -each tc="$TC" proto=uci option.Threads="$THREADS" option.Hash="$HASH" \
  -games "$GAMES" \
  -rounds 1 \
  -openings file="$SUITE_FILE" format=epd order=sequential \
  -pgnout "$RESULT_PGN"

echo "[counterline] wrote $RESULT_PGN"
