#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FASTCHESS_BIN="${FASTCHESS_PATH:-$ROOT_DIR/tools/fastchess/fastchess}"
SUITE_FILE="${SUITE_FILE:-$ROOT_DIR/opening_suites/combined/suite_fixed.epd}"
TC="${TC:-10+0.1}"
ROUNDS="${ROUNDS:-20}"
THREADS="${THREADS:-1}"
HASH="${HASH:-64}"
RESULTS_DIR="${RESULTS_DIR:-$ROOT_DIR/results/matches}"
TIMESTAMP="$(date -u +%Y%m%d_%H%M%S)"

# Engine 1 and 2 — configurable
ENGINE1_CMD="${ENGINE1_CMD:-$ROOT_DIR/.venv/bin/counterline-uci}"
ENGINE1_NAME="${ENGINE1_NAME:-CounterLine}"
ENGINE1_ARGS="${ENGINE1_ARGS:-}"
ENGINE2_CMD="${ENGINE2_CMD:-$ROOT_DIR/bin/stockfish-master}"
ENGINE2_NAME="${ENGINE2_NAME:-StockfishMaster}"
ENGINE2_ARGS="${ENGINE2_ARGS:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --rounds) ROUNDS="$2"; shift 2 ;;
    --tc) TC="$2"; shift 2 ;;
    --threads) THREADS="$2"; shift 2 ;;
    --hash) HASH="$2"; shift 2 ;;
    --engine1-cmd) ENGINE1_CMD="$2"; shift 2 ;;
    --engine1-name) ENGINE1_NAME="$2"; shift 2 ;;
    --engine2-cmd) ENGINE2_CMD="$2"; shift 2 ;;
    --engine2-name) ENGINE2_NAME="$2"; shift 2 ;;
    --suite) SUITE_FILE="$2"; shift 2 ;;
    --results-dir) RESULTS_DIR="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

# Auto-fetch fastchess if missing
if [[ ! -x "$FASTCHESS_BIN" ]]; then
  echo "[counterline] fastchess not found, fetching..."
  bash "$ROOT_DIR/scripts/fetch_fastchess.sh"
fi

[[ -x "$FASTCHESS_BIN" ]] || { echo "missing fastchess: $FASTCHESS_BIN" >&2; exit 1; }
[[ -f "$SUITE_FILE" ]] || { echo "missing suite file: $SUITE_FILE" >&2; exit 1; }

MATCH_NAME="${ENGINE1_NAME}_vs_${ENGINE2_NAME}_${TIMESTAMP}"
MATCH_DIR="$RESULTS_DIR/$MATCH_NAME"
mkdir -p "$MATCH_DIR"

PGN_FILE="$MATCH_DIR/games.pgn"
LOG_FILE="$MATCH_DIR/console.log"
SUMMARY_FILE="$MATCH_DIR/summary.md"
JSON_FILE="$MATCH_DIR/results.json"

echo "[counterline] match: $ENGINE1_NAME vs $ENGINE2_NAME"
echo "[counterline] suite: $SUITE_FILE"
echo "[counterline] tc=$TC rounds=$ROUNDS threads=$THREADS hash=$HASH"
echo "[counterline] output: $MATCH_DIR"

"$FASTCHESS_BIN" \
  -engine cmd="$ENGINE1_CMD" name="$ENGINE1_NAME" \
  -engine cmd="$ENGINE2_CMD" name="$ENGINE2_NAME" \
  -each tc="$TC" proto=uci option.Threads="$THREADS" option.Hash="$HASH" \
  -rounds "$ROUNDS" \
  -games 2 \
  -repeat \
  -openings file="$SUITE_FILE" format=epd order=sequential \
  -pgnout file="$PGN_FILE" \
  2>&1 | tee "$LOG_FILE"

# Generate summary
if [[ -f "$PGN_FILE" ]]; then
  source "$ROOT_DIR/.venv/bin/activate" 2>/dev/null || true

  python3 -c "
import json
from collections import Counter
from pathlib import Path
import chess.pgn

pgn_path = Path('$PGN_FILE')
counts = Counter()
with pgn_path.open() as f:
    while True:
        g = chess.pgn.read_game(f)
        if g is None:
            break
        counts[g.headers.get('Result', '*')] += 1
total = sum(counts.values())
w = counts.get('1-0', 0)
b = counts.get('0-1', 0)
d = counts.get('1/2-1/2', 0)
u = counts.get('*', 0)

# Write JSON
data = {
    'engine1': '$ENGINE1_NAME',
    'engine2': '$ENGINE2_NAME',
    'tc': '$TC',
    'games': total,
    'rounds': $ROUNDS,
    'threads': $THREADS,
    'hash': $HASH,
    'suite': '$SUITE_FILE',
    'white_wins': w,
    'black_wins': b,
    'draws': d,
    'unfinished': u,
}
Path('$JSON_FILE').write_text(json.dumps(data, indent=2))

# Write markdown summary
md = f'''# Match Summary

| Field | Value |
| --- | --- |
| Engine 1 (White first) | $ENGINE1_NAME |
| Engine 2 (Black first) | $ENGINE2_NAME |
| TC | $TC |
| Games | {total} |
| Rounds | $ROUNDS |
| Thread | $THREADS |
| Hash | $HASH MB |
| Suite | $SUITE_FILE |
| White wins (1-0) | {w} |
| Black wins (0-1) | {b} |
| Draws | {d} |
| Unfinished | {u} |
| E1 Score | {w + d * 0.5:.1f}/{total} |
| E2 Score | {b + d * 0.5:.1f}/{total} |
'''
Path('$SUMMARY_FILE').write_text(md)
print(md)
"
fi

echo "[counterline] match complete: $MATCH_DIR"
