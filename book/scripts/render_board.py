#!/usr/bin/env python3
"""Render chess board diagrams from FEN strings to SVG and PNG.

Usage:
    python render_board.py <fen> <output_base> [--size 400] [--flip]

Example:
    python render_board.py \
        "rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10" \
        diagrams/vienna_exit --size 400
"""

import argparse
import sys
from pathlib import Path

import chess
import chess.svg
import cairosvg


def render_board(fen: str, output_base: str, size: int = 400, flip: bool = False,
                 lastmove: str | None = None, arrows: list[str] | None = None) -> None:
    board = chess.Board(fen)

    lm = None
    if lastmove:
        lm = chess.Move.from_uci(lastmove)

    arrow_objs = []
    if arrows:
        for a in arrows:
            tail = chess.parse_square(a[:2])
            head = chess.parse_square(a[2:4])
            arrow_objs.append(chess.svg.Arrow(tail, head, color="#88cc4488"))

    svg_data = chess.svg.board(
        board,
        size=size,
        flipped=flip,
        lastmove=lm,
        arrows=arrow_objs,
        coordinates=True,
    )

    out = Path(output_base)
    out.parent.mkdir(parents=True, exist_ok=True)

    svg_path = out.with_suffix(".svg")
    svg_path.write_text(svg_data)

    png_path = out.with_suffix(".png")
    cairosvg.svg2png(bytestring=svg_data.encode(), write_to=str(png_path),
                     output_width=size, output_height=size)

    print(f"  SVG → {svg_path}")
    print(f"  PNG → {png_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Render chess board diagrams")
    parser.add_argument("fen", help="FEN string")
    parser.add_argument("output", help="Output base path (without extension)")
    parser.add_argument("--size", type=int, default=400, help="Board size in pixels")
    parser.add_argument("--flip", action="store_true", help="Flip board (Black's perspective)")
    parser.add_argument("--lastmove", help="UCI move to highlight (e.g. e2e4)")
    parser.add_argument("--arrows", nargs="*", help="Arrow specifications (e.g. e2e4 d7d5)")
    args = parser.parse_args()

    render_board(args.fen, args.output, args.size, args.flip, args.lastmove, args.arrows)


if __name__ == "__main__":
    main()
