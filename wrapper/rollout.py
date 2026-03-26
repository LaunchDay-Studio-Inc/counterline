"""Selective candidate generation and probing with rollout variants."""

from __future__ import annotations

from typing import TYPE_CHECKING

import chess

from wrapper.plan_score import score_move
from wrapper.types import CandidateMove

if TYPE_CHECKING:
    from wrapper.engine_pool import EnginePool


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


def half_step_rollout(
    board: chess.Board,
    candidates: list[CandidateMove],
    pool: EnginePool,
    *,
    nodes: int | None = None,
) -> list[CandidateMove]:
    """For each candidate move m, evaluate the resulting position from the opponent's POV."""

    results: list[CandidateMove] = []
    for cand in candidates:
        move = chess.Move.from_uci(cand.move_uci)
        if move not in board.legal_moves:
            results.append(cand)
            continue
        child = board.copy()
        child.push(move)
        score = pool.analyse_root(child, nodes=nodes)
        # Negate because it's from opponent's perspective
        rollout_cp = -score.score_cp
        results.append(
            cand.model_copy(
                update={
                    "rollout_score_cp": rollout_cp,
                    "combined_score_cp": cand.plan_score_cp + rollout_cp + cand.empirical_prior_cp - cand.risk_penalty_cp,
                }
            )
        )
    results.sort(key=lambda c: c.combined_score_cp, reverse=True)
    return results


def one_step_rollout(
    board: chess.Board,
    candidates: list[CandidateMove],
    pool: EnginePool,
    *,
    nodes: int | None = None,
) -> list[CandidateMove]:
    """For each candidate m, ask nominal opponent for reply r, then evaluate after m,r."""

    results: list[CandidateMove] = []
    for cand in candidates:
        move = chess.Move.from_uci(cand.move_uci)
        if move not in board.legal_moves:
            results.append(cand)
            continue
        child = board.copy()
        child.push(move)
        # Ask nominal opponent for its best reply
        reply_uci, _ = pool.predict_reply(child, nodes=nodes)
        reply_move = chess.Move.from_uci(reply_uci)
        if reply_move in child.legal_moves:
            child.push(reply_move)
        # Evaluate the position after m, r from our perspective
        score = pool.analyse_root(child, nodes=nodes)
        rollout_cp = score.score_cp
        results.append(
            cand.model_copy(
                update={
                    "rollout_score_cp": rollout_cp,
                    "combined_score_cp": cand.plan_score_cp + rollout_cp + cand.empirical_prior_cp - cand.risk_penalty_cp,
                }
            )
        )
    results.sort(key=lambda c: c.combined_score_cp, reverse=True)
    return results


def reply_bundle_rollout(
    board: chess.Board,
    candidates: list[CandidateMove],
    pool: EnginePool,
    *,
    nodes: int | None = None,
    top_replies: int = 2,
) -> list[CandidateMove]:
    """On unstable nodes, consider top N opponent replies and aggregate conservatively."""

    results: list[CandidateMove] = []
    for cand in candidates:
        move = chess.Move.from_uci(cand.move_uci)
        if move not in board.legal_moves:
            results.append(cand)
            continue
        child = board.copy()
        child.push(move)
        # Get top opponent replies via searchmoves on top legal moves
        opp_moves = [m.uci() for m in list(child.legal_moves)[:top_replies * 2]]
        if not opp_moves:
            results.append(cand)
            continue
        opp_scores = pool.analyse_searchmoves(child, opp_moves[:top_replies], nodes=nodes)
        # For each opponent reply, evaluate the resulting position
        eval_scores: list[int] = []
        for reply_uci, _opp_score in sorted(
            opp_scores.items(), key=lambda x: x[1].score_cp, reverse=True
        )[:top_replies]:
            reply_move = chess.Move.from_uci(reply_uci)
            grandchild = child.copy()
            if reply_move in grandchild.legal_moves:
                grandchild.push(reply_move)
            gs = pool.analyse_root(grandchild, nodes=nodes)
            eval_scores.append(gs.score_cp)
        # Conservative aggregation: use the worst case (soft-min)
        if eval_scores:
            rollout_cp = min(eval_scores)
        else:
            rollout_cp = 0
        results.append(
            cand.model_copy(
                update={
                    "rollout_score_cp": rollout_cp,
                    "combined_score_cp": cand.plan_score_cp + rollout_cp + cand.empirical_prior_cp - cand.risk_penalty_cp,
                }
            )
        )
    results.sort(key=lambda c: c.combined_score_cp, reverse=True)
    return results


def compute_instability(candidates: list[CandidateMove]) -> float:
    """Return instability score: high if top candidates are close in combined score."""

    if len(candidates) < 2:
        return 0.0
    top = candidates[0].combined_score_cp
    second = candidates[1].combined_score_cp
    gap = abs(top - second)
    if gap >= 30:
        return 0.0
    return max(0.0, 1.0 - gap / 30.0)

