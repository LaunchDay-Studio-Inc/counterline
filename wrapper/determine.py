"""Final move arbitration between base and wrapper candidates."""

from __future__ import annotations

import time
from typing import TYPE_CHECKING

import chess

from wrapper.fortify import fortify_with_duel, should_accept_challenger
from wrapper.opening_lock import detect_opening_family, get_seed_move, is_book_complete, lock_color_for_family
from wrapper.plan_score import score_move
from wrapper.rollout import (
    compute_instability,
    generate_candidates,
    half_step_rollout,
    one_step_rollout,
    reply_bundle_rollout,
)
from wrapper.types import CandidateMove, MoveDecision

if TYPE_CHECKING:
    from wrapper.engine_pool import EnginePool
    from wrapper.repertoire import Repertoire
    from wrapper.repertoire_db import RepertoireDB


def determine_bestmove(
    *,
    family: str,
    base: CandidateMove,
    challenger: CandidateMove | None,
    min_gain_cp: int = 8,
    max_regression_cp: int = 12,
) -> MoveDecision:
    """Simple decision without engine pool (for tests and fallback)."""

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


def determine_countermove(
    board: chess.Board,
    pool: "EnginePool",
    repertoire: "Repertoire",
    repertoire_db: "RepertoireDB",
    *,
    max_candidates: int = 3,
    min_gain_cp: int = 5,
    max_regression_cp: int = 8,
    use_duel: bool = True,
    probe_nodes: int = 20000,
) -> MoveDecision:
    """Master decision function implementing the full wrapper logic.

    Uses node-limited searches (probe_nodes) to stay within time budget.
    """

    t0 = time.monotonic()

    # 1. Detect family
    family = detect_opening_family(board)
    side_name = "white" if board.turn == chess.WHITE else "black"

    # 2. Check for seed move (opening lock)
    seed = get_seed_move(board)
    if seed is not None:
        elapsed = int((time.monotonic() - t0) * 1000)
        return MoveDecision(
            bestmove=seed,
            family=family,
            reason="seed_line",
            used_wrapper=True,
            base_move=seed,
            time_ms=elapsed,
        )

    # 3. Get base move and base eval from evaluator engine
    try:
        base_move, base_ponder, _info = pool.bestmove(board)
        base_score = pool.analyse_root(board, nodes=probe_nodes)
        base_cp = base_score.score_cp
    except Exception:
        fallback = next(iter(board.legal_moves), None)
        elapsed = int((time.monotonic() - t0) * 1000)
        return MoveDecision(
            bestmove=fallback.uci() if fallback else "0000",
            family=family,
            reason="engine_error",
            used_wrapper=False,
            time_ms=elapsed,
        )

    base_candidate = CandidateMove(
        move_uci=base_move,
        engine_score_cp=base_cp,
        combined_score_cp=base_cp,
        source="base",
        pv=base_score.pv,
    )

    # 4. If not in family or not our turn: return base move
    lock_color = lock_color_for_family(family)
    if family == "unknown" or lock_color != side_name:
        elapsed = int((time.monotonic() - t0) * 1000)
        return MoveDecision(
            bestmove=base_move,
            ponder=base_ponder,
            family=family,
            reason="outside_family",
            used_wrapper=False,
            base_move=base_move,
            scores={"base_cp": base_cp},
            time_ms=elapsed,
        )

    # 5. Check if book is complete
    if not is_book_complete(board):
        elapsed = int((time.monotonic() - t0) * 1000)
        return MoveDecision(
            bestmove=base_move,
            ponder=base_ponder,
            family=family,
            reason="book_incomplete",
            used_wrapper=False,
            base_move=base_move,
            time_ms=elapsed,
        )

    # 6. Gather candidate set
    candidate_set: list[CandidateMove] = [base_candidate]

    # Repertoire DB priors
    rep_moves = repertoire.candidate_moves(board, family)
    for rm in rep_moves:
        if rm.move_uci != base_move and len(candidate_set) < max_candidates:
            candidate_set.append(
                rm.model_copy(update={"empirical_prior_cp": rm.combined_score_cp, "source": "repertoire"})
            )

    # Family move priors from plan scoring
    plan_candidates = generate_candidates(board, family, limit=max_candidates)
    for pc in plan_candidates:
        if pc.move_uci not in {c.move_uci for c in candidate_set} and len(candidate_set) < max_candidates:
            candidate_set.append(pc)

    # 7. Compute instability score
    instability = compute_instability(candidate_set)
    is_critical = repertoire_db.is_critical(board)

    # 8. If stable and not critical: return base move
    if instability < 0.3 and not is_critical and len(candidate_set) <= 1:
        elapsed = int((time.monotonic() - t0) * 1000)
        return MoveDecision(
            bestmove=base_move,
            ponder=base_ponder,
            family=family,
            reason="stable_base",
            used_wrapper=False,
            base_move=base_move,
            scores={"base_cp": base_cp, "instability": int(instability * 100)},
            time_ms=elapsed,
        )

    # 9. Run half-step rollout on candidate set
    rollout_results = half_step_rollout(board, candidate_set, pool, nodes=probe_nodes)

    # 10. If highly unstable, run reply_bundle_rollout on top challengers
    if instability > 0.7 or is_critical:
        top_challengers = [c for c in rollout_results if c.move_uci != base_move][:2]
        if top_challengers:
            rollout_results_bundle = reply_bundle_rollout(board, top_challengers, pool, nodes=probe_nodes)
            # Merge bundle results back
            bundle_map = {c.move_uci: c for c in rollout_results_bundle}
            rollout_results = [
                bundle_map.get(c.move_uci, c) for c in rollout_results
            ]

    # 11. Find best challenger (not the base move)
    challengers = [c for c in rollout_results if c.move_uci != base_move]
    challengers.sort(key=lambda c: c.combined_score_cp, reverse=True)

    if not challengers:
        elapsed = int((time.monotonic() - t0) * 1000)
        return MoveDecision(
            bestmove=base_move,
            ponder=base_ponder,
            family=family,
            reason="no_challenger_found",
            used_wrapper=False,
            base_move=base_move,
            scores={"base_cp": base_cp},
            time_ms=elapsed,
        )

    best_challenger = challengers[0]

    # Add plan score to challenger combined
    plan_cp = score_move(board, family, chess.Move.from_uci(best_challenger.move_uci))
    best_challenger = best_challenger.model_copy(
        update={
            "plan_score_cp": plan_cp,
            "combined_score_cp": best_challenger.rollout_score_cp + best_challenger.empirical_prior_cp + plan_cp - best_challenger.risk_penalty_cp,
        }
    )

    # 12. Run fortification / verification
    accepted = False
    if use_duel:
        try:
            accepted = fortify_with_duel(
                board,
                best_challenger,
                base_candidate,
                pool,
                min_gain_cp=min_gain_cp,
                max_regression_cp=max_regression_cp,
                nodes=probe_nodes,
            )
        except Exception:
            accepted = should_accept_challenger(
                best_challenger,
                base_candidate,
                min_gain_cp=min_gain_cp,
                max_regression_cp=max_regression_cp,
            )
    else:
        accepted = should_accept_challenger(
            best_challenger,
            base_candidate,
            min_gain_cp=min_gain_cp,
            max_regression_cp=max_regression_cp,
        )

    elapsed = int((time.monotonic() - t0) * 1000)

    if accepted:
        return MoveDecision(
            bestmove=best_challenger.move_uci,
            family=family,
            reason="wrapper_override",
            used_wrapper=True,
            base_move=base_move,
            challenger_move=best_challenger.move_uci,
            scores={
                "base_cp": base_cp,
                "challenger_combined": best_challenger.combined_score_cp,
                "challenger_rollout": best_challenger.rollout_score_cp,
                "challenger_plan": best_challenger.plan_score_cp,
                "instability": int(instability * 100),
            },
            time_ms=elapsed,
        )

    return MoveDecision(
        bestmove=base_move,
        ponder=base_ponder,
        family=family,
        reason="fortify_rejected",
        used_wrapper=False,
        base_move=base_move,
        challenger_move=best_challenger.move_uci,
        scores={
            "base_cp": base_cp,
            "challenger_combined": best_challenger.combined_score_cp,
            "instability": int(instability * 100),
        },
        time_ms=elapsed,
    )

