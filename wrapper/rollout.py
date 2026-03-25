"""Selective candidate generation and probing."""

from __future__ import annotations

import chess

from wrapper.plan_score import score_move
from wrapper.types import CandidateMove


def generate_candidates(board: chess.Board, family: str, limit: int = 4) -> list[CandidateMove]:
    """Generate a small set of legal candidates ranked by structure score."""

    candidates: list[CandidateMove] = []
    for move in board.legal_moves:
        plan_score = score_move(board, family, move)
        candidates.append(
            CandidateMove(
                move_uci=move.uci(),
                plan_score_cp=plan_score,
                combined_score_cp=plan_score,
                source="rollout",
            )
        )
    candidates.sort(key=lambda item: (item.combined_score_cp, item.move_uci), reverse=True)
    return candidates[:limit]


def annotate_with_engine_scores(
    candidates: list[CandidateMove],
    engine_lines: dict[str, int],
) -> list[CandidateMove]:
    """Merge precomputed engine scores into candidate data."""

    merged: list[CandidateMove] = []
    for candidate in candidates:
        engine_score = engine_lines.get(candidate.move_uci, 0)
        merged.append(
            candidate.model_copy(
                update={
                    "engine_score_cp": engine_score,
                    "combined_score_cp": candidate.plan_score_cp + engine_score,
                }
            )
        )
    merged.sort(key=lambda item: (item.combined_score_cp, item.move_uci), reverse=True)
    return merged

