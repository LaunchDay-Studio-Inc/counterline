#!/usr/bin/env python3
"""Summarize candidate screening results.

Parses fastchess log files to extract W/D/L tallies, then ranks candidates
by score (from the perspective of our side: White for white candidates,
Black for black candidates).

Output: a ranked table plus JSON for downstream use.
"""

import json
import os
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
LAB = REPO / "suite_lab"
RESULTS = LAB / "results"


def parse_fastchess_log(log_path: Path) -> dict | None:
    """Parse a fastchess log to extract results.
    
    Looks for lines like:
    Score of master vs sf18: W-D-L  [pct]  N
    or the results summary table.
    """
    text = log_path.read_text()
    
    # Try to find "Score of ..." line
    # Pattern: Score of NAME1 vs NAME2: 10 - 5 - 5  [0.625] 20
    m = re.search(
        r'Score of (\S+) vs (\S+):\s+(\d+)\s*-\s*(\d+)\s*-\s*(\d+)\s+\[([0-9.]+)\]\s+(\d+)',
        text
    )
    if m:
        engine1 = m.group(1)
        engine2 = m.group(2)
        w = int(m.group(3))
        d = int(m.group(4))
        l = int(m.group(5))
        pct = float(m.group(6))
        n = int(m.group(7))
        return {
            "engine1": engine1,
            "engine2": engine2,
            "wins": w,
            "draws": d,
            "losses": l,
            "score_pct": pct,
            "total": n,
        }
    
    # Fallback: try PGN result counting
    results_1_0 = text.count('[Result "1-0"]')
    results_0_1 = text.count('[Result "0-1"]')
    results_draw = text.count('[Result "1/2-1/2"]')
    total = results_1_0 + results_0_1 + results_draw
    
    if total > 0:
        return {
            "engine1": "engine1",
            "engine2": "engine2",
            "wins": results_1_0,
            "draws": results_draw,
            "losses": results_0_1,
            "score_pct": (results_1_0 + 0.5 * results_draw) / total,
            "total": total,
        }
    
    return None


def summarize_color(color: str, screen_dir: Path, meta_lookup: dict) -> list[dict]:
    """Summarize all candidates of a given color."""
    results = []
    
    if not screen_dir.exists():
        return results
    
    for log_file in sorted(screen_dir.glob("*.log")):
        cand_id = log_file.stem
        parsed = parse_fastchess_log(log_file)
        
        if parsed is None:
            print(f"  WARNING: Could not parse {log_file}")
            continue
        
        # For white candidates: master is engine1 (White), score is from master's perspective
        # For black candidates: sf18 is engine1 (White), master is engine2 (Black)
        #   So we need to flip: our score = losses (sf18 losses = master wins as Black)
        if color == "white":
            our_wins = parsed["wins"]
            our_losses = parsed["losses"]
            our_score = parsed["score_pct"]
        else:
            # Swap perspective: engine1=sf18, engine2=master
            our_wins = parsed["losses"]      # sf18 losses = master/Black wins
            our_losses = parsed["wins"]       # sf18 wins = master/Black losses
            our_score = 1.0 - parsed["score_pct"]
        
        draws = parsed["draws"]
        total = parsed["total"]
        decisive_rate = (our_wins + our_losses) / total if total > 0 else 0
        
        meta = meta_lookup.get(cand_id, {})
        
        entry = {
            "id": cand_id,
            "name": meta.get("name", cand_id),
            "color": color,
            "our_wins": our_wins,
            "draws": draws,
            "our_losses": our_losses,
            "total": total,
            "our_score_pct": round(our_score, 4),
            "decisive_rate": round(decisive_rate, 4),
            "elo_diff": elo_from_pct(our_score) if total >= 4 else None,
        }
        results.append(entry)
    
    # Sort by score (descending)
    results.sort(key=lambda x: x["our_score_pct"], reverse=True)
    return results


def elo_from_pct(pct: float) -> float:
    """Convert win percentage to approximate Elo difference."""
    import math
    if pct <= 0:
        return -999
    if pct >= 1:
        return 999
    return -400 * math.log10(1 / pct - 1)


def print_table(title: str, results: list[dict]):
    """Pretty-print results table."""
    print(f"\n{'='*80}")
    print(f"  {title}")
    print(f"{'='*80}")
    print(f"{'Rank':>4} {'ID':<45} {'W':>3} {'D':>3} {'L':>3} {'Score':>6} {'Dec%':>5} {'Elo':>6}")
    print(f"{'-'*80}")
    
    for i, r in enumerate(results, 1):
        elo_str = f"{r['elo_diff']:+.0f}" if r['elo_diff'] is not None else "N/A"
        print(
            f"{i:>4} {r['id']:<45} "
            f"{r['our_wins']:>3} {r['draws']:>3} {r['our_losses']:>3} "
            f"{r['our_score_pct']:>5.1%} {r['decisive_rate']:>5.1%} {elo_str:>6}"
        )


def main():
    # Load metadata
    meta_file = RESULTS / "all_candidates.json"
    meta_lookup = {}
    if meta_file.exists():
        all_meta = json.loads(meta_file.read_text())
        meta_lookup = {m["id"]: m for m in all_meta}
    
    white_results = summarize_color("white", RESULTS / "white_screen", meta_lookup)
    black_results = summarize_color("black", RESULTS / "black_screen", meta_lookup)
    
    if white_results:
        print_table("WHITE CANDIDATES (master as White vs sf18)", white_results)
    else:
        print("\nNo white screening results found.")
    
    if black_results:
        print_table("BLACK CANDIDATES (master as Black vs sf18)", black_results)
    else:
        print("\nNo black screening results found.")
    
    # Write JSON summary
    summary = {
        "white_ranked": white_results,
        "black_ranked": black_results,
    }
    out_file = RESULTS / "screening_summary.json"
    out_file.write_text(json.dumps(summary, indent=2) + "\n")
    print(f"\nJSON summary written to {out_file}")
    
    # Print top picks
    if white_results:
        top_w = white_results[0]
        print(f"\n>>> Top White: {top_w['id']} "
              f"(Score: {top_w['our_score_pct']:.1%}, "
              f"W/D/L: {top_w['our_wins']}/{top_w['draws']}/{top_w['our_losses']})")
    if black_results:
        top_b = black_results[0]
        print(f">>> Top Black: {top_b['id']} "
              f"(Score: {top_b['our_score_pct']:.1%}, "
              f"W/D/L: {top_b['our_wins']}/{top_b['draws']}/{top_b['our_losses']})")


if __name__ == "__main__":
    main()
