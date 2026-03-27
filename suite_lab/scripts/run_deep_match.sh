#!/usr/bin/env bash
# run_deep_match.sh — Run a high-confidence match for a single candidate.
# Usage: bash suite_lab/scripts/run_deep_match.sh <candidate_dir> <color> [rounds] [tc]
#   e.g.: bash suite_lab/scripts/run_deep_match.sh suite_lab/white_candidates/W06_vienna_gambit white 100 1+0.1

set -euo pipefail

CAND_DIR="${1:?Usage: $0 <candidate_dir> <color> [rounds] [tc]}"
COLOR="${2:?Specify color: white or black}"
ROUNDS="${3:-100}"
TC="${4:-1+0.1}"

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
FASTCHESS="${REPO}/tools/fastchess/fastchess"
MASTER="${REPO}/bin/stockfish-master"
SF18="${REPO}/bin/stockfish-sf18"

CAND_ID="$(basename "$CAND_DIR")"
EPD_FILE="${CAND_DIR}/exit.epd"
RESULTS_DIR="${REPO}/suite_lab/results/deep_matches"
mkdir -p "$RESULTS_DIR"

OUT_PGN="${RESULTS_DIR}/${CAND_ID}_deep.pgn"
OUT_LOG="${RESULTS_DIR}/${CAND_ID}_deep.log"

# Remove stale output
rm -f "$OUT_PGN" "$OUT_LOG"

echo "=== Deep Match: ${CAND_ID} ==="
echo "Color: ${COLOR}, Rounds: ${ROUNDS}, TC: ${TC}"
echo "EPD: $(cat "$EPD_FILE")"
echo ""

if [[ "$COLOR" == "white" ]]; then
    # master as engine1 (White side), sf18 as engine2
    "${FASTCHESS}" \
        -engine cmd="${MASTER}" name=master \
        -engine cmd="${SF18}" name=sf18 \
        -openings file="${EPD_FILE}" format=epd \
        -each tc="${TC}" proto=uci \
        -rounds "${ROUNDS}" -repeat -concurrency 1 -recover \
        -pgnout file="${OUT_PGN}" \
        > "${OUT_LOG}" 2>&1 || true
else
    # sf18 as engine1 (White side), master as engine2 (Black side)
    "${FASTCHESS}" \
        -engine cmd="${SF18}" name=sf18 \
        -engine cmd="${MASTER}" name=master \
        -openings file="${EPD_FILE}" format=epd \
        -each tc="${TC}" proto=uci \
        -rounds "${ROUNDS}" -repeat -concurrency 1 -recover \
        -pgnout file="${OUT_PGN}" \
        > "${OUT_LOG}" 2>&1 || true
fi

echo ""
echo "--- Results ---"
tail -10 "${OUT_LOG}" | grep -E "^(Results|Games|Elo|Ptnml)" || true
echo ""
echo "Log: ${OUT_LOG}"
echo "PGN: ${OUT_PGN}"
