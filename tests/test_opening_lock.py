from __future__ import annotations

import chess

from wrapper.opening_lock import (
    BLACK_EXIT_FEN,
    WHITE_EXIT_FEN,
    detect_opening_family,
    get_seed_move,
    is_book_complete,
    opening_lock,
)


def board_from_sans(moves: list[str]) -> chess.Board:
    board = chess.Board()
    for san in moves:
        board.push_san(san)
    return board


def test_white_seed_detects_family_and_exit() -> None:
    board = board_from_sans(
        ["d4", "d5", "c4", "e6", "Nc3", "Nf6", "cxd5", "exd5", "Bg5", "c6", "e3", "Be7", "Bd3", "Nbd7", "Qc2", "O-O", "Nge2", "Re8", "O-O", "Nf8"]
    )
    assert board.fen() == WHITE_EXIT_FEN
    assert detect_opening_family(board) == "qgd_exchange_carlsbad"
    assert is_book_complete(board) is True
    assert opening_lock(board) == (True, "qgd_exchange_carlsbad")


def test_black_seed_detects_family_and_exit() -> None:
    board = board_from_sans(
        ["e4", "e5", "Nf3", "Nf6", "Nxe5", "d6", "Nf3", "Nxe4", "d4", "d5", "Bd3", "Be7", "O-O", "O-O", "c4", "c6", "Nc3", "Nxc3", "bxc3"]
    )
    assert board.fen() == BLACK_EXIT_FEN
    assert detect_opening_family(board) == "petroff_mainline"
    assert is_book_complete(board) is True


def test_unknown_opening_stays_outside_suite() -> None:
    board = board_from_sans(["e4", "c5", "Nf3", "d6"])
    assert detect_opening_family(board) == "unknown"
    assert opening_lock(board) == (False, "unknown")


def test_seed_move_returns_next_uci_in_prefix() -> None:
    board = board_from_sans(["d4", "d5", "c4"])
    assert detect_opening_family(board) == "qgd_exchange_carlsbad"
    seed = get_seed_move(board)
    assert seed == "e7e6"


def test_seed_move_none_at_exit() -> None:
    board = board_from_sans(
        ["d4", "d5", "c4", "e6", "Nc3", "Nf6", "cxd5", "exd5", "Bg5", "c6", "e3", "Be7", "Bd3", "Nbd7", "Qc2", "O-O", "Nge2", "Re8", "O-O", "Nf8"]
    )
    assert get_seed_move(board) is None


def test_seed_move_none_for_unknown() -> None:
    board = board_from_sans(["e4", "c5"])
    assert get_seed_move(board) is None

