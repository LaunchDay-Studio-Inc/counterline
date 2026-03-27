"""Empirical database for tracking game results against target opponent.

Stores per-position, per-move results from actual engine-vs-engine games
for the White killer line specialist.
"""

from __future__ import annotations

import sqlite3
import time
from dataclasses import dataclass
from pathlib import Path


@dataclass
class EmpiricalRecord:
    """A single record of empirical data for a move at a position."""

    fen: str
    move_history: str
    move_uci: str
    wins: int = 0
    draws: int = 0
    losses: int = 0
    avg_eval_cp: float = 0.0
    best_reply: str = ""
    last_updated: float = 0.0

    @property
    def total(self) -> int:
        return self.wins + self.draws + self.losses

    @property
    def score_pct(self) -> float:
        if self.total == 0:
            return 0.0
        return (self.wins + 0.5 * self.draws) / self.total * 100

    @property
    def decisive_rate(self) -> float:
        if self.total == 0:
            return 0.0
        return (self.wins + self.losses) / self.total


class EmpiricalDB:
    """SQLite-backed empirical results database."""

    def __init__(self, db_path: Path) -> None:
        self.db_path = db_path
        self._conn: sqlite3.Connection | None = None

    @property
    def conn(self) -> sqlite3.Connection:
        if self._conn is None:
            self.db_path.parent.mkdir(parents=True, exist_ok=True)
            self._conn = sqlite3.connect(str(self.db_path))
            self._conn.execute("PRAGMA journal_mode=WAL")
        return self._conn

    def initialize(self) -> None:
        """Create tables if they don't exist."""
        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS empirical (
                move_history TEXT NOT NULL,
                move_uci TEXT NOT NULL,
                fen TEXT NOT NULL,
                wins INTEGER DEFAULT 0,
                draws INTEGER DEFAULT 0,
                losses INTEGER DEFAULT 0,
                avg_eval_cp REAL DEFAULT 0.0,
                best_reply TEXT DEFAULT '',
                last_updated REAL DEFAULT 0.0,
                PRIMARY KEY (move_history, move_uci)
            )
        """)
        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS game_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                move_history TEXT NOT NULL,
                move_uci TEXT NOT NULL,
                result TEXT NOT NULL,
                eval_cp INTEGER DEFAULT 0,
                reply_uci TEXT DEFAULT '',
                timestamp REAL DEFAULT 0.0
            )
        """)
        self.conn.commit()

    def record_result(
        self,
        move_history: str,
        move_uci: str,
        fen: str,
        result: str,
        eval_cp: int = 0,
        reply_uci: str = "",
    ) -> None:
        """Record a game result for a specific move at a position.

        result: '1-0' (white win), '0-1' (white loss), '1/2-1/2' (draw)
        """
        now = time.time()
        # Log the individual game
        self.conn.execute(
            "INSERT INTO game_log (move_history, move_uci, result, eval_cp, reply_uci, timestamp) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (move_history, move_uci, result, eval_cp, reply_uci, now),
        )

        # Update aggregate
        existing = self.get_record(move_history, move_uci)
        if existing is None:
            wins = 1 if result == "1-0" else 0
            draws = 1 if result == "1/2-1/2" else 0
            losses = 1 if result == "0-1" else 0
            self.conn.execute(
                "INSERT INTO empirical (move_history, move_uci, fen, wins, draws, losses, avg_eval_cp, best_reply, last_updated) "
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (move_history, move_uci, fen, wins, draws, losses, float(eval_cp), reply_uci, now),
            )
        else:
            wins = existing.wins + (1 if result == "1-0" else 0)
            draws = existing.draws + (1 if result == "1/2-1/2" else 0)
            losses = existing.losses + (1 if result == "0-1" else 0)
            total = wins + draws + losses
            avg_eval = (existing.avg_eval_cp * existing.total + eval_cp) / total if total > 0 else 0.0
            self.conn.execute(
                "UPDATE empirical SET wins=?, draws=?, losses=?, avg_eval_cp=?, best_reply=?, last_updated=? "
                "WHERE move_history=? AND move_uci=?",
                (wins, draws, losses, avg_eval, reply_uci or existing.best_reply, now,
                 move_history, move_uci),
            )
        self.conn.commit()

    def get_record(self, move_history: str, move_uci: str) -> EmpiricalRecord | None:
        """Get aggregate record for a specific move at a position."""
        row = self.conn.execute(
            "SELECT fen, wins, draws, losses, avg_eval_cp, best_reply, last_updated "
            "FROM empirical WHERE move_history=? AND move_uci=?",
            (move_history, move_uci),
        ).fetchone()
        if row is None:
            return None
        return EmpiricalRecord(
            fen=row[0],
            move_history=move_history,
            move_uci=move_uci,
            wins=row[1],
            draws=row[2],
            losses=row[3],
            avg_eval_cp=row[4],
            best_reply=row[5],
            last_updated=row[6],
        )

    def get_all_for_position(self, move_history: str) -> list[EmpiricalRecord]:
        """Get all move records for a position."""
        rows = self.conn.execute(
            "SELECT move_uci, fen, wins, draws, losses, avg_eval_cp, best_reply, last_updated "
            "FROM empirical WHERE move_history=? ORDER BY (wins + 0.5 * draws) / MAX(wins + draws + losses, 1) DESC",
            (move_history,),
        ).fetchall()
        return [
            EmpiricalRecord(
                fen=row[1],
                move_history=move_history,
                move_uci=row[0],
                wins=row[2],
                draws=row[3],
                losses=row[4],
                avg_eval_cp=row[5],
                best_reply=row[6],
                last_updated=row[7],
            )
            for row in rows
        ]

    def close(self) -> None:
        if self._conn is not None:
            self._conn.close()
            self._conn = None
