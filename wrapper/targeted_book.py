"""Targeted book for the White killer line specialist.

Integrates the policy tree with the empirical database to provide
book moves for exact-line play against SF18.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import chess

from wrapper.empirical_db import EmpiricalDB
from wrapper.policy_tree import PolicyTree, PolicyNode, MoveStats


# Vienna Gambit Accepted seed moves (UCI)
VIENNA_SEED_UCIS = [
    "e2e4", "e7e5", "b1c3", "g8f6", "f2f4", "e5f4",
    "e4e5", "f6g8", "g1f3", "d7d6", "d2d4", "d6e5",
    "d1e2", "f8b4", "e2e5", "d8e7", "c1f4", "b4c3",
    "b2c3",
]

VIENNA_EXIT_FEN = "rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq -"


class TargetedBook:
    """Combines policy tree + empirical DB for exact-line specialist moves."""

    def __init__(
        self,
        book_path: Path,
        db_path: Path,
        *,
        empirical_weight: float = 0.7,
        wdl_weight: float = 0.3,
        decisive_bonus: float = 0.1,
        book_depth_limit: int = 30,
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
            self.tree.exit_fen = VIENNA_EXIT_FEN
            self.tree.exit_moves = " ".join(VIENNA_SEED_UCIS)

        # Open empirical DB
        self.empirical_db = EmpiricalDB(db_path)
        self.empirical_db.initialize()

    def is_in_seed_line(self, board: chess.Board) -> bool:
        """Check if the current position is within the Vienna Gambit seed line."""
        moves = [m.uci() for m in board.move_stack]
        if len(moves) > len(VIENNA_SEED_UCIS):
            return False
        return moves == VIENNA_SEED_UCIS[:len(moves)]

    def get_seed_move(self, board: chess.Board) -> str | None:
        """If in seed line and not yet past exit, return next seed move."""
        moves = [m.uci() for m in board.move_stack]
        if len(moves) >= len(VIENNA_SEED_UCIS):
            return None
        if moves != VIENNA_SEED_UCIS[:len(moves)]:
            return None
        next_move = VIENNA_SEED_UCIS[len(moves)]
        # Verify it's legal
        try:
            move = chess.Move.from_uci(next_move)
            if move in board.legal_moves:
                return next_move
        except ValueError:
            pass
        return None

    def is_at_exit(self, board: chess.Board) -> bool:
        """Check if board is at the Vienna exit position."""
        # Normalize FEN comparison (ignore move counters)
        board_fen_parts = board.fen().split()
        exit_fen_parts = VIENNA_EXIT_FEN.split()
        return board_fen_parts[:4] == exit_fen_parts[:4]

    def _is_exit_root(self, board: chess.Board) -> bool:
        """Check if the board was set up from the exit FEN (no seed prefix in move_stack)."""
        root = board.root()
        root_fen_parts = root.fen().split()
        exit_fen_parts = VIENNA_EXIT_FEN.split()
        return root_fen_parts[:4] == exit_fen_parts[:4]

    def is_past_exit(self, board: chess.Board) -> bool:
        """Check if we're past the exit position (in the learned subtree)."""
        # Case 1: game started from startpos with full seed line
        moves = [m.uci() for m in board.move_stack]
        if len(moves) > len(VIENNA_SEED_UCIS):
            prefix = moves[:len(VIENNA_SEED_UCIS)]
            if prefix == VIENNA_SEED_UCIS:
                return True

        # Case 2: game started from exit FEN (e.g., fastchess EPD)
        if self._is_exit_root(board) and len(moves) > 0:
            return True

        return False

    def move_history_key(self, board: chess.Board) -> str:
        """Get the move history key for lookup.

        If the board started from the exit FEN, prepend the seed moves
        so our book keys match.
        """
        moves = [m.uci() for m in board.move_stack]
        if self._is_exit_root(board):
            return " ".join(VIENNA_SEED_UCIS + moves)
        return " ".join(moves)

    def lookup_book_move(self, board: chess.Board) -> tuple[str | None, dict[str, Any]]:
        """Look up a book move for the current position.

        Returns (move_uci, metadata) or (None, {}).
        """
        # 1. Check seed line
        seed = self.get_seed_move(board)
        if seed is not None:
            return seed, {"source": "seed_line", "depth_from_exit": -(len(VIENNA_SEED_UCIS) - len(board.move_stack))}

        # 2. Must be past exit to use learned book
        if not self.is_past_exit(board):
            return None, {}

        # 3. Check depth limit
        depth = len(board.move_stack) - len(VIENNA_SEED_UCIS)
        if depth > self.book_depth_limit:
            return None, {}

        # 4. Only play book moves on White's turn
        if board.turn != chess.WHITE:
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
            # Verify legality
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

    def update_from_game(
        self,
        board: chess.Board,
        move_uci: str,
        result: str,
        eval_cp: int = 0,
        reply_uci: str = "",
    ) -> None:
        """Update the book with a game result."""
        history = self.move_history_key(board)
        fen = board.fen()

        # Update empirical DB
        self.empirical_db.record_result(
            move_history=history,
            move_uci=move_uci,
            fen=fen,
            result=result,
            eval_cp=eval_cp,
            reply_uci=reply_uci,
        )

        # Update policy tree node
        node = self.tree.get_node(history)
        if node is None:
            depth = max(0, len(board.move_stack) - len(VIENNA_SEED_UCIS))
            node = PolicyNode(
                fen=fen,
                move_history=history,
                depth_from_exit=depth,
            )
            self.tree.add_node(node)

        if move_uci not in node.candidate_moves:
            node.candidate_moves[move_uci] = MoveStats()

        stats = node.candidate_moves[move_uci]
        if result == "1-0":
            stats.wins += 1
        elif result == "0-1":
            stats.losses += 1
        else:
            stats.draws += 1

        total = stats.total_games
        if total > 0:
            stats.empirical_score = (stats.wins + 0.5 * stats.draws) / total * 100
            stats.decisive_rate = (stats.wins + stats.losses) / total
        stats.visits = total
        if reply_uci and reply_uci not in stats.predicted_replies:
            stats.predicted_replies.append(reply_uci)

    def save(self) -> None:
        """Persist the policy tree to disk."""
        self.tree.save(self.book_path)

    def close(self) -> None:
        """Clean up resources."""
        self.empirical_db.close()

    def summary(self) -> str:
        return self.tree.summary()
