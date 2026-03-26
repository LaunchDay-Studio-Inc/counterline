#!/usr/bin/env python3
"""Populate the CounterLine repertoire database with deep engine analysis.

Builds a shallow tree from each exit FEN:
  Level 0: exit position  – analyse top N moves at high depth
  Level 1: after our move + opponent reply – analyse top N/2 moves
  Level 2: one more ply of our responses

Stores nodes + move_priors in the SQLite DB so the wrapper has
empirical scores to choose from during play.
"""

from __future__ import annotations

import sys
import time
from pathlib import Path

import chess
import typer

from wrapper.engine_pool import UciSubprocessEngine
from wrapper.opening_lock import BLACK_EXIT_FEN, WHITE_EXIT_FEN
from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB

cli = typer.Typer(add_completion=False)

FAMILIES = {
    WHITE_EXIT_FEN: "qgd_exchange_carlsbad",
    BLACK_EXIT_FEN: "petroff_mainline",
}


def analyse_position(
    engine: UciSubprocessEngine,
    board: chess.Board,
    *,
    movetime_ms: int = 1000,
    top_n: int = 5,
) -> list[tuple[str, int]]:
    """Analyse a position and return top_n moves with their scores (from side-to-move POV)."""
    # Use MultiPV to get top moves at once
    engine.command(f"setoption name MultiPV value {top_n}")
    engine.command("isready", wait_for="readyok")
    engine.command("ucinewgame")
    engine.command("isready", wait_for="readyok")
    engine._write(engine._position_cmd(board))
    lines = engine.command(f"go movetime {movetime_ms}", wait_for="bestmove")

    # Parse MultiPV results
    import re
    pv_scores: dict[int, tuple[str, int]] = {}
    for line in lines:
        if not line.startswith("info") or "score" not in line:
            continue
        m_pv = re.search(r"multipv (\d+)", line)
        pv_idx = int(m_pv.group(1)) if m_pv else 1

        m_cp = re.search(r"score cp (-?\d+)", line)
        m_mate = re.search(r"score mate (-?\d+)", line)
        if m_cp:
            score = int(m_cp.group(1))
        elif m_mate:
            mate_in = int(m_mate.group(1))
            score = 30000 - abs(mate_in) if mate_in > 0 else -(30000 - abs(mate_in))
        else:
            continue

        m_moves = re.search(r" pv (.+)", line)
        if m_moves:
            first_move = m_moves.group(1).split()[0]
            pv_scores[pv_idx] = (first_move, score)

    # Reset MultiPV
    engine.command("setoption name MultiPV value 1")
    engine.command("isready", wait_for="readyok")

    # Return sorted by PV index
    results = []
    for idx in sorted(pv_scores.keys()):
        move_uci, score_cp = pv_scores[idx]
        results.append((move_uci, score_cp))
    return results[:top_n]


def populate_tree(
    engine: UciSubprocessEngine,
    db: RepertoireDB,
    repo: Repertoire,
    board: chess.Board,
    family: str,
    *,
    our_color: chess.Color,
    l0_movetime: int = 2000,
    l1_movetime: int = 1000,
    l2_movetime: int = 500,
    top_n: int = 5,
) -> int:
    """Build repertoire tree from exit position. Returns number of nodes stored."""
    count = 0

    # Level 0: exit position (critical node)
    db.upsert_node(board, family, is_critical=True, structure_tags=family)
    typer.echo(f"  L0: {board.fen()} ({family})")
    l0_moves = analyse_position(engine, board, movetime_ms=l0_movetime, top_n=top_n)
    best_cp = l0_moves[0][1] if l0_moves else 0

    for move_uci, score_cp in l0_moves:
        # Store score relative to best move (differential prior)
        prior = max(0, score_cp - best_cp + 5)  # bonus of 5 for being analyzed
        repo.remember(board, family, move_uci, prior, note=f"deep_L0_{score_cp}cp")
        typer.echo(f"    {move_uci}: {score_cp}cp (prior={prior})")
        count += 1

    # Level 1: after our move + opponent reply
    our_moves = l0_moves[:3]  # top 3 of our moves
    for our_uci, our_cp in our_moves:
        our_move = chess.Move.from_uci(our_uci)
        if our_move not in board.legal_moves:
            continue

        child = board.copy()
        child.push(our_move)

        # Get opponent's likely reply
        engine.command("ucinewgame")
        engine.command("isready", wait_for="readyok")
        reply_uci, reply_score = engine.predict_reply(child, movetime_ms=l1_movetime)

        if not reply_uci or reply_uci == "(none)":
            continue

        reply_move = chess.Move.from_uci(reply_uci)
        if reply_move not in child.legal_moves:
            continue

        grandchild = child.copy()
        grandchild.push(reply_move)

        # Now it's our turn again at Level 1
        db.upsert_node(grandchild, family, is_critical=False, structure_tags=family)
        l1_moves = analyse_position(engine, grandchild, movetime_ms=l1_movetime, top_n=top_n // 2 + 1)
        l1_best = l1_moves[0][1] if l1_moves else 0

        typer.echo(f"  L1: after {our_uci} {reply_uci}")
        for m_uci, m_cp in l1_moves:
            prior = max(0, m_cp - l1_best + 3)
            repo.remember(grandchild, family, m_uci, prior, note=f"deep_L1_{m_cp}cp")
            typer.echo(f"    {m_uci}: {m_cp}cp (prior={prior})")
            count += 1

        # Level 2: one more exchange
        for resp_uci, resp_cp in l1_moves[:2]:
            resp_move = chess.Move.from_uci(resp_uci)
            if resp_move not in grandchild.legal_moves:
                continue

            ggchild = grandchild.copy()
            ggchild.push(resp_move)

            # Opponent reply
            engine.command("ucinewgame")
            engine.command("isready", wait_for="readyok")
            r2_uci, r2_score = engine.predict_reply(ggchild, movetime_ms=l2_movetime)
            if not r2_uci or r2_uci == "(none)":
                continue
            r2_move = chess.Move.from_uci(r2_uci)
            if r2_move not in ggchild.legal_moves:
                continue

            gggchild = ggchild.copy()
            gggchild.push(r2_move)

            db.upsert_node(gggchild, family, is_critical=False, structure_tags=family)
            l2_moves = analyse_position(engine, gggchild, movetime_ms=l2_movetime, top_n=3)
            l2_best = l2_moves[0][1] if l2_moves else 0

            typer.echo(f"  L2: after {our_uci} {reply_uci} {resp_uci} {r2_uci}")
            for m2_uci, m2_cp in l2_moves:
                prior = max(0, m2_cp - l2_best + 2)
                repo.remember(gggchild, family, m2_uci, prior, note=f"deep_L2_{m2_cp}cp")
                typer.echo(f"    {m2_uci}: {m2_cp}cp (prior={prior})")
                count += 1

    return count


def seed_entries(db: RepertoireDB, repo: Repertoire) -> None:
    """Seed basic hardcoded priors (backward compat)."""
    white = chess.Board(WHITE_EXIT_FEN)
    black = chess.Board(BLACK_EXIT_FEN)

    db.upsert_node(white, "qgd_exchange_carlsbad", is_critical=True, structure_tags="carlsbad,minority_attack")
    db.upsert_node(black, "petroff_mainline", is_critical=True, structure_tags="petroff,symmetric_center")

    repo.remember(white, "qgd_exchange_carlsbad", "f2f3", 18, note="e4_break_prep")
    repo.remember(white, "qgd_exchange_carlsbad", "a2a3", 12, note="minority_attack_prep")
    repo.remember(white, "qgd_exchange_carlsbad", "a1c1", 10, note="c_file_pressure")
    repo.remember(white, "qgd_exchange_carlsbad", "h2h3", 6, note="luft")

    repo.remember(black, "petroff_mainline", "c8e6", 18, note="bishop_development")
    repo.remember(black, "petroff_mainline", "d5c4", 12, note="c_pawn_exchange")
    repo.remember(black, "petroff_mainline", "b8d7", 10, note="knight_development")
    repo.remember(black, "petroff_mainline", "f8e8", 8, note="e_file_pressure")


def dump_db(db: RepertoireDB) -> None:
    data = db.dump_all()
    for table_name, rows in data.items():
        typer.echo(f"\n=== {table_name} ({len(rows)} rows) ===")
        for row in rows:
            typer.echo(f"  {row}")


@cli.command()
def main(
    db_path: Path = typer.Option(Path("data/repertoire/counterline.sqlite"), "--db-path"),
    engine_path: Path = typer.Option(Path("bin/stockfish-master"), "--engine"),
    seed_defaults: bool = typer.Option(True, "--seed-defaults/--no-seed-defaults"),
    deep_analyse: bool = typer.Option(True, "--deep/--no-deep"),
    l0_movetime: int = typer.Option(2000, "--l0-ms"),
    l1_movetime: int = typer.Option(1000, "--l1-ms"),
    l2_movetime: int = typer.Option(500, "--l2-ms"),
    top_n: int = typer.Option(5, "--top-n"),
    dump: bool = typer.Option(False, "--dump"),
) -> None:
    db = RepertoireDB(db_path)
    db.initialize()
    repo = Repertoire(db)

    if deep_analyse:
        if not engine_path.exists():
            typer.echo(f"engine not found: {engine_path}", err=True)
            raise typer.Exit(1)

        engine = UciSubprocessEngine(engine_path, threads=1, hash_mb=128)
        engine.start()
        typer.echo(f"engine started: {engine_path}")

        total = 0
        t0 = time.monotonic()
        for fen, family in FAMILIES.items():
            board = chess.Board(fen)
            our_color = board.turn
            n = populate_tree(
                engine, db, repo, board, family,
                our_color=our_color,
                l0_movetime=l0_movetime,
                l1_movetime=l1_movetime,
                l2_movetime=l2_movetime,
                top_n=top_n,
            )
            total += n
            typer.echo(f"  → {n} entries for {family}")

        elapsed = time.monotonic() - t0
        typer.echo(f"\npopulated {total} entries in {elapsed:.1f}s")
        engine.quit()

    # Seed entries run AFTER deep analysis so they override deep scores
    if seed_defaults:
        seed_entries(db, repo)
        typer.echo(f"seeded basic priors at {db_path}")

    if dump:
        dump_db(db)

    if not seed_defaults and not deep_analyse and not dump:
        typer.echo(f"initialized repertoire db at {db_path}")


if __name__ == "__main__":
    cli()

