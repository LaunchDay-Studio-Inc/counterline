#!/usr/bin/env python3
"""Generate all chapter figures reproducibly.

This is the master build script for all book diagrams and generated assets.
It calls render_board.py for board diagrams and extract_line_tables.py for
line tables.

Usage:
    cd book/scripts
    python generate_chapter_figures.py
"""

import subprocess
import sys
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
BOOK_DIR = SCRIPTS_DIR.parent
ASSETS_DIR = BOOK_DIR / "assets"


def run(cmd: list[str], label: str) -> bool:
    """Run a command and report success/failure."""
    print(f"  [{label}] {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  FAIL: {result.stderr.strip()}")
        return False
    if result.stdout.strip():
        print(f"  {result.stdout.strip()}")
    return True


def main() -> None:
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)

    ok = True

    # Step 1: Generate board diagrams
    print("=== Step 1: Board Diagrams ===")
    ok &= run(
        [sys.executable, str(SCRIPTS_DIR / "generate_all_diagrams.py")],
        "diagrams"
    )

    # Step 2: Extract line tables from repertoire manifest
    print("\n=== Step 2: Line Tables ===")
    ok &= run(
        [sys.executable, str(SCRIPTS_DIR / "extract_line_tables.py"),
         "--output", str(ASSETS_DIR / "line_tables.md")],
        "line-tables"
    )

    # Step 3: Build opening trees from seed PGNs
    print("\n=== Step 3: Opening Trees ===")
    seed_files = [
        ("White — Vienna Gambit", "opening_suites/final/white/seed.pgn"),
        ("Black — Caro-Kann Classical", "opening_suites/final/black/seed.pgn"),
    ]
    repo_root = BOOK_DIR.parent
    for label, rel_path in seed_files:
        pgn = repo_root / rel_path
        if pgn.exists():
            ok &= run(
                [sys.executable, str(SCRIPTS_DIR / "build_opening_tree.py"),
                 str(pgn), "--label", label],
                f"tree:{label}"
            )
        else:
            print(f"  SKIP: {pgn} not found")

    print()
    if ok:
        print("All figure generation steps completed successfully.")
    else:
        print("Some steps failed — check output above.")
        sys.exit(1)


if __name__ == "__main__":
    main()
