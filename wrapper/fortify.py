"""Fortification step that refuses fragile wrapper overrides."""

from __future__ import annotations

from typing import TYPE_CHECKING

from wrapper.types import CandidateMove

if TYPE_CHECKING:
    import chess
    from wrapper.engine_pool import EnginePool


def should_accept_challenger(
    challenger: CandidateMove,
    base: CandidateMove,
    *,
    min_gain_cp: int = 8,
    max_regression_cp: int = 12,
) -> bool:
    """Keep a challenger only if it beats the base move by enough."""

    gain = challenger.combined_score_cp - base.combined_score_cp
    # Use rollout score for regression if engine score wasn't set (non-base candidates)
    challenger_eval = challenger.engine_score_cp if challenger.engine_score_cp != 0 else challenger.rollout_score_cp
    base_eval = base.engine_score_cp if base.engine_score_cp != 0 else base.rollout_score_cp
    regression = base_eval - challenger_eval
    return gain >= min_gain_cp and regression <= max_regression_cp


def fortify_with_duel(
    board: "chess.Board",
    challenger: CandidateMove,
    base: CandidateMove,
    pool: "EnginePool",
    *,
    min_gain_cp: int = 8,
    max_regression_cp: int = 12,
    nodes: int | None = None,
) -> bool:
    """Run a deeper verification duel between challenger and base move.

    Returns True if the challenger survives verification.
    """
    if not should_accept_challenger(
        challenger, base, min_gain_cp=min_gain_cp, max_regression_cp=max_regression_cp
    ):
        return False

    try:
        challenger_score, base_score = pool.verify_duel(
            board, challenger.move_uci, base.move_uci, nodes=nodes
        )
    except Exception:
        return False

    # Challenger must not be worse than base in the deeper search
    return challenger_score.score_cp >= base_score.score_cp - max_regression_cp

