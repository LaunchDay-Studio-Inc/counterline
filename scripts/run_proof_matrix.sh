#!/usr/bin/env bash
# Proof Matrix: 9 matches to validate the integrated CounterLine engine
# Usage: bash scripts/run_proof_matrix.sh [--rounds N] [--tc TC]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FASTCHESS="$ROOT_DIR/tools/fastchess/fastchess"
ROUNDS="${ROUNDS:-20}"
TC="${TC:-1+0.1}"
THREADS=1
HASH=64
RESULTS="$ROOT_DIR/results/proof_matrix"
TIMESTAMP="$(date -u +%Y%m%d_%H%M%S)"

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --rounds) ROUNDS="$2"; shift 2 ;;
    --tc) TC="$2"; shift 2 ;;
    *) echo "unknown: $1" >&2; exit 2 ;;
  esac
done

# Engine paths
MASTER="$ROOT_DIR/bin/stockfish-master"
SF18="$ROOT_DIR/bin/stockfish-sf18"
NULL_WRAPPER="$ROOT_DIR/bin/null-wrapper"
CL_COMBINED="$ROOT_DIR/bin/counterline-combined"

# Suite paths
WHITE_SUITE="$ROOT_DIR/opening_suites/final/white/exit.epd"
BLACK_SUITE="$ROOT_DIR/opening_suites/final/black/exit.epd"
COMBINED_SUITE="$ROOT_DIR/opening_suites/final/combined/combined.epd"

# Verify binaries
for bin in "$FASTCHESS" "$MASTER" "$SF18" "$NULL_WRAPPER" "$CL_COMBINED"; do
  [[ -x "$bin" ]] || { echo "missing: $bin" >&2; exit 1; }
done
for suite in "$WHITE_SUITE" "$BLACK_SUITE" "$COMBINED_SUITE"; do
  [[ -f "$suite" ]] || { echo "missing: $suite" >&2; exit 1; }
done

echo "=========================================="
echo "  CounterLine Proof Matrix"
echo "  TC=$TC  Rounds=$ROUNDS  Threads=$THREADS  Hash=$HASH"
echo "  Timestamp: $TIMESTAMP"
echo "=========================================="

run_match() {
  local engine1_cmd="$1"
  local engine1_name="$2"
  local engine2_cmd="$3"
  local engine2_name="$4"
  local suite_file="$5"
  local match_id="$6"
  local match_dir="$RESULTS/${match_id}"

  mkdir -p "$match_dir"
  local pgn_file="$match_dir/games.pgn"
  local log_file="$match_dir/console.log"

  echo ""
  echo "--- Match $match_id: $engine1_name vs $engine2_name ---"
  echo "    Suite: $suite_file"
  echo "    Output: $match_dir"

  "$FASTCHESS" \
    -engine cmd="$engine1_cmd" name="$engine1_name" \
    -engine cmd="$engine2_cmd" name="$engine2_name" \
    -each tc="$TC" proto=uci option.Threads="$THREADS" option.Hash="$HASH" \
    -rounds "$ROUNDS" \
    -games 2 \
    -repeat \
    -openings file="$suite_file" format=epd order=sequential \
    -pgnout file="$pgn_file" \
    -concurrency 1 \
    2>&1 | tee "$log_file"

  # Parse PGN and generate summary
  if [[ -f "$pgn_file" ]]; then
    python3 - "$pgn_file" "$match_dir" "$engine1_name" "$engine2_name" "$TC" "$ROUNDS" "$match_id" "$suite_file" <<'PYEOF'
import json, sys
from collections import Counter
from pathlib import Path
import chess.pgn

pgn_path = Path(sys.argv[1])
match_dir = Path(sys.argv[2])
e1_name = sys.argv[3]
e2_name = sys.argv[4]
tc = sys.argv[5]
rounds = int(sys.argv[6])
match_id = sys.argv[7]
suite_file = sys.argv[8]

counts = Counter()
e1_as_white_w = e1_as_white_d = e1_as_white_l = 0
e1_as_black_w = e1_as_black_d = e1_as_black_l = 0

with pgn_path.open() as f:
    while True:
        g = chess.pgn.read_game(f)
        if g is None:
            break
        result = g.headers.get("Result", "*")
        counts[result] += 1
        white_name = g.headers.get("White", "")
        if e1_name in white_name:
            if result == "1-0": e1_as_white_w += 1
            elif result == "0-1": e1_as_white_l += 1
            else: e1_as_white_d += 1
        else:
            if result == "1-0": e1_as_black_l += 1
            elif result == "0-1": e1_as_black_w += 1
            else: e1_as_black_d += 1

total = sum(counts.values())
w = counts.get("1-0", 0)
b = counts.get("0-1", 0)
d = counts.get("1/2-1/2", 0)
u = counts.get("*", 0)

e1_score = (e1_as_white_w + e1_as_black_w) + 0.5 * (e1_as_white_d + e1_as_black_d)
e2_score = total - e1_score
e1_wins = e1_as_white_w + e1_as_black_w
e1_losses = e1_as_white_l + e1_as_black_l
e1_draws = e1_as_white_d + e1_as_black_d
e1_pct = (e1_score / total * 100) if total > 0 else 0

data = {
    "match_id": match_id,
    "engine1": e1_name,
    "engine2": e2_name,
    "tc": tc,
    "games": total,
    "rounds": rounds,
    "e1_wins": e1_wins,
    "e1_losses": e1_losses,
    "e1_draws": e1_draws,
    "e1_score": e1_score,
    "e1_pct": round(e1_pct, 2),
    "e1_as_white": f"{e1_as_white_w}W-{e1_as_white_l}L-{e1_as_white_d}D",
    "e1_as_black": f"{e1_as_black_w}W-{e1_as_black_l}L-{e1_as_black_d}D",
    "suite": suite_file,
}
(match_dir / "results.json").write_text(json.dumps(data, indent=2))

md = f"""# {match_id}

| Field | Value |
|---|---|
| Engine 1 | {e1_name} |
| Engine 2 | {e2_name} |
| TC | {tc} |
| Games | {total} |
| {e1_name} W-L-D | {e1_wins}-{e1_losses}-{e1_draws} |
| {e1_name} Score | {e1_score}/{total} ({e1_pct:.1f}%) |
| As White | {e1_as_white_w}W-{e1_as_white_l}L-{e1_as_white_d}D |
| As Black | {e1_as_black_w}W-{e1_as_black_l}L-{e1_as_black_d}D |
| Suite | {suite_file} |
"""
(match_dir / "summary.md").write_text(md)
print(md)
PYEOF
  fi
}

# === WHITE LINE MATCHES ===
echo ""
echo "========== WHITE LINE (Vienna Gambit) =========="

run_match "$MASTER" "StockfishMaster" "$SF18" "StockfishSF18" \
  "$WHITE_SUITE" "M1_white_master_vs_sf18"

run_match "$NULL_WRAPPER" "NullWrapper" "$SF18" "StockfishSF18" \
  "$WHITE_SUITE" "M2_white_null_vs_sf18"

run_match "$CL_COMBINED" "CounterLineCombined" "$SF18" "StockfishSF18" \
  "$WHITE_SUITE" "M3_white_cl_vs_sf18"

# === BLACK LINE MATCHES ===
echo ""
echo "========== BLACK LINE (Caro-Kann Classical) =========="

run_match "$MASTER" "StockfishMaster" "$SF18" "StockfishSF18" \
  "$BLACK_SUITE" "M4_black_master_vs_sf18"

run_match "$NULL_WRAPPER" "NullWrapper" "$SF18" "StockfishSF18" \
  "$BLACK_SUITE" "M5_black_null_vs_sf18"

run_match "$CL_COMBINED" "CounterLineCombined" "$SF18" "StockfishSF18" \
  "$BLACK_SUITE" "M6_black_cl_vs_sf18"

# === COMBINED SUITE MATCHES ===
echo ""
echo "========== COMBINED SUITE =========="

run_match "$MASTER" "StockfishMaster" "$SF18" "StockfishSF18" \
  "$COMBINED_SUITE" "M7_combined_master_vs_sf18"

run_match "$NULL_WRAPPER" "NullWrapper" "$SF18" "StockfishSF18" \
  "$COMBINED_SUITE" "M8_combined_null_vs_sf18"

run_match "$CL_COMBINED" "CounterLineCombined" "$SF18" "StockfishSF18" \
  "$COMBINED_SUITE" "M9_combined_cl_vs_sf18"

# === GENERATE OVERALL SUMMARY ===
echo ""
echo "========== GENERATING OVERALL SUMMARY =========="

python3 - "$RESULTS" <<'PYEOF'
import json
from pathlib import Path

results_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("results/proof_matrix")
import sys
results_dir = Path(sys.argv[1])

rows = []
for d in sorted(results_dir.iterdir()):
    rj = d / "results.json"
    if rj.exists():
        rows.append(json.loads(rj.read_text()))

md = "# Proof Matrix Summary\n\n"
md += "| # | Match | E1 | E2 | Games | W-L-D | Score | As White | As Black |\n"
md += "|---|-------|----|----|-------|-------|-------|----------|----------|\n"
for i, r in enumerate(rows, 1):
    md += (
        f"| {i} | {r['match_id']} | {r['engine1']} | {r['engine2']} | "
        f"{r['games']} | {r['e1_wins']}-{r['e1_losses']}-{r['e1_draws']} | "
        f"{r['e1_pct']}% | {r['e1_as_white']} | {r['e1_as_black']} |\n"
    )
md += "\n"

(results_dir / "PROOF_MATRIX.md").write_text(md)
(results_dir / "proof_matrix.json").write_text(json.dumps(rows, indent=2))
print(md)
PYEOF

echo ""
echo "=========================================="
echo "  Proof matrix complete!"
echo "  Results: $RESULTS"
echo "=========================================="
