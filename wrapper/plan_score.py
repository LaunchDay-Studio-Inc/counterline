"""Structure-aware lightweight plan scoring."""

from __future__ import annotations

import chess


def _piece_bonus(board: chess.Board, piece_type: chess.PieceType, color: chess.Color, squares: list[chess.Square]) -> int:
    score = 0
    for square in squares:
        piece = board.piece_at(square)
        if piece and piece.piece_type == piece_type and piece.color == color:
            score += 6
    return score


def score_qgd_exchange(board: chess.Board, move: chess.Move) -> int:
    """Reward typical Carlsbad plans and active white piece play."""

    score = 0
    if move.uci() in {"f2f3", "a2a3", "b2b4", "f1e1"}:
        score += 10
    if move.to_square in {chess.E1, chess.C5, chess.H4}:
        score += 4
    score += _piece_bonus(board, chess.BISHOP, chess.WHITE, [chess.G5, chess.D3])
    score += _piece_bonus(board, chess.KNIGHT, chess.WHITE, [chess.E2, chess.F4])
    return score


def score_petroff(board: chess.Board, move: chess.Move) -> int:
    """Reward solid Petroff structures and simplifying black play."""

    score = 0
    if move.uci() in {"c8e6", "d5c4", "e7f6", "b8d7"}:
        score += 10
    if move.to_square in {chess.E6, chess.F6, chess.G4}:
        score += 4
    score += _piece_bonus(board, chess.BISHOP, chess.BLACK, [chess.E7, chess.D6])
    score += _piece_bonus(board, chess.KNIGHT, chess.BLACK, [chess.F6, chess.D7])
    return score


def score_move(board: chess.Board, family: str, move: chess.Move) -> int:
    """Return a family-specific heuristic score in centipawn-like units."""

    if family == "qgd_exchange_carlsbad":
        return score_qgd_exchange(board, move)
    if family == "petroff_mainline":
        return score_petroff(board, move)
    return 0

