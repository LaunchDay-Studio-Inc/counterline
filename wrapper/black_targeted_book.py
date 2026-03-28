"""Targeted book for the Black killer line specialist.

Integrates the policy tree with the empirical database to provide
book moves for exact-line play against SF18 as Black.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import chess

from wrapper.empirical_db import EmpiricalDB
from wrapper.policy_tree import PolicyTree, PolicyNode, MoveStats


# Caro-Kann Classical 4...Bf5 seed moves (UCI)
CAROKANN_SEED_UCIS = [
    "e2e4", "c7c6", "d2d4", "d7d5", "b1c3", "d5e4",
    "c3e4", "c8f5", "e4g3", "f5g6", "h2h4", "h7h6",
    "g1f3", "b8d7", "h4h5", "g6h7", "f1d3", "h7d3",
    "d1d3", "e7e6",
]

CAROKANN_EXIT_FEN = "r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq -"


class BlackTargetedBook:
    """Combines policy tree + empirical DB for Black exact-line specialist."""

    def __init__(
        self,
        book_path: Path,
        db_path: Path,
        *,
        empirical_weight: float = 0.6,
        wdl_weight: float = 0.25,
        decisive_bonus: float = 0.15,
        book_depth_limit: int = 40,
    ) -> None:
        self.book_path = book_path
        self.empirical_weight = empirical_weight
        self.wdl_weight = wdl_weight
        self.decisive_bonus = decisive_bonus
        self.book_depth_limit = book_depth_limit

        # Load or create policy tree
        if book_path.exists():
            self.tree = PolicyTree.load(book_path)
        else:
            self.tree = PolicyTree()
            self.tree.exit_fen = CAROKANN_EXIT_FEN
            self.tree.exit_moves = " ".join(CAROKANN_SEED_UCIS)

        # Open empirical DB
        self.empirical_db = EmpiricalDB(db_path)
        self.empirical_db.initialize()

    def is_in_seed_line(self, board: chess.Board) -> bool:
        """Check if the current position is within the Caro-Kann seed line."""
        moves = [m.uci() for m in board.move_stack]
        if len(moves) > len(CAROKANN_SEED_UCIS):
            return False
        return moves == CAROKANN_SEED_UCIS[:len(moves)]

    def get_seed_move(self, board: chess.Board) -> str | None:
        """If in seed line and not yet past exit, return next seed move."""
        moves = [m.uci() for m in board.move_stack]
        if len(moves) >= len(CAROKANN_SEED_UCIS):
            return None
        if moves != CAROKANN_SEED_UCIS[:len(moves)]:
            return None
        next_move = CAROKANN_SEED_UCIS[len(moves)]
        try:
            move = chess.Move.from_uci(next_move)
            if move in board.legal_moves:
                return next_move
        except ValueError:
            pass
        return None

    def is_at_exit(self, board: chess.Board) -> bool:
        """Check if board is at the Caro-Kann exit position."""
        board_fen_parts = board.fen().split()
        exit_fen_parts = CAROKANN_EXIT_FEN.split()
        return board_fen_parts[:4] == exit_fen_parts[:4]

    def _is_exit_root(self, board: chess.Board) -> bool:
        """Check if the board was set up from the exit FEN."""
        root = board.root()
        root_fen_parts = root.fen().split()
        exit_fen_parts = CAROKANN_EXIT_FEN.split()
        return root_fen_parts[:4] == exit_fen_parts[:4]

    def is_past_exit(self, board: chess.Board) -> bool:
        """Check if we're past the exit position."""
        moves = [m.uci() for m in board.move_stack]
        if len(moves) > len(CAROKANN_SEED_UCIS):
            prefix = moves[:len(CAROKANN_SEED_UCIS)]
            if prefix == CAROKANN_SEED_UCIS:
                return True
        if self._is_exit_root(board) and len(moves) > 0:
            return True
        return False

    def move_history_key(self, board: chess.Board) -> str:
        """Get the move history key for lookup."""
        moves = [m.uci() for m in board.move_stack]
        if self._is_exit_root(board):
            return " ".join(CAROKANN_SEED_UCIS + moves)
        return " ".join(moves)

    def lookup_book_move(self, board: chess.Board) -> tuple[str | None, dict[str, Any]]:
        """Look up a Black book move for the current position."""
        # 1. Check seed line
        seed = self.get_seed_move(board)
        if seed is not None:
            return seed, {
                "source": "seed_line",
                "depth_from_exit": -(len(CAROKANN_SEED_UCIS) - len(board.move_stack)),
            }

        # 2. Must be past exit
        if not self.is_past_exit(board):
            return None, {}

        # 3. Check depth limit
        moves = [m.uci() for m in board.move_stack]
        if self._is_exit_root(board):
            depth = len(moves)
        else:
            depth = len(moves) - len(CAROKANN_SEED_UCIS)
        if depth > self.book_depth_limit:
            return None, {}

        # 4. Only play book moves on Black's turn
        if board.turn != chess.BLACK:
            return None, {}

        # 5. Look up in policy tree
        history = self.move_history_key(board)
        move = self.tree.lookup(
            history,
            empirical_weight=self.empirical_weight,
            wdl_weight=self.wdl_weight,
            decisive_bonus=self.decisive_bonus,
        )

        if move is not None:
            try:
                chess_move = chess.Move.from_uci(move)
                if chess_move not in board.legal_moves:
                    move = None
            except ValueError:
                move = None

        if move is not None:
            node = self.tree.get_node(history)
            stats = node.candidate_moves.get(move) if node else None
            meta: dict[str, Any] = {
                "source": "learned_book",
                "depth_from_exit": depth,
                "visits": stats.visits if stats else 0,
                "empirical_score": stats.empirical_score if stats else 0.0,
                "decisive_rate": stats.decisive_rate if stats else 0.0,
            }
            return move, meta

        return None, {}

    def save(self) -> None:
        """Persist the policy tree to disk."""
        self.tree.save(self.book_path)

    def close(self) -> None:
        """Clean up resources."""
        self.empirical_db.close()

    def summary(self) -> str:
        return self.tree.summary()
