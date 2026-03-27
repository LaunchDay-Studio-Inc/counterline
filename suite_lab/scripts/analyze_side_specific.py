#!/usr/bin/env python3
"""Side-specific analysis of screening results.

The raw W/D/L from fastchess logs counts from engine1's perspective across all
games, regardless of which physical side (White/Black) each engine plays.
In the real deployment, our engine (master) always plays a fixed side:
  - White candidates: master is always White
  - Black candidates: master is always Black

This script parses PGN files to compute side-specific win rates, which reflect
the true expected performance in deployment.
"""

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
RESULTS = REPO / "suite_lab" / "results"


def parse_pgn_games(pgn_path: Path) -> list[dict]:
    """Parse PGN and extract per-game White/Black engine names and result."""
    text = pgn_path.read_text()
    games = []

    # Split on game boundaries (blank line before [Event)
    game_blocks = re.split(r'\n(?=\[Event )', text.strip())

    for block in game_blocks:
        white_m = re.search(r'\[White "([^"]+)"\]', block)
        black_m = re.search(r'\[Black "([^"]+)"\]', block)
        result_m = re.search(r'\[Result "([^"]+)"\]', block)

        if white_m and black_m and result_m:
            games.append({
                "white": white_m.group(1),
                "black": black_m.group(1),
                "result": result_m.group(1),
            })
    return games


def compute_side_stats(games: list[dict], our_engine: str, our_side: str) -> dict:
    """Compute stats when our_engine plays our_side (White or Black).

    Returns dict with wins/draws/losses/total/score for games where
    our_engine was on our_side.
    """
    w, d, l = 0, 0, 0
    for g in games:
        if our_side == "White" and g["white"] == our_engine:
            if g["result"] == "1-0":
                w += 1
            elif g["result"] == "0-1":
                l += 1
            elif g["result"] == "1/2-1/2":
                d += 1
        elif our_side == "Black" and g["black"] == our_engine:
            if g["result"] == "0-1":
                w += 1
            elif g["result"] == "1-0":
                l += 1
            elif g["result"] == "1/2-1/2":
                d += 1
    total = w + d + l
    score = (w + 0.5 * d) / total if total > 0 else 0
    return {"wins": w, "draws": d, "losses": l, "total": total, "score": score}


def elo_diff(pct: float) -> str:
    import math
    if pct <= 0:
        return "-inf"
    if pct >= 1:
        return "+inf"
    return f"{-400 * math.log10(1 / pct - 1):+.0f}"


def analyze_dir(screen_dir: Path, color: str, our_engine: str = "master"):
    """Analyze all PGN files in a screening directory."""
    our_side = "White" if color == "white" else "Black"
    pgn_files = sorted(screen_dir.glob("*.pgn"))
    if not pgn_files:
        print(f"  No PGN files in {screen_dir}")
        return []

    results = []
    for pgn in pgn_files:
        cand_id = pgn.stem
        games = parse_pgn_games(pgn)
        if not games:
            continue

        # Overall stats (all games, from engine1 perspective)
        overall_w = sum(1 for g in games if g["result"] == "1-0")
        overall_d = sum(1 for g in games if g["result"] == "1/2-1/2")
        overall_l = sum(1 for g in games if g["result"] == "0-1")

        # Side-specific stats
        side = compute_side_stats(games, our_engine, our_side)

        results.append({
            "id": cand_id,
            "total_games": len(games),
            "overall": f"{overall_w}W-{overall_d}D-{overall_l}L",
            "side_games": side["total"],
            "side_w": side["wins"],
            "side_d": side["draws"],
            "side_l": side["losses"],
            "side_score": side["score"],
        })

    # Sort by side-specific score descending
    results.sort(key=lambda x: x["side_score"], reverse=True)
    return results


def main():
    print("=" * 90)
    print("  SIDE-SPECIFIC SCREENING ANALYSIS")
    print("  (Score = master's win rate when playing the candidate's target side)")
    print("=" * 90)

    for color, dirname in [("white", "white_screen"), ("black", "black_screen")]:
        screen_dir = RESULTS / dirname
        if not screen_dir.exists():
            continue

        our_side = "White" if color == "white" else "Black"
        results = analyze_dir(screen_dir, color)

        if not results:
            print(f"\n  No {color} results.")
            continue

        print(f"\n{'─' * 90}")
        print(f"  {color.upper()} CANDIDATES — master as {our_side}")
        print(f"{'─' * 90}")
        print(f"{'#':>2} {'Candidate':<45} {'Games':>5} {'SideW':>5} {'SideD':>5} {'SideL':>5} {'Score':>7} {'Elo':>6}")
        print(f"{'─' * 90}")

        for i, r in enumerate(results, 1):
            elo = elo_diff(r["side_score"]) if r["side_games"] >= 4 else "N/A"
            print(
                f"{i:>2} {r['id']:<45} "
                f"{r['total_games']:>5} "
                f"{r['side_w']:>5} {r['side_d']:>5} {r['side_l']:>5} "
                f"{r['side_score']:>6.1%} {elo:>6}"
            )

    print()


if __name__ == "__main__":
    main()
