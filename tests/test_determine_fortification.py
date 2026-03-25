from __future__ import annotations

from wrapper.determine import determine_bestmove
from wrapper.fortify import should_accept_challenger
from wrapper.types import CandidateMove


def test_should_accept_challenger_when_gain_is_clear() -> None:
    challenger = CandidateMove(move_uci="f2f3", engine_score_cp=2, combined_score_cp=20)
    base = CandidateMove(move_uci="a2a3", engine_score_cp=0, combined_score_cp=0)
    assert should_accept_challenger(challenger, base, min_gain_cp=8, max_regression_cp=12) is True


def test_should_reject_challenger_on_regression() -> None:
    challenger = CandidateMove(move_uci="f2f3", engine_score_cp=-20, combined_score_cp=10)
    base = CandidateMove(move_uci="a2a3", engine_score_cp=0, combined_score_cp=5)
    assert should_accept_challenger(challenger, base, min_gain_cp=4, max_regression_cp=12) is False


def test_determine_bestmove_prefers_wrapper_override() -> None:
    base = CandidateMove(move_uci="a2a3", engine_score_cp=0, combined_score_cp=0)
    challenger = CandidateMove(move_uci="f2f3", engine_score_cp=0, combined_score_cp=20)
    decision = determine_bestmove(family="qgd_exchange_carlsbad", base=base, challenger=challenger)
    assert decision.bestmove == "f2f3"
    assert decision.used_wrapper is True

