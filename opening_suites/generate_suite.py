#!/usr/bin/env python3
"""Validate the fixed opening seeds and regenerate the combined EPD suite."""

from __future__ import annotations

from pathlib import Path

import chess
import chess.pgn

ROOT = Path(__file__).resolve().parent

WHITE_EXPECTED = "r1bqrnk1/pp2bppp/2p2n2/3p2B1/3P4/2NBP3/PPQ1NPPP/R4RK1 w - - 9 11"
BLACK_EXPECTED = "rnbq1rk1/pp2bppp/2p5/3p4/2PP4/2PB1N2/P4PPP/R1BQ1RK1 b - - 0 10"


def load_final_board(pgn_path: Path) -> chess.Board:
    with pgn_path.open("r", encoding="utf-8") as handle:
        game = chess.pgn.read_game(handle)
    if game is None:
        raise ValueError(f"failed to parse PGN: {pgn_path}")
    board = game.board()
    for move in game.mainline_moves():
        board.push(move)
    return board


def epd_line(board: chess.Board, name: str) -> str:
    fields = board.fen().split(" ")
    return f"{' '.join(fields[:4])} id \"{name}\";"


def verify_seed(name: str, pgn_path: Path, expected_fen: str) -> tuple[chess.Board, str]:
    board = load_final_board(pgn_path)
    actual = board.fen()
    if actual != expected_fen:
        raise SystemExit(f"{name} FEN mismatch\nexpected: {expected_fen}\nactual:   {actual}")
    return board, epd_line(board, f"{name}_exit")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def main() -> int:
    white_board, white_epd = verify_seed(
        "white_qgd_exchange_carlsbad",
        ROOT / "white/qgd_exchange_carlsbad/seed.pgn",
        WHITE_EXPECTED,
    )
    black_board, black_epd = verify_seed(
        "black_petroff_mainline",
        ROOT / "black/petroff_mainline/seed.pgn",
        BLACK_EXPECTED,
    )

    write_text(ROOT / "white/qgd_exchange_carlsbad/exits.epd", f"{white_epd}\n")
    write_text(ROOT / "black/petroff_mainline/exits.epd", f"{black_epd}\n")
    write_text(ROOT / "combined/suite_fixed.epd", f"{white_epd}\n{black_epd}\n")

    print("validated white seed:", white_board.fen())
    print("validated black seed:", black_board.fen())
    print("wrote combined suite:", ROOT / "combined/suite_fixed.epd")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
