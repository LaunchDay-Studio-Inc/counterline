"""Structure-aware lightweight plan scoring."""

from __future__ import annotations

import chess


def _piece_on(board: chess.Board, piece_type: chess.PieceType, color: chess.Color, square: chess.Square) -> bool:
    piece = board.piece_at(square)
    return piece is not None and piece.piece_type == piece_type and piece.color == color


def _piece_bonus(board: chess.Board, piece_type: chess.PieceType, color: chess.Color, squares: list[chess.Square]) -> int:
    score = 0
    for square in squares:
        if _piece_on(board, piece_type, color, square):
            score += 6
    return score


def _count_pieces(board: chess.Board, piece_type: chess.PieceType, color: chess.Color) -> int:
    return len(board.pieces(piece_type, color))


def score_qgd_exchange(board: chess.Board, move: chess.Move) -> int:
    """Reward typical Carlsbad plans and active white piece play."""

    score = 0

    # Minority attack readiness: pawn pushes a3, b4
    if move.uci() in {"a2a3", "b2b4", "a3a4", "b4b5"}:
        score += 5
    # C-file pressure: Rac1, Rfc1
    if move.uci() in {"a1c1", "f1c1"}:
        score += 4
    # e4 break readiness
    if move.uci() == "f2f3":
        score += 4
    if move.uci() == "e3e4":
        score += 4
    # Knight outpost potential on e5/c5
    if move.to_square in {chess.E5, chess.C5} and board.piece_at(move.from_square) and board.piece_at(move.from_square).piece_type == chess.KNIGHT:
        score += 5
    # Knight development to f4 (targeting d5)
    if move.to_square == chess.F4 and board.piece_at(move.from_square) and board.piece_at(move.from_square).piece_type == chess.KNIGHT:
        score += 4
    # Dark-square bishop trade context: keeping Bg5 is good in many lines
    score += _piece_bonus(board, chess.BISHOP, chess.WHITE, [chess.G5])
    score += _piece_bonus(board, chess.BISHOP, chess.WHITE, [chess.D3])
    score += _piece_bonus(board, chess.KNIGHT, chess.WHITE, [chess.E2, chess.F4])
    # King safety / back-rank slack
    if move.uci() in {"g2g3", "h2h3"}:
        score += 2
    # Rook lift
    if move.uci() in {"f1e1", "a1e1"}:
        score += 3

    return score


def score_petroff(board: chess.Board, move: chess.Move) -> int:
    """Reward solid Petroff structures and simplifying black play."""

    score = 0

    # Central tension and e-file pressure
    if move.uci() in {"f8e8", "a8e8"}:
        score += 4
    # Bishop pair / light-square control
    if move.uci() in {"c8e6", "c8f5", "c8g4"}:
        score += 5
    # Development completion
    if move.uci() in {"b8d7", "b8a6"}:
        score += 4
    # c-pawn majority handling
    if move.uci() in {"d5c4", "c6c5"}:
        score += 4
    # King safety
    if move.uci() in {"h7h6", "g7g6"}:
        score += 2
    # Simplification safety
    if move.to_square in {chess.E6, chess.F6, chess.G4}:
        score += 2
    # Bishop activity
    if move.uci() in {"e7f6", "e7d6"}:
        score += 3
    # Knight moves to active squares
    if move.to_square in {chess.F6, chess.D7, chess.E5} and board.piece_at(move.from_square) and board.piece_at(move.from_square).piece_type == chess.KNIGHT:
        score += 3
    # Piece bonuses for good placement
    score += _piece_bonus(board, chess.BISHOP, chess.BLACK, [chess.E7, chess.D6, chess.E6])
    score += _piece_bonus(board, chess.KNIGHT, chess.BLACK, [chess.F6, chess.D7])
    # Queenside expansion
    if move.uci() in {"a7a5", "b7b5"}:
        score += 2

    return score


def score_move(board: chess.Board, family: str, move: chess.Move) -> int:
    """Return a family-specific heuristic score in centipawn-like units."""

    if family == "qgd_exchange_carlsbad":
        return score_qgd_exchange(board, move)
    if family == "petroff_mainline":
        return score_petroff(board, move)
    return 0

