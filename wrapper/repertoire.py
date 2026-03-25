"""Repertoire access and candidate move selection."""

from __future__ import annotations

import chess

from wrapper.repertoire_db import RepertoireDB
from wrapper.types import CandidateMove


class Repertoire:
    """Position memory backed by a SQLite database."""

    def __init__(self, db: RepertoireDB) -> None:
        self.db = db

    def candidate_moves(self, board: chess.Board, family: str) -> list[CandidateMove]:
        entries = self.db.get_entries(board, family)
        return [
            CandidateMove(
                move_uci=entry.move_uci,
                plan_score_cp=entry.score_cp,
                combined_score_cp=entry.score_cp,
                source="repertoire",
            )
            for entry in entries
            if chess.Move.from_uci(entry.move_uci) in board.legal_moves
        ]

    def remember(self, board: chess.Board, family: str, move_uci: str, score_cp: int, note: str = "") -> None:
        self.db.upsert(board, family, move_uci, score_cp, note)

