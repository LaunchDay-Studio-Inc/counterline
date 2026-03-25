"""Fortification step that refuses fragile wrapper overrides."""

from __future__ import annotations

from wrapper.types import CandidateMove


def should_accept_challenger(
    challenger: CandidateMove,
    base: CandidateMove,
    *,
    min_gain_cp: int = 8,
    max_regression_cp: int = 12,
) -> bool:
    """Keep a challenger only if it beats the base move by enough."""

    gain = challenger.combined_score_cp - base.combined_score_cp
    regression = base.engine_score_cp - challenger.engine_score_cp
    return gain >= min_gain_cp and regression <= max_regression_cp

