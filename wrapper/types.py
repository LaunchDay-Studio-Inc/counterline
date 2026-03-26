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
    nominal_path: Path | None = None
    verify_path: Path | None = None
    threads: int = 1
    hash_mb: int = 64
    movetime_ms: int = 200
    nodes_main: int | None = None
    nodes_child: int | None = None
    nodes_verify: int | None = None
    show_wdl: bool = False


class OpeningSpec(BaseModel):
    """Description of a supported fixed opening family."""

    seed_file: Path
    exit_fen: str
    lock_color: ColorName


class OpponentProfile(BaseModel):
    """Nominal opponent description for rollout simulation."""

    name: str = "stockfish-master"
    strength: str = "default"


class WrapperConfig(BaseModel):
    """High-level configuration for CounterLine."""

    db_path: Path = Path("data/repertoire/counterline.sqlite")
    telemetry_path: Path = Path("results/telemetry.jsonl")
    openings: dict[str, OpeningSpec] = Field(default_factory=dict)
    max_candidates: int = 4
    opening_lock: bool = True
    wrapper_mode: str = "selective"
    opponent_profile: OpponentProfile = Field(default_factory=OpponentProfile)


class AppConfig(BaseModel):
    """Top-level application configuration."""

    engine: EngineConfig = Field(default_factory=EngineConfig)
    wrapper: WrapperConfig = Field(default_factory=WrapperConfig)


class NodeScore(BaseModel):
    """Score from a single engine analysis."""

    score_cp: int = 0
    wdl: tuple[int, int, int] | None = None
    depth: int = 0
    pv: list[str] = Field(default_factory=list)
    nodes: int = 0


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
    rollout_score_cp: int = 0
    empirical_prior_cp: int = 0
    risk_penalty_cp: int = 0
    combined_score_cp: int = 0
    pv: list[str] = Field(default_factory=list)
    source: str = "base"
    instability: float = 0.0


class MoveDecision(BaseModel):
    """Final move decision made by the wrapper."""

    bestmove: str
    ponder: str | None = None
    family: FamilyName = "unknown"
    reason: str
    used_wrapper: bool = False
    base_move: str | None = None
    challenger_move: str | None = None
    scores: dict[str, int] = Field(default_factory=dict)
    time_ms: int = 0


class RepertoireEntry(BaseModel):
    """Persistent position memory keyed by Polyglot hash."""

    polyglot_key: str
    family: FamilyName
    move_uci: str
    score_cp: int = 0
    visits: int = 1
    note: str = ""
    is_critical: bool = False

