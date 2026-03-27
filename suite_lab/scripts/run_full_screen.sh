#!/usr/bin/env bash
# run_full_screen.sh — Complete candidate screening in a single non-interactive run.
# This script runs master vs sf18 from every candidate exit EPD position.
# Output goes to suite_lab/results/{white,black}_screen/
# Usage: nohup bash suite_lab/scripts/run_full_screen.sh &

set -euo pipefail

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
FASTCHESS="${REPO}/tools/fastchess/fastchess"
MASTER="${REPO}/bin/stockfish-master"
SF18="${REPO}/bin/stockfish-sf18"
LAB="${REPO}/suite_lab"
RESULTS="${LAB}/results"
ROUNDS=20
TC="1+0.1"
PROGRESS="${RESULTS}/screening_progress.txt"

mkdir -p "${RESULTS}/white_screen" "${RESULTS}/black_screen"

echo "=== Full Candidate Screening ===" | tee "$PROGRESS"
echo "Started: $(date -u)" | tee -a "$PROGRESS"
echo "Rounds per position: ${ROUNDS}, TC: ${TC}" | tee -a "$PROGRESS"
echo "" | tee -a "$PROGRESS"

# ── White candidates ──
echo "--- WHITE CANDIDATES ---" | tee -a "$PROGRESS"
for epd_file in "${LAB}"/white_candidates/W*/exit.epd; do
    cand_id="$(basename "$(dirname "$epd_file")")"
    out_pgn="${RESULTS}/white_screen/${cand_id}.pgn"
    out_log="${RESULTS}/white_screen/${cand_id}.log"

    # Skip if already has sufficient results
    if [[ -f "$out_log" ]] && grep -q "^Games: " "$out_log" 2>/dev/null; then
        existing=$(grep -c "Finished game" "$out_log" 2>/dev/null || echo 0)
        if [[ "$existing" -ge 30 ]]; then
            echo "  SKIP ${cand_id}: already has ${existing} games" | tee -a "$PROGRESS"
            continue
        fi
    fi

    echo -n "  ${cand_id}: " | tee -a "$PROGRESS"

    "${FASTCHESS}" \
        -engine cmd="${MASTER}" name=master \
        -engine cmd="${SF18}" name=sf18 \
        -openings file="${epd_file}" format=epd \
        -each tc="${TC}" proto=uci \
        -rounds "${ROUNDS}" -repeat -concurrency 1 -recover \
        -pgnout file="${out_pgn}" \
        > "${out_log}" 2>&1 || true

    # Extract result
    result=$(grep "Games:" "${out_log}" | tail -1)
    echo "${result}" | tee -a "$PROGRESS"
done

echo "" | tee -a "$PROGRESS"

# ── Black candidates ──
echo "--- BLACK CANDIDATES ---" | tee -a "$PROGRESS"
for epd_file in "${LAB}"/black_candidates/B*/exit.epd; do
    cand_id="$(basename "$(dirname "$epd_file")")"
    out_pgn="${RESULTS}/black_screen/${cand_id}.pgn"
    out_log="${RESULTS}/black_screen/${cand_id}.log"

    # Skip if already has sufficient results
    if [[ -f "$out_log" ]] && grep -q "^Games: " "$out_log" 2>/dev/null; then
        existing=$(grep -c "Finished game" "$out_log" 2>/dev/null || echo 0)
        if [[ "$existing" -ge 30 ]]; then
            echo "  SKIP ${cand_id}: already has ${existing} games" | tee -a "$PROGRESS"
            continue
        fi
    fi

    echo -n "  ${cand_id}: " | tee -a "$PROGRESS"

    "${FASTCHESS}" \
        -engine cmd="${SF18}" name=sf18 \
        -engine cmd="${MASTER}" name=master \
        -openings file="${epd_file}" format=epd \
        -each tc="${TC}" proto=uci \
        -rounds "${ROUNDS}" -repeat -concurrency 1 -recover \
        -pgnout file="${out_pgn}" \
        > "${out_log}" 2>&1 || true

    result=$(grep "Games:" "${out_log}" | tail -1)
    echo "${result}" | tee -a "$PROGRESS"
done

echo "" | tee -a "$PROGRESS"
echo "=== DONE: $(date -u) ===" | tee -a "$PROGRESS"
echo "Run: .venv/bin/python suite_lab/scripts/summarize_candidate_screen.py" | tee -a "$PROGRESS"
