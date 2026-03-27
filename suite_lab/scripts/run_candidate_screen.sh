#!/usr/bin/env bash
# run_candidate_screen.sh — Run short matches for every candidate exit position.
#
# Usage: bash suite_lab/scripts/run_candidate_screen.sh [rounds_per_pos] [tc]
#
# Defaults: 20 rounds per position, tc=1+0.1
#
# Runs stockfish-master vs stockfish-sf18 from every candidate exit EPD,
# saving PGN + summary for each.

set -euo pipefail

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
FASTCHESS="${REPO}/tools/fastchess/fastchess"
MASTER="${REPO}/bin/stockfish-master"
SF18="${REPO}/bin/stockfish-sf18"
LAB="${REPO}/suite_lab"
RESULTS="${LAB}/results"

ROUNDS="${1:-20}"
TC="${2:-1+0.1}"

mkdir -p "${RESULTS}/white_screen" "${RESULTS}/black_screen"

echo "=== Candidate Screening ==="
echo "Rounds per position: ${ROUNDS}"
echo "Time control: ${TC}"
echo ""

# ── White candidates ──
# For White candidates: master plays White (engine1), sf18 plays Black (engine2)
echo "--- WHITE CANDIDATES (master=White vs sf18=Black) ---"
for epd_file in "${LAB}"/white_candidates/W*/exit.epd; do
    cand_id="$(basename "$(dirname "$epd_file")")"
    out_pgn="${RESULTS}/white_screen/${cand_id}.pgn"
    out_log="${RESULTS}/white_screen/${cand_id}.log"

    echo -n "  ${cand_id}: "

    "${FASTCHESS}" \
        -engine cmd="${MASTER}" name=master \
        -engine cmd="${SF18}" name=sf18 \
        -openings file="${epd_file}" format=epd \
        -each tc="${TC}" proto=uci \
        -rounds "${ROUNDS}" -repeat -concurrency 1 -recover \
        -pgnout file="${out_pgn}" \
        > "${out_log}" 2>&1 || true

    # Extract final score line
    tail -3 "${out_log}" | head -3
done

echo ""

# ── Black candidates ──
# For Black candidates: sf18 plays White (engine1), master plays Black (engine2)
# We swap engine order so master plays Black
echo "--- BLACK CANDIDATES (sf18=White vs master=Black) ---"
for epd_file in "${LAB}"/black_candidates/B*/exit.epd; do
    cand_id="$(basename "$(dirname "$epd_file")")"
    out_pgn="${RESULTS}/black_screen/${cand_id}.pgn"
    out_log="${RESULTS}/black_screen/${cand_id}.log"

    echo -n "  ${cand_id}: "

    "${FASTCHESS}" \
        -engine cmd="${SF18}" name=sf18 \
        -engine cmd="${MASTER}" name=master \
        -openings file="${epd_file}" format=epd \
        -each tc="${TC}" proto=uci \
        -rounds "${ROUNDS}" -repeat -concurrency 1 -recover \
        -pgnout file="${out_pgn}" \
        > "${out_log}" 2>&1 || true

    tail -3 "${out_log}" | head -3
done

echo ""
echo "=== Screening complete. Results in ${RESULTS}/ ==="
echo "Run: .venv/bin/python suite_lab/scripts/summarize_candidate_screen.py"
