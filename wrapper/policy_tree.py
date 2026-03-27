"""Compact policy tree for exact-line specialist play.

Stores learned moves keyed by move history + FEN, with empirical
performance data against a specific target opponent (SF18).
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Any


@dataclass
class PolicyNode:
    """A node in the learned policy tree."""

    fen: str
    move_history: str  # space-separated UCI moves from game start
    candidate_moves: dict[str, MoveStats] = field(default_factory=dict)
    depth_from_exit: int = 0

    def best_move(self, *, empirical_weight: float = 0.7, wdl_weight: float = 0.3,
                  decisive_bonus: float = 0.1) -> str | None:
        """Return the best candidate move by weighted empirical+WDL score."""
        if not self.candidate_moves:
            return None
        best_uci = None
        best_score = -999999.0
        for uci, stats in self.candidate_moves.items():
            if stats.visits == 0:
                continue
            emp_score = stats.empirical_score
            wdl_score = stats.wdl_score
            decisive = stats.decisive_rate
            combined = (empirical_weight * emp_score +
                        wdl_weight * wdl_score +
                        decisive_bonus * decisive * 100)
            if combined > best_score:
                best_score = combined
                best_uci = uci
        return best_uci

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {
            "fen": self.fen,
            "move_history": self.move_history,
            "depth_from_exit": self.depth_from_exit,
            "candidate_moves": {
                uci: asdict(stats) for uci, stats in self.candidate_moves.items()
            },
        }
        return d

    @classmethod
    def from_dict(cls, d: dict[str, Any]) -> PolicyNode:
        candidates = {}
        for uci, stats_dict in d.get("candidate_moves", {}).items():
            candidates[uci] = MoveStats(**stats_dict)
        return cls(
            fen=d["fen"],
            move_history=d["move_history"],
            depth_from_exit=d.get("depth_from_exit", 0),
            candidate_moves=candidates,
        )


@dataclass
class MoveStats:
    """Statistics for a single candidate move at a policy node."""

    master_eval_cp: int = 0
    predicted_replies: list[str] = field(default_factory=list)
    empirical_score: float = 0.0  # 0-100 scale, % score vs target
    wdl_score: float = 0.0  # 0-100, from master WDL perspective
    decisive_rate: float = 0.0  # 0.0-1.0, fraction of decisive results
    visits: int = 0
    wins: int = 0
    draws: int = 0
    losses: int = 0
    last_verified_ts: float = 0.0

    @property
    def total_games(self) -> int:
        return self.wins + self.draws + self.losses


class PolicyTree:
    """Container for the full learned book/tree."""

    def __init__(self) -> None:
        self.nodes: dict[str, PolicyNode] = {}  # keyed by move_history
        self.exit_fen: str = ""
        self.exit_moves: str = ""
        self.metadata: dict[str, Any] = {}

    def _make_key(self, move_history: str) -> str:
        """Normalize key: strip whitespace, lowercase."""
        return " ".join(move_history.strip().split()).lower()

    def add_node(self, node: PolicyNode) -> None:
        key = self._make_key(node.move_history)
        self.nodes[key] = node

    def get_node(self, move_history: str) -> PolicyNode | None:
        key = self._make_key(move_history)
        return self.nodes.get(key)

    def lookup(self, move_history: str, *, empirical_weight: float = 0.7,
               wdl_weight: float = 0.3, decisive_bonus: float = 0.1) -> str | None:
        """Look up the best move for a given move history."""
        node = self.get_node(move_history)
        if node is None:
            return None
        return node.best_move(
            empirical_weight=empirical_weight,
            wdl_weight=wdl_weight,
            decisive_bonus=decisive_bonus,
        )

    def has_prefix(self, move_history: str) -> bool:
        """Check if the move history matches the exit line prefix."""
        if not self.exit_moves:
            return False
        hist = self._make_key(move_history)
        exit_key = self._make_key(self.exit_moves)
        return exit_key.startswith(hist) or hist.startswith(exit_key) or hist == exit_key

    def is_in_tree(self, move_history: str) -> bool:
        """Check if we have any learned data for this history or prefix."""
        key = self._make_key(move_history)
        if key in self.nodes:
            return True
        # Check if any node's history is a prefix of ours
        for node_key in self.nodes:
            if key.startswith(node_key) or node_key.startswith(key):
                return True
        return False

    def save(self, path: Path) -> None:
        """Save the tree to a JSON file."""
        data = {
            "exit_fen": self.exit_fen,
            "exit_moves": self.exit_moves,
            "metadata": self.metadata,
            "nodes": {k: v.to_dict() for k, v in self.nodes.items()},
        }
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)

    @classmethod
    def load(cls, path: Path) -> PolicyTree:
        """Load a tree from a JSON file."""
        with path.open("r", encoding="utf-8") as f:
            data = json.load(f)
        tree = cls()
        tree.exit_fen = data.get("exit_fen", "")
        tree.exit_moves = data.get("exit_moves", "")
        tree.metadata = data.get("metadata", {})
        for key, node_dict in data.get("nodes", {}).items():
            tree.nodes[key] = PolicyNode.from_dict(node_dict)
        return tree

    def __len__(self) -> int:
        return len(self.nodes)

    def summary(self) -> str:
        total_nodes = len(self.nodes)
        total_candidates = sum(len(n.candidate_moves) for n in self.nodes.values())
        total_visits = sum(
            s.visits for n in self.nodes.values() for s in n.candidate_moves.values()
        )
        return (
            f"PolicyTree: {total_nodes} nodes, {total_candidates} candidates, "
            f"{total_visits} total visits"
        )
