"""Structured decision logging."""

from __future__ import annotations

import time
from pathlib import Path
from typing import Any

import orjson

from wrapper.types import MoveDecision


class TelemetryLogger:
    """Append JSONL telemetry events to disk."""

    def __init__(self, path: Path) -> None:
        self.path = path

    def log(self, event: dict[str, Any]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("ab") as handle:
            handle.write(orjson.dumps(event))
            handle.write(b"\n")

    def log_decision(
        self,
        *,
        fen: str,
        family: str,
        decision: MoveDecision,
        thread_budget: int = 1,
        source_of_priors: str = "",
    ) -> None:
        event = {
            "ts": time.time(),
            "fen": fen,
            "family": family,
            "base_move": decision.base_move,
            "challenger_move": decision.challenger_move,
            "bestmove": decision.bestmove,
            "used_override": decision.used_wrapper,
            "reason": decision.reason,
            "scores": decision.scores,
            "time_ms": decision.time_ms,
            "thread_budget": thread_budget,
            "source_of_priors": source_of_priors,
        }
        self.log(event)

