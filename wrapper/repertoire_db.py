"""SQLite-backed position memory keyed by Polyglot hashes."""

from __future__ import annotations

import sqlite3
from pathlib import Path

import chess
import chess.polyglot

from wrapper.types import RepertoireEntry


SCHEMA = """
CREATE TABLE IF NOT EXISTS nodes (
    polyglot_key TEXT NOT NULL PRIMARY KEY,
    family TEXT NOT NULL,
    fen TEXT NOT NULL,
    is_critical INTEGER NOT NULL DEFAULT 0,
    structure_tags TEXT NOT NULL DEFAULT '',
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS move_priors (
    polyglot_key TEXT NOT NULL,
    family TEXT NOT NULL,
    move_uci TEXT NOT NULL,
    score_cp INTEGER NOT NULL DEFAULT 0,
    visits INTEGER NOT NULL DEFAULT 1,
    note TEXT NOT NULL DEFAULT '',
    source TEXT NOT NULL DEFAULT 'seed',
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (polyglot_key, family, move_uci)
);

CREATE TABLE IF NOT EXISTS match_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    polyglot_key TEXT NOT NULL,
    family TEXT NOT NULL,
    move_uci TEXT NOT NULL,
    opponent TEXT NOT NULL DEFAULT 'unknown',
    result TEXT NOT NULL DEFAULT '*',
    score_cp INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS overrides (
    polyglot_key TEXT NOT NULL,
    family TEXT NOT NULL,
    move_uci TEXT NOT NULL,
    reason TEXT NOT NULL DEFAULT '',
    active INTEGER NOT NULL DEFAULT 1,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (polyglot_key, family)
);
"""

# Keep backward compat alias
_LEGACY_SCHEMA = """
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
        conn.executescript(SCHEMA)
        conn.executescript(_LEGACY_SCHEMA)
        return conn

    @staticmethod
    def polyglot_key(board: chess.Board) -> str:
        return f"{chess.polyglot.zobrist_hash(board):016x}"

    def initialize(self) -> None:
        with self.connect():
            pass

    def upsert_node(
        self,
        board: chess.Board,
        family: str,
        *,
        is_critical: bool = False,
        structure_tags: str = "",
    ) -> None:
        key = self.polyglot_key(board)
        fen = board.fen(en_passant="fen")
        with self.connect() as conn:
            conn.execute(
                """
                INSERT INTO nodes (polyglot_key, family, fen, is_critical, structure_tags)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(polyglot_key) DO UPDATE SET
                    family=excluded.family,
                    fen=excluded.fen,
                    is_critical=excluded.is_critical,
                    structure_tags=excluded.structure_tags,
                    updated_at=datetime('now')
                """,
                (key, family, fen, int(is_critical), structure_tags),
            )

    def upsert(
        self,
        board: chess.Board,
        family: str,
        move_uci: str,
        score_cp: int,
        note: str = "",
    ) -> None:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            conn.execute(
                """
                INSERT INTO move_priors (polyglot_key, family, move_uci, score_cp, visits, note)
                VALUES (?, ?, ?, ?, 1, ?)
                ON CONFLICT(polyglot_key, family, move_uci) DO UPDATE SET
                    score_cp=excluded.score_cp,
                    visits=move_priors.visits + 1,
                    note=excluded.note,
                    updated_at=datetime('now')
                """,
                (key, family, move_uci, score_cp, note),
            )
            # Also insert into legacy table for backward compat
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
                FROM move_priors
                WHERE polyglot_key = ? AND family = ?
                ORDER BY score_cp DESC, visits DESC, move_uci ASC
                """,
                (key, family),
            ).fetchall()
        if not rows:
            # Fallback to legacy table
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

    def is_critical(self, board: chess.Board) -> bool:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            row = conn.execute(
                "SELECT is_critical FROM nodes WHERE polyglot_key = ?",
                (key,),
            ).fetchone()
        return bool(row and row[0])

    def record_match_result(
        self,
        board: chess.Board,
        family: str,
        move_uci: str,
        opponent: str,
        result: str,
        score_cp: int = 0,
    ) -> None:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            conn.execute(
                """
                INSERT INTO match_results (polyglot_key, family, move_uci, opponent, result, score_cp)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (key, family, move_uci, opponent, result, score_cp),
            )

    def set_override(
        self,
        board: chess.Board,
        family: str,
        move_uci: str,
        reason: str = "",
    ) -> None:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            conn.execute(
                """
                INSERT INTO overrides (polyglot_key, family, move_uci, reason)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(polyglot_key, family) DO UPDATE SET
                    move_uci=excluded.move_uci,
                    reason=excluded.reason,
                    updated_at=datetime('now')
                """,
                (key, family, move_uci, reason),
            )

    def get_override(self, board: chess.Board, family: str) -> str | None:
        key = self.polyglot_key(board)
        with self.connect() as conn:
            row = conn.execute(
                "SELECT move_uci FROM overrides WHERE polyglot_key = ? AND family = ? AND active = 1",
                (key, family),
            ).fetchone()
        return row[0] if row else None

    def dump_all(self) -> dict[str, list[dict]]:
        with self.connect() as conn:
            result: dict[str, list[dict]] = {}
            for table in ["nodes", "move_priors", "match_results", "overrides"]:
                try:
                    cursor = conn.execute(f"SELECT * FROM {table}")  # noqa: S608
                    cols = [d[0] for d in cursor.description]
                    result[table] = [dict(zip(cols, row)) for row in cursor.fetchall()]
                except sqlite3.OperationalError:
                    result[table] = []
        return result

