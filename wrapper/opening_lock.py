"""Opening family lock and suite membership detection."""

from __future__ import annotations

import chess

WHITE_EXIT_FEN = "r1bqr1k1/pp2bppp/2p2n2/3p2B1/3P4/3BPN2/PPQ2PPP/R4RK1 w - - 0 11"
BLACK_EXIT_FEN = "r1bq1rk1/pp2bppp/2p5/3p4/2PP4/2PB4/P4PPP/R1BQ1RK1 b - - 0 10"

WHITE_SEED_UCIS = [
    "d2d4",
    "d7d5",
    "c2c4",
    "e7e6",
    "b1c3",
    "g8f6",
    "c4d5",
    "e6d5",
    "c1g5",
    "c7c6",
    "e2e3",
    "f8e7",
    "f1d3",
    "b8d7",
    "d1c2",
    "e8g8",
    "g1e2",
    "f8e8",
    "e1g1",
    "f6f8",
]

BLACK_SEED_UCIS = [
    "e2e4",
    "e7e5",
    "g1f3",
    "g8f6",
    "f3e5",
    "d7d6",
    "e5f3",
    "f6e4",
    "d2d4",
    "d6d5",
    "f1d3",
    "f8e7",
    "e1g1",
    "e8g8",
    "c2c4",
    "c7c6",
    "b1c3",
    "e4c3",
    "b2c3",
]


def _normalize_fen(board: chess.Board) -> str:
    return board.fen(en_passant="fen")


def _moves_match_prefix(board: chess.Board, prefix: list[str]) -> bool:
    moves = [move.uci() for move in board.move_stack]
    return len(moves) <= len(prefix) and moves == prefix[: len(moves)]


def detect_opening_family(board: chess.Board) -> str:
    """Return the supported opening family for the current position."""

    fen = _normalize_fen(board)
    if fen == WHITE_EXIT_FEN or _moves_match_prefix(board, WHITE_SEED_UCIS):
        return "qgd_exchange_carlsbad"
    if fen == BLACK_EXIT_FEN or _moves_match_prefix(board, BLACK_SEED_UCIS):
        return "petroff_mainline"
    return "unknown"


def is_book_complete(board: chess.Board) -> bool:
    """True once the exact post-book exit position has been reached."""

    fen = _normalize_fen(board)
    return fen in {WHITE_EXIT_FEN, BLACK_EXIT_FEN}


def opening_lock(board: chess.Board) -> tuple[bool, str]:
    """Return whether the position is inside the supported suite and its family."""

    family = detect_opening_family(board)
    return family != "unknown", family


def lock_color_for_family(family: str) -> str | None:
    """Return the side the wrapper is intended to control for the family."""

    if family == "qgd_exchange_carlsbad":
        return "white"
    if family == "petroff_mainline":
        return "black"
    return None
