#!/usr/bin/env python3
"""Mine a compact learned book for the Black killer line (Caro-Kann Classical).

Algorithm:
1. Start from the Caro-Kann exit position (White to move)
2. Predict top SF18 White moves
3. For each SF18 reply, generate Black candidate responses via master analysis
4. Run short playouts (mini-duels) to score Black empirically
5. Extend only promising branches
6. Build a compact, high-value Black policy tree

The objective is *Black-centric*: maximize Black's score vs SF18.
Unlike the White miner, we mine Black's replies to SF18's White moves.

Usage:
    python scripts/mine_black_killer.py [OPTIONS]
"""

from __future__ import annotations

import argparse
import re
import sys
import time
from pathlib import Path

import chess

# Add project root to path
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from wrapper.engine_pool import UciSubprocessEngine
from wrapper.policy_tree import PolicyTree, PolicyNode, MoveStats
from wrapper.targeted_book import TargetedBook

# Caro-Kann Classical 4...Bf5 seed moves (UCI)
CAROKANN_SEED_UCIS = [
    "e2e4", "c7c6", "d2d4", "d7d5", "b1c3", "d5e4",
    "c3e4", "c8f5", "e4g3", "f5g6", "h2h4", "h7h6",
    "g1f3", "b8d7", "h4h5", "g6h7", "f1d3", "h7d3",
    "d1d3", "e7e6",
]

CAROKANN_EXIT_FEN = "r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq -"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Mine Black killer line book")
    p.add_argument("--master", default=str(ROOT / "bin" / "stockfish-master"),
                    help="Path to master engine")
    p.add_argument("--sf18", default=str(ROOT / "bin" / "stockfish-sf18"),
                    help="Path to SF18 engine")
    p.add_argument("--book-path",
                    default=str(ROOT / "data" / "black_killer" / "book" / "carokann.json"),
                    help="Output book path")
    p.add_argument("--db-path",
                    default=str(ROOT / "data" / "black_killer" / "db" / "empirical.sqlite"),
                    help="Empirical DB path")
    p.add_argument("--max-candidates", type=int, default=5,
                    help="Max Black candidates per node (wider than White)")
    p.add_argument("--reply-bundle", type=int, default=3,
                    help="Top SF18 White replies to consider per node")
    p.add_argument("--max-depth", type=int, default=16,
                    help="Max plies past exit to explore")
    p.add_argument("--nodes-analysis", type=int, default=80000,
                    help="Nodes for candidate analysis (deeper for Black)")
    p.add_argument("--nodes-playout", type=int, default=30000,
                    help="Nodes for mini-duel playouts")
    p.add_argument("--playout-depth", type=int, default=8,
                    help="Plies of playout per branch")
    p.add_argument("--min-score-extend", type=float, default=45.0,
                    help="Min empirical Black score to extend (lower threshold for Black)")
    p.add_argument("--decisive-bonus", type=float, default=0.15,
                    help="Bonus weight for decisive results (higher for Black)")
    p.add_argument("--threads", type=int, default=1)
    p.add_argument("--hash", type=int, default=64)
    return p.parse_args()


def build_exit_board() -> chess.Board:
    """Construct the board at the Caro-Kann exit position."""
    board = chess.Board()
    for uci in CAROKANN_SEED_UCIS:
        board.push_uci(uci)
    return board


def get_multipv_moves(
    engine: UciSubprocessEngine,
    board: chess.Board,
    n_moves: int,
    nodes: int,
) -> list[tuple[str, int]]:
    """Get top candidate moves from an engine via MultiPV."""
    engine.command(f"setoption name MultiPV value {n_moves}")
    engine.command("isready", wait_for="readyok")
    engine._write(engine._position_cmd(board))
    lines = engine.command(f"go nodes {nodes}", wait_for="bestmove")
    engine.command("setoption name MultiPV value 1")
    engine.command("isready", wait_for="readyok")

    candidates: list[tuple[str, int]] = []
    seen_moves: set[str] = set()

    for line in lines:
        if not line.startswith("info") or "multipv" not in line:
            continue
        m_pv = re.search(r" pv (\S+)", line)
        m_cp = re.search(r"score cp (-?\d+)", line)
        m_mate = re.search(r"score mate (-?\d+)", line)
        if m_pv:
            move_uci = m_pv.group(1)
            if move_uci in seen_moves:
                continue
            seen_moves.add(move_uci)
            if m_cp:
                score = int(m_cp.group(1))
            elif m_mate:
                mate_in = int(m_mate.group(1))
                score = 30000 - abs(mate_in) if mate_in > 0 else -(30000 - abs(mate_in))
            else:
                score = 0
            candidates.append((move_uci, score))

    candidates.sort(key=lambda x: x[1], reverse=True)

    if not candidates:
        score_info = engine.analyse_root(board, nodes=nodes)
        if score_info.pv:
            candidates.append((score_info.pv[0], score_info.score_cp))

    return candidates[:n_moves]


def run_mini_duel_black(
    master: UciSubprocessEngine,
    sf18: UciSubprocessEngine,
    board: chess.Board,
    playout_depth: int,
    nodes: int,
) -> tuple[str, int]:
    """Run a short playout: SF18 plays White, master plays Black.

    Returns (result_from_blacks_perspective, final_eval_cp_for_black).
    """
    pos = board.copy()
    last_eval = 0

    for ply in range(playout_depth):
        if pos.is_game_over():
            result_str = pos.result()
            # Convert to Black's perspective
            if result_str == "0-1":
                return "1-0", last_eval  # Black wins
            elif result_str == "1-0":
                return "0-1", last_eval  # Black loses
            return "1/2-1/2", last_eval

        if pos.turn == chess.WHITE:
            # SF18 plays White (the opponent)
            reply_uci, score = sf18.predict_reply(pos, nodes=nodes)
            try:
                move = chess.Move.from_uci(reply_uci)
                if move in pos.legal_moves:
                    pos.push(move)
                    last_eval = -score.score_cp  # Negate: SF18's score from White POV
                    continue
            except ValueError:
                pass
            move = next(iter(pos.legal_moves))
            pos.push(move)
        else:
            # Master plays Black (our specialist side)
            score = master.analyse_root(pos, nodes=nodes)
            if score.pv:
                move_uci = score.pv[0]
                try:
                    move = chess.Move.from_uci(move_uci)
                    if move in pos.legal_moves:
                        pos.push(move)
                        last_eval = score.score_cp  # Already from Black's POV (mover)
                        continue
                except ValueError:
                    pass
            move = next(iter(pos.legal_moves))
            pos.push(move)

    # Final eval from Black's perspective
    final = master.analyse_root(pos, nodes=nodes)
    # If Black to move, score is from Black's POV; if White to move, negate
    if pos.turn == chess.BLACK:
        eval_cp = final.score_cp
    else:
        eval_cp = -final.score_cp

    if eval_cp > 200:
        return "1-0", eval_cp   # Black winning
    elif eval_cp < -200:
        return "0-1", eval_cp   # Black losing
    else:
        return "1/2-1/2", eval_cp


def mine_black_node(
    master: UciSubprocessEngine,
    sf18: UciSubprocessEngine,
    board: chess.Board,
    book: TargetedBook,
    args: argparse.Namespace,
    depth: int = 0,
) -> None:
    """Mine a single node for Black's moves.

    This is called when it's Black's turn. We generate Black candidate
    moves, predict SF18 White replies, and run playouts from Black's POV.
    """
    if depth > args.max_depth:
        return
    if board.is_game_over():
        return
    if board.turn != chess.BLACK:
        return  # Only mine Black's moves

    history = " ".join(m.uci() for m in board.move_stack)
    fen = board.fen()

    print(f"  [depth={depth}] Mining Black's move: {fen}")

    # Get master's Black candidate moves
    candidates = get_multipv_moves(master, board, args.max_candidates, args.nodes_analysis)
    print(f"  Black candidates: {[(m, s) for m, s in candidates]}")

    if not candidates:
        return

    node = PolicyNode(
        fen=fen,
        move_history=history,
        depth_from_exit=depth,
    )

    promising_branches: list[tuple[str, str, float]] = []

    for black_move, master_eval in candidates:
        print(f"    Black candidate: {black_move} (eval={master_eval}cp)")

        # After Black plays, predict SF18's White replies
        child_after_black = board.copy()
        try:
            child_after_black.push_uci(black_move)
        except (ValueError, chess.IllegalMoveError):
            continue

        if child_after_black.is_game_over():
            result_str = child_after_black.result()
            stats = MoveStats(master_eval_cp=master_eval, predicted_replies=[])
            if result_str == "0-1":
                stats.wins += 1  # Black wins
                stats.empirical_score = 100.0
                stats.decisive_rate = 1.0
            elif result_str == "1-0":
                stats.losses += 1
                stats.empirical_score = 0.0
                stats.decisive_rate = 1.0
            else:
                stats.draws += 1
                stats.empirical_score = 50.0
            stats.visits = 1
            stats.last_verified_ts = time.time()
            node.candidate_moves[black_move] = stats
            continue

        # Get SF18 White replies
        sf18_replies = get_multipv_moves(sf18, child_after_black, args.reply_bundle, args.nodes_analysis)
        print(f"    SF18 White replies: {[(r, s) for r, s in sf18_replies]}")

        stats = MoveStats(
            master_eval_cp=master_eval,
            predicted_replies=[r for r, _ in sf18_replies],
        )

        total_score = 0.0
        total_games = 0

        for white_reply, reply_eval in sf18_replies:
            child = child_after_black.copy()
            try:
                child.push_uci(white_reply)
            except (ValueError, chess.IllegalMoveError):
                continue

            if child.is_game_over():
                result_str = child.result()
                if result_str == "0-1":
                    stats.wins += 1
                    total_score += 1.0
                elif result_str == "1-0":
                    stats.losses += 1
                else:
                    stats.draws += 1
                    total_score += 0.5
                total_games += 1
                continue

            # Run mini-duel from Black's perspective
            result, eval_cp = run_mini_duel_black(
                master, sf18, child, args.playout_depth, args.nodes_playout,
            )

            if result == "1-0":  # Black wins
                stats.wins += 1
                total_score += 1.0
            elif result == "0-1":  # Black loses
                stats.losses += 1
            else:
                stats.draws += 1
                total_score += 0.5
            total_games += 1

            # Record to empirical DB
            book.empirical_db.record_result(
                move_history=history,
                move_uci=black_move,
                fen=fen,
                result=result,
                eval_cp=eval_cp,
                reply_uci=white_reply,
            )

        if total_games > 0:
            stats.empirical_score = total_score / total_games * 100
            stats.decisive_rate = (stats.wins + stats.losses) / total_games
            stats.visits = total_games

            # WDL score from eval — master_eval is already from mover's (Black's) POV
            black_eval = master_eval
            if black_eval > 200:
                stats.wdl_score = 90.0
            elif black_eval > 100:
                stats.wdl_score = 75.0
            elif black_eval > 50:
                stats.wdl_score = 60.0
            elif black_eval > -20:
                stats.wdl_score = 55.0
            elif black_eval > -50:
                stats.wdl_score = 45.0
            else:
                stats.wdl_score = 30.0

        stats.last_verified_ts = time.time()
        node.candidate_moves[black_move] = stats

        emp_score = stats.empirical_score
        print(f"    Result: {stats.wins}W-{stats.draws}D-{stats.losses}L = {emp_score:.1f}%")

        # Track promising branches for extension
        if emp_score >= args.min_score_extend:
            for reply_uci, _ in sf18_replies:
                promising_branches.append((black_move, reply_uci, emp_score))

    book.tree.add_node(node)
    book.save()

    # Extend promising branches: after Black's move and SF18's White reply,
    # look for next Black move opportunities
    for black_move, white_reply, score in promising_branches:
        child = board.copy()
        try:
            child.push_uci(black_move)
            if child.is_game_over():
                continue
            child.push_uci(white_reply)
            if child.is_game_over():
                continue
        except (ValueError, chess.IllegalMoveError):
            continue

        # After Black + White, it's Black's turn again
        if child.turn == chess.BLACK:
            mine_black_node(master, sf18, child, book, args, depth + 2)


def main() -> None:
    args = parse_args()

    print("=== Black Killer Line Book Mining ===")
    print(f"Selected line: Caro-Kann Classical 4...Bf5 (B09)")
    print(f"Exit FEN: {CAROKANN_EXIT_FEN}")
    print(f"Max candidates: {args.max_candidates}")
    print(f"Reply bundle: {args.reply_bundle}")
    print(f"Max depth: {args.max_depth}")
    print(f"Nodes analysis: {args.nodes_analysis}")
    print(f"Nodes playout: {args.nodes_playout}")
    print(f"Decisive bonus: {args.decisive_bonus}")
    print()

    # Start engines
    print("Starting master engine...")
    master = UciSubprocessEngine(Path(args.master), threads=args.threads, hash_mb=args.hash)
    master.start()

    print("Starting SF18 engine...")
    sf18 = UciSubprocessEngine(Path(args.sf18), threads=args.threads, hash_mb=args.hash)
    sf18.start()

    # Initialize targeted book for Black
    book = TargetedBook(
        book_path=Path(args.book_path),
        db_path=Path(args.db_path),
        empirical_weight=0.6,
        wdl_weight=0.25,
        decisive_bonus=args.decisive_bonus,
        book_depth_limit=40,
    )
    # Override the tree's exit info for Caro-Kann
    book.tree.exit_fen = CAROKANN_EXIT_FEN
    book.tree.exit_moves = " ".join(CAROKANN_SEED_UCIS)
    book.tree.metadata["line_name"] = "Caro-Kann Classical 4...Bf5"
    book.tree.metadata["line_id"] = "B09"
    book.tree.metadata["color"] = "black"
    book.tree.metadata["mined_at"] = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())

    # Build exit board
    board = build_exit_board()
    print(f"\nExit position: {board.fen()}")
    print(f"Side to move: {'White' if board.turn == chess.WHITE else 'Black'}")

    # The exit position has White to move after 10...e6.
    # We need to predict SF18's White move first, then mine Black's responses.
    print("\nPredicting SF18's White moves at exit position...")
    sf18_white_moves = get_multipv_moves(sf18, board, args.reply_bundle, args.nodes_analysis)
    print(f"SF18 White candidates: {sf18_white_moves}")

    # Also get master's prediction of White's move
    master_white_moves = get_multipv_moves(master, board, 2, args.nodes_analysis)
    print(f"Master White candidates: {master_white_moves}")

    # Merge unique White moves to explore
    all_white_moves: list[str] = []
    seen: set[str] = set()
    for m, _ in sf18_white_moves + master_white_moves:
        if m not in seen:
            all_white_moves.append(m)
            seen.add(m)

    print(f"White moves to explore: {all_white_moves}")

    # For each White move, mine Black's responses
    for wm in all_white_moves:
        child = board.copy()
        try:
            child.push_uci(wm)
        except (ValueError, chess.IllegalMoveError):
            print(f"Illegal White move {wm}, skipping")
            continue

        if child.is_game_over():
            print(f"Game over after {wm}: {child.result()}")
            continue

        if child.turn == chess.BLACK:
            print(f"\n--- Mining Black's responses after SF18 plays {wm} ---")
            mine_black_node(master, sf18, child, book, args, depth=1)

    # Save final book
    book.save()
    print(f"\n=== Mining complete ===")
    print(f"Book saved to: {args.book_path}")
    print(book.summary())

    # Cleanup
    if master.process:
        master.process.terminate()
    if sf18.process:
        sf18.process.terminate()
    book.close()


if __name__ == "__main__":
    main()
