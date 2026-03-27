#!/usr/bin/env bash
# Wait for screening to complete, then run analysis.
PROGRESS="/workspaces/counterline/suite_lab/results/screening_progress.txt"
READY="/workspaces/counterline/suite_lab/results/screening_done.flag"

while true; do
    if grep -q "=== DONE:" "$PROGRESS" 2>/dev/null; then
        echo "Screening complete at $(date -u)"
        touch "$READY"
        cd /workspaces/counterline
        source .venv/bin/activate
        python suite_lab/scripts/analyze_side_specific.py > suite_lab/results/final_analysis.txt 2>&1
        echo "Analysis saved to suite_lab/results/final_analysis.txt"
        break
    fi
    sleep 60
done
