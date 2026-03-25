from __future__ import annotations

from pathlib import Path

import chess

from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB


def test_repertoire_db_roundtrip(tmp_path: Path) -> None:
    board = chess.Board("r1bqrnk1/pp2bppp/2p2n2/3p2B1/3P4/2NBP3/PPQ1NPPP/R4RK1 w - - 9 11")
    db = RepertoireDB(tmp_path / "counterline.sqlite")
    db.initialize()
    repertoire = Repertoire(db)

    repertoire.remember(board, "qgd_exchange_carlsbad", "f2f3", 18, note="test")
    repertoire.remember(board, "qgd_exchange_carlsbad", "a2a3", 12, note="test")

    moves = repertoire.candidate_moves(board, "qgd_exchange_carlsbad")
    assert [move.move_uci for move in moves] == ["f2f3", "a2a3"]


def test_polyglot_key_is_stable(tmp_path: Path) -> None:
    board = chess.Board()
    db = RepertoireDB(tmp_path / "counterline.sqlite")
    first = db.polyglot_key(board)
    second = db.polyglot_key(chess.Board())
    assert first == second

