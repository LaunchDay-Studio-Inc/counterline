#!/usr/bin/env python3
"""Build an opening tree from PGN seed files and render as a text/Markdown tree.

Usage:
    python build_opening_tree.py <pgn_or_epd> [--depth 10] [--format md]

Examples:
    python build_opening_tree.py ../../opening_suites/final/white/seed.pgn
    python build_opening_tree.py ../../opening_suites/final/black/seed.pgn --format text
"""

import argparse
import io
import sys
from pathlib import Path

import chess
import chess.pgn


def pgn_to_moves(pgn_path: str) -> list[str]:
    """Extract the mainline moves from a PGN file."""
    with open(pgn_path) as f:
        game = chess.pgn.read_game(f)
    if game is None:
        raise ValueError(f"No game found in {pgn_path}")
    board = game.board()
    moves = []
    for move in game.mainline_moves():
        san = board.san(move)
        moves.append(san)
        board.push(move)
    return moves


def moves_to_tree_md(moves: list[str], label: str = "") -> str:
    """Render a move list as a Markdown tree with move numbers."""
    lines = []
    if label:
        lines.append(f"## {label}\n")
    lines.append("```")
    move_num = 1
    for i, san in enumerate(moves):
        if i % 2 == 0:
            prefix = f"{move_num}."
            indent = "  " * (i // 2)
            lines.append(f"{indent}{prefix} {san}")
        else:
            indent = "  " * (i // 2)
            lines.append(f"{indent}   ...{san}")
            move_num += 1
    lines.append("```")
    return "\n".join(lines)


def moves_to_tree_text(moves: list[str]) -> str:
    """Render as plain text move list."""
    parts = []
    move_num = 1
    for i, san in enumerate(moves):
        if i % 2 == 0:
            parts.append(f"{move_num}.{san}")
        else:
            parts.append(san)
            move_num += 1
    return " ".join(parts)


def main() -> None:
    parser = argparse.ArgumentParser(description="Build opening tree from PGN")
    parser.add_argument("input", help="PGN file with seed line")
    parser.add_argument("--depth", type=int, default=0,
                        help="Max ply depth (0 = all)")
    parser.add_argument("--format", choices=["md", "text"], default="md",
                        help="Output format")
    parser.add_argument("--label", default="",
                        help="Tree label/title")
    args = parser.parse_args()

    moves = pgn_to_moves(args.input)
    if args.depth > 0:
        moves = moves[:args.depth]

    label = args.label or Path(args.input).stem

    if args.format == "md":
        print(moves_to_tree_md(moves, label))
    else:
        print(moves_to_tree_text(moves))


if __name__ == "__main__":
    main()
