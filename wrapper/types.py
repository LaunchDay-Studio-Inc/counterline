"""Shared data models for the CounterLine wrapper."""

from __future__ import annotations

from pathlib import Path
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

ColorName = Literal["white", "black"]
FamilyName = Literal["qgd_exchange_carlsbad", "petroff_mainline", "unknown"]


class EngineConfig(BaseModel):
    """Runtime configuration for a Stockfish subprocess."""

    base_path: Path = Path("bin/stockfish-master")
    fallback_path: Path | None = Path("bin/stockfish-sf_18")
    threads: int = 1
    hash_mb: int = 64
    movetime_ms: int = 200
    nodes: int | None = None


class OpeningSpec(BaseModel):
    """Description of a supported fixed opening family."""

    seed_file: Path
    exit_fen: str
    lock_color: ColorName


class WrapperConfig(BaseModel):
    """High-level configuration for CounterLine."""

    db_path: Path = Path("data/repertoire/counterline.sqlite")
    telemetry_path: Path = Path("results/telemetry.jsonl")
    openings: dict[str, OpeningSpec] = Field(default_factory=dict)


class AppConfig(BaseModel):
    """Top-level application configuration."""

    engine: EngineConfig = Field(default_factory=EngineConfig)
    wrapper: WrapperConfig = Field(default_factory=WrapperConfig)


class PositionContext(BaseModel):
    """Current board state plus detected suite information."""

    model_config = ConfigDict(arbitrary_types_allowed=True)

    fen: str
    side_to_move: ColorName
    family: FamilyName = "unknown"
    in_suite: bool = False
    book_complete: bool = False


class CandidateMove(BaseModel):
    """Candidate move evaluation data."""

    move_uci: str
    plan_score_cp: int = 0
    engine_score_cp: int = 0
    combined_score_cp: int = 0
    pv: list[str] = Field(default_factory=list)
    source: str = "base"


class MoveDecision(BaseModel):
    """Final move decision made by the wrapper."""

    bestmove: str
    ponder: str | None = None
    family: FamilyName = "unknown"
    reason: str
    used_wrapper: bool = False
    base_move: str | None = None
    challenger_move: str | None = None


class RepertoireEntry(BaseModel):
    """Persistent position memory keyed by Polyglot hash."""

    polyglot_key: str
    family: FamilyName
    move_uci: str
    score_cp: int = 0
    visits: int = 1
    note: str = ""

