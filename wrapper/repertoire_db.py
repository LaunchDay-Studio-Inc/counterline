"""SQLite-backed position memory keyed by Polyglot hashes."""

from __future__ import annotations

import sqlite3
from pathlib import Path

import chess
import chess.polyglot

from wrapper.types import RepertoireEntry


SCHEMA = """
CREATE TABLE IF NOT EXISTS repertoire (
    polyglot_key TEXT NOT NULL,
    family TEXT NOT NULL,
    move_uci TEXT NOT NULL,
    score_cp INTEGER NOT NULL DEFAULT 0,
    visits INTEGER NOT NULL DEFAULT 1,
    note TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (polyglot_key, family, move_uci)
);
"""


class RepertoireDB:
    """Small SQLite helper for position-specific wrapper memory."""

    def __init__(self, path: Path) -> None:
        self.path = path

    def connect(self) -> sqlite3.Connection:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        conn = sqlite3.connect(self.path)
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute(SCHEMA)
        return conn

    @staticmethod
    def polyglot_key(board: chess.Board) -> str:
        return f"{chess.polyglot.zobrist_hash(board):016x}"

    def initialize(self) -> None:
        with self.connect() as conn:
            conn.execute(SCHEMA)

    def upsert(self, board: chess.Board, family: str, move_uci: str, score_cp: int, note: str = "") -> None:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            conn.execute(
                """
                INSERT INTO repertoire (polyglot_key, family, move_uci, score_cp, visits, note)
                VALUES (?, ?, ?, ?, 1, ?)
                ON CONFLICT(polyglot_key, family, move_uci) DO UPDATE SET
                    score_cp=excluded.score_cp,
                    visits=repertoire.visits + 1,
                    note=excluded.note
                """,
                (key, family, move_uci, score_cp, note),
            )

    def get_entries(self, board: chess.Board, family: str) -> list[RepertoireEntry]:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            rows = conn.execute(
                """
                SELECT polyglot_key, family, move_uci, score_cp, visits, note
                FROM repertoire
                WHERE polyglot_key = ? AND family = ?
                ORDER BY score_cp DESC, visits DESC, move_uci ASC
                """,
                (key, family),
            ).fetchall()
        return [
            RepertoireEntry(
                polyglot_key=row[0],
                family=row[1],
                move_uci=row[2],
                score_cp=row[3],
                visits=row[4],
                note=row[5],
            )
            for row in rows
        ]

