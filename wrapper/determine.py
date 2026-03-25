"""Final move arbitration between base and wrapper candidates."""

from __future__ import annotations

from wrapper.fortify import should_accept_challenger
from wrapper.types import CandidateMove, MoveDecision


def determine_bestmove(
    *,
    family: str,
    base: CandidateMove,
    challenger: CandidateMove | None,
    min_gain_cp: int = 8,
    max_regression_cp: int = 12,
) -> MoveDecision:
    """Choose the move that should be exposed back to the GUI."""

    if challenger is None:
        return MoveDecision(
            bestmove=base.move_uci,
            family=family,
            reason="no_challenger",
            used_wrapper=False,
            base_move=base.move_uci,
        )
    if should_accept_challenger(
        challenger,
        base,
        min_gain_cp=min_gain_cp,
        max_regression_cp=max_regression_cp,
    ):
        return MoveDecision(
            bestmove=challenger.move_uci,
            family=family,
            reason="wrapper_override",
            used_wrapper=True,
            base_move=base.move_uci,
            challenger_move=challenger.move_uci,
        )
    return MoveDecision(
        bestmove=base.move_uci,
        family=family,
        reason="fortify_rejected",
        used_wrapper=False,
        base_move=base.move_uci,
        challenger_move=challenger.move_uci,
    )

