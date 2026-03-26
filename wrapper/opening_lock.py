"""Opening family lock and suite membership detection."""

from __future__ import annotations

import chess

WHITE_EXIT_FEN = "r1bqrnk1/pp2bppp/2p2n2/3p2B1/3P4/2NBP3/PPQ1NPPP/R4RK1 w - - 9 11"
BLACK_EXIT_FEN = "rnbq1rk1/pp2bppp/2p5/3p4/2PP4/2PB1N2/P4PPP/R1BQ1RK1 b - - 0 10"

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
    "d7f8",
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
    if not moves:
        return False
    return len(moves) <= len(prefix) and moves == prefix[: len(moves)]


def detect_opening_family(board: chess.Board) -> str:
    """Return the supported opening family for the current position."""

    fen = _normalize_fen(board)
    if fen == WHITE_EXIT_FEN or _moves_match_prefix(board, WHITE_SEED_UCIS):
        return "qgd_exchange_carlsbad"
    if fen == BLACK_EXIT_FEN or _moves_match_prefix(board, BLACK_SEED_UCIS):
        return "petroff_mainline"
    # Structural detection for positions that evolved from the exit FEN
    family = _detect_by_structure(board)
    if family != "unknown":
        return family
    return "unknown"


def _detect_by_structure(board: chess.Board) -> str:
    """Detect opening family by pawn structure and piece configuration."""

    # QGD Exchange / Carlsbad detection:
    # Key features: d4/d5 pawns, c6 pawn, e3 pawn, symmetric pawn structure
    # White has pawns on d4,e3; Black has pawns on c6,d5
    white_pawns = board.pieces(chess.PAWN, chess.WHITE)
    black_pawns = board.pieces(chess.PAWN, chess.BLACK)

    d4_pawn = chess.D4 in white_pawns
    e3_pawn = chess.E3 in white_pawns
    c6_pawn = chess.C6 in black_pawns
    d5_pawn = chess.D5 in black_pawns

    # Carlsbad structure: d4 vs d5, c-file half-open for white, e3 pawn
    if d4_pawn and d5_pawn and e3_pawn and c6_pawn:
        # Additional check: no c-file pawns for white (exchange happened)
        c_file_white_pawns = white_pawns & chess.BB_FILE_C
        if not c_file_white_pawns:
            return "qgd_exchange_carlsbad"

    # Petroff detection:
    # Key features: d5 pawn for black, c4/d4 pawns for white, c3 pawn for white
    # Open e-file, black kingside castled
    c4_pawn = chess.C4 in white_pawns
    c3_pawn = chess.C3 in white_pawns
    d4_pawn_w = chess.D4 in white_pawns

    if d5_pawn and c3_pawn and c6_pawn and (c4_pawn or d4_pawn_w):
        # Check for Petroff-specific: no e-file pawns
        e_file_white = white_pawns & chess.BB_FILE_E
        e_file_black = black_pawns & chess.BB_FILE_E
        if not e_file_white and not e_file_black:
            return "petroff_mainline"

    return "unknown"


def is_book_complete(board: chess.Board) -> bool:
    """True once the exact post-book exit position has been reached or surpassed."""

    fen = _normalize_fen(board)
    if fen in {WHITE_EXIT_FEN, BLACK_EXIT_FEN}:
        return True
    n_moves = len(board.move_stack)
    if _moves_match_prefix(board, WHITE_SEED_UCIS) and n_moves >= len(WHITE_SEED_UCIS):
        return True
    if _moves_match_prefix(board, BLACK_SEED_UCIS) and n_moves >= len(BLACK_SEED_UCIS):
        return True
    return False


def get_seed_move(board: chess.Board) -> str | None:
    """If the position is exactly on a seed line prefix with one forced move, return it."""

    moves = [move.uci() for move in board.move_stack]
    n = len(moves)

    if _moves_match_prefix(board, WHITE_SEED_UCIS) and n < len(WHITE_SEED_UCIS):
        next_uci = WHITE_SEED_UCIS[n]
        move = chess.Move.from_uci(next_uci)
        if move in board.legal_moves:
            return next_uci
        return None

    if _moves_match_prefix(board, BLACK_SEED_UCIS) and n < len(BLACK_SEED_UCIS):
        next_uci = BLACK_SEED_UCIS[n]
        move = chess.Move.from_uci(next_uci)
        if move in board.legal_moves:
            return next_uci
        return None

    return None


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
