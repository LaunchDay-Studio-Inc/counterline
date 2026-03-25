"""Structured decision logging."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import orjson


class TelemetryLogger:
    """Append JSONL telemetry events to disk."""

    def __init__(self, path: Path) -> None:
        self.path = path

    def log(self, event: dict[str, Any]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("ab") as handle:
            handle.write(orjson.dumps(event))
            handle.write(b"\n")

