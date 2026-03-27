#!/usr/bin/env python3
"""Mine a compact learned book for the White killer line (Vienna Gambit).

Algorithm:
1. Start from the Vienna exit position
2. Generate White candidate moves via master analysis
3. For each candidate, predict top SF18 replies
4. Run short playouts (mini-duels) to score empirically
5. Extend only promising branches
6. Build a compact, high-value policy tree

Usage:
    python scripts/mine_white_killer.py [OPTIONS]
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

import chess

# Add project root to path
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from wrapper.engine_pool import UciSubprocessEngine
from wrapper.policy_tree import PolicyTree, PolicyNode, MoveStats
from wrapper.targeted_book import TargetedBook, VIENNA_SEED_UCIS, VIENNA_EXIT_FEN


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Mine White killer line book")
    p.add_argument("--master", default=str(ROOT / "bin" / "stockfish-master"),
                    help="Path to master engine")
    p.add_argument("--sf18", default=str(ROOT / "bin" / "stockfish-sf18"),
                    help="Path to SF18 engine")
    p.add_argument("--book-path", default=str(ROOT / "data" / "white_killer" / "book" / "vienna.json"),
                    help="Output book path")
    p.add_argument("--db-path", default=str(ROOT / "data" / "white_killer" / "db" / "empirical.sqlite"),
                    help="Empirical DB path")
    p.add_argument("--max-candidates", type=int, default=4,
                    help="Max White candidates per node")
    p.add_argument("--reply-bundle", type=int, default=2,
                    help="Top SF18 replies to consider per candidate")
    p.add_argument("--max-depth", type=int, default=12,
                    help="Max plies past exit to explore")
    p.add_argument("--nodes-analysis", type=int, default=50000,
                    help="Nodes for candidate analysis")
    p.add_argument("--nodes-playout", type=int, default=20000,
                    help="Nodes for mini-duel playouts")
    p.add_argument("--playout-depth", type=int, default=6,
                    help="Plies of playout per branch")
    p.add_argument("--min-score-extend", type=float, default=60.0,
                    help="Min empirical score to extend a branch")
    p.add_argument("--threads", type=int, default=1)
    p.add_argument("--hash", type=int, default=64)
    return p.parse_args()


def build_exit_board() -> chess.Board:
    """Construct the board at the Vienna exit position."""
    board = chess.Board()
    for uci in VIENNA_SEED_UCIS:
        board.push_uci(uci)
    return board


def get_master_candidates(
    engine: UciSubprocessEngine,
    board: chess.Board,
    max_candidates: int,
    nodes: int,
) -> list[tuple[str, int]]:
    """Get top candidate White moves from master with evals."""
    # Use MultiPV to get multiple candidates
    engine.command(f"setoption name MultiPV value {max_candidates}")
    engine.command("isready", wait_for="readyok")
    engine._write(engine._position_cmd(board))
    lines = engine.command(f"go nodes {nodes}", wait_for="bestmove")
    engine.command("setoption name MultiPV value 1")
    engine.command("isready", wait_for="readyok")

    candidates: list[tuple[str, int]] = []
    seen_moves: set[str] = set()

    import re
    # Parse all info lines for multipv results
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

    # Deduplicate and sort by eval
    candidates.sort(key=lambda x: x[1], reverse=True)

    # If MultiPV didn't work well, fall back to single best
    if not candidates:
        score_info = engine.analyse_root(board, nodes=nodes)
        if score_info.pv:
            candidates.append((score_info.pv[0], score_info.score_cp))

    return candidates[:max_candidates]


def predict_sf18_replies(
    engine: UciSubprocessEngine,
    board: chess.Board,
    white_move: str,
    n_replies: int,
    nodes: int,
) -> list[tuple[str, int]]:
    """Predict top SF18 replies to a White move."""
    child = board.copy()
    child.push_uci(white_move)

    if child.is_game_over():
        return []

    engine.command(f"setoption name MultiPV value {n_replies}")
    engine.command("isready", wait_for="readyok")
    engine._write(engine._position_cmd(child))
    lines = engine.command(f"go nodes {nodes}", wait_for="bestmove")
    engine.command("setoption name MultiPV value 1")
    engine.command("isready", wait_for="readyok")

    replies: list[tuple[str, int]] = []
    seen: set[str] = set()

    import re
    for line in lines:
        if not line.startswith("info") or "multipv" not in line:
            continue
        m_pv = re.search(r" pv (\S+)", line)
        m_cp = re.search(r"score cp (-?\d+)", line)
        m_mate = re.search(r"score mate (-?\d+)", line)
        if m_pv:
            move_uci = m_pv.group(1)
            if move_uci in seen:
                continue
            seen.add(move_uci)
            if m_cp:
                score = int(m_cp.group(1))
            elif m_mate:
                mate_in = int(m_mate.group(1))
                score = 30000 - abs(mate_in) if mate_in > 0 else -(30000 - abs(mate_in))
            else:
                score = 0
            replies.append((move_uci, score))

    replies.sort(key=lambda x: x[1], reverse=True)

    if not replies:
        reply_uci, score = engine.predict_reply(child, nodes=nodes)
        replies.append((reply_uci, score.score_cp))

    return replies[:n_replies]


def run_mini_duel(
    master: UciSubprocessEngine,
    sf18: UciSubprocessEngine,
    board: chess.Board,
    playout_depth: int,
    nodes: int,
) -> tuple[str, int]:
    """Run a short playout alternating master (White) and sf18 (Black).

    Returns (result, final_eval_cp).
    result is '1-0', '0-1', or '1/2-1/2' based on eval trend.
    """
    pos = board.copy()
    last_eval = 0

    for ply in range(playout_depth):
        if pos.is_game_over():
            result_str = pos.result()
            return result_str, last_eval

        if pos.turn == chess.WHITE:
            # Master plays White
            score = master.analyse_root(pos, nodes=nodes)
            if score.pv:
                move_uci = score.pv[0]
                try:
                    move = chess.Move.from_uci(move_uci)
                    if move in pos.legal_moves:
                        pos.push(move)
                        last_eval = score.score_cp
                        continue
                except ValueError:
                    pass
            # Fallback
            move = next(iter(pos.legal_moves))
            pos.push(move)
        else:
            # SF18 plays Black
            reply_uci, score = sf18.predict_reply(pos, nodes=nodes)
            try:
                move = chess.Move.from_uci(reply_uci)
                if move in pos.legal_moves:
                    pos.push(move)
                    last_eval = -score.score_cp  # Negate for White's perspective
                    continue
            except ValueError:
                pass
            move = next(iter(pos.legal_moves))
            pos.push(move)

    # Classify by final eval
    final = master.analyse_root(pos, nodes=nodes)
    eval_cp = final.score_cp if pos.turn == chess.WHITE else -final.score_cp

    if eval_cp > 200:
        return "1-0", eval_cp
    elif eval_cp < -200:
        return "0-1", eval_cp
    else:
        return "1/2-1/2", eval_cp


def mine_node(
    master: UciSubprocessEngine,
    sf18: UciSubprocessEngine,
    board: chess.Board,
    book: TargetedBook,
    args: argparse.Namespace,
    depth: int = 0,
) -> None:
    """Mine a single node in the tree."""
    if depth > args.max_depth:
        return
    if board.is_game_over():
        return
    if board.turn != chess.WHITE:
        return  # Only mine White's moves

    history = " ".join(m.uci() for m in board.move_stack)
    fen = board.fen()

    print(f"  [depth={depth}] Mining: {fen}")
    print(f"  History: {history}")

    # Get master candidates
    candidates = get_master_candidates(master, board, args.max_candidates, args.nodes_analysis)
    print(f"  Candidates: {[(m, s) for m, s in candidates]}")

    if not candidates:
        return

    # For each candidate, predict SF18 replies and run mini-duels
    node = PolicyNode(
        fen=fen,
        move_history=history,
        depth_from_exit=depth,
    )

    promising_branches: list[tuple[str, str, float]] = []  # (white_move, sf18_reply, score)

    for white_move, master_eval in candidates:
        print(f"    Candidate: {white_move} (eval={master_eval}cp)")

        # Predict SF18 replies
        replies = predict_sf18_replies(sf18, board, white_move, args.reply_bundle, args.nodes_analysis)
        print(f"    SF18 replies: {[(r, s) for r, s in replies]}")

        stats = MoveStats(
            master_eval_cp=master_eval,
            predicted_replies=[r for r, _ in replies],
        )

        # For each reply, run mini-duel
        total_score = 0.0
        total_games = 0

        for reply_uci, reply_eval in replies:
            child = board.copy()
            child.push_uci(white_move)

            if child.is_game_over():
                stats.wins += 1
                total_score += 1.0
                total_games += 1
                continue

            child.push_uci(reply_uci)

            if child.is_game_over():
                result_str = child.result()
                if result_str == "1-0":
                    stats.wins += 1
                    total_score += 1.0
                elif result_str == "0-1":
                    stats.losses += 1
                else:
                    stats.draws += 1
                    total_score += 0.5
                total_games += 1
                continue

            # Run mini-duel from child position
            result, eval_cp = run_mini_duel(
                master, sf18, child, args.playout_depth, args.nodes_playout,
            )

            if result == "1-0":
                stats.wins += 1
                total_score += 1.0
            elif result == "0-1":
                stats.losses += 1
            else:
                stats.draws += 1
                total_score += 0.5
            total_games += 1

            # Also update empirical DB
            book.empirical_db.record_result(
                move_history=history,
                move_uci=white_move,
                fen=fen,
                result=result,
                eval_cp=eval_cp,
                reply_uci=reply_uci,
            )

        if total_games > 0:
            stats.empirical_score = total_score / total_games * 100
            stats.decisive_rate = (stats.wins + stats.losses) / total_games
            stats.visits = total_games

            # WDL score from eval
            if master_eval > 200:
                stats.wdl_score = 90.0
            elif master_eval > 100:
                stats.wdl_score = 75.0
            elif master_eval > 50:
                stats.wdl_score = 60.0
            elif master_eval > 0:
                stats.wdl_score = 55.0
            else:
                stats.wdl_score = 40.0

        stats.last_verified_ts = time.time()
        node.candidate_moves[white_move] = stats

        emp_score = stats.empirical_score
        print(f"    Result: {stats.wins}W-{stats.draws}D-{stats.losses}L = {emp_score:.1f}%")

        # Track promising branches for extension
        if emp_score >= args.min_score_extend:
            for reply_uci, _ in replies:
                promising_branches.append((white_move, reply_uci, emp_score))

    book.tree.add_node(node)
    book.save()

    # Extend promising branches
    for white_move, reply_uci, score in promising_branches:
        child = board.copy()
        try:
            child.push_uci(white_move)
            if child.is_game_over():
                continue
            child.push_uci(reply_uci)
            if child.is_game_over():
                continue
        except (ValueError, chess.IllegalMoveError):
            continue

        # Only extend if White has to move (after Black's reply)
        if child.turn == chess.WHITE:
            mine_node(master, sf18, child, book, args, depth + 2)


def main() -> None:
    args = parse_args()

    print("=== White Killer Line Book Mining ===")
    print(f"Selected line: Vienna Gambit Accepted")
    print(f"Exit FEN: {VIENNA_EXIT_FEN}")
    print(f"Max candidates: {args.max_candidates}")
    print(f"Reply bundle: {args.reply_bundle}")
    print(f"Max depth: {args.max_depth}")
    print(f"Nodes analysis: {args.nodes_analysis}")
    print(f"Nodes playout: {args.nodes_playout}")
    print()

    # Start engines
    print("Starting master engine...")
    master = UciSubprocessEngine(Path(args.master), threads=args.threads, hash_mb=args.hash)
    master.start()

    print("Starting SF18 engine...")
    sf18 = UciSubprocessEngine(Path(args.sf18), threads=args.threads, hash_mb=args.hash)
    sf18.start()

    # Initialize targeted book
    book = TargetedBook(
        book_path=Path(args.book_path),
        db_path=Path(args.db_path),
    )

    # Build exit board
    board = build_exit_board()
    print(f"\nExit position: {board.fen()}")
    print(f"Side to move: {'White' if board.turn == chess.WHITE else 'Black'}")

    # The exit position has Black to move (after 10. bxc3).
    # Black will play Qxe5+ (forced queen exchange). We need to handle this.
    # Actually, the exit FEN is: rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq -
    # Black to move. The main continuation is 10...Qxe5+ 11. Bxe5 (or dxe5).
    # We need to predict Black's move first, then mine White's responses.

    print("\nPredicting SF18's reply at exit position...")

    # Get SF18's move from the exit position (Black to move)
    sf18.command("isready", wait_for="readyok")
    sf18._write(sf18._position_cmd(board))
    lines = sf18.command(f"go nodes {args.nodes_analysis}", wait_for="bestmove")
    best_line = next(line for line in reversed(lines) if line.startswith("bestmove"))
    black_reply = best_line.split()[1]
    print(f"SF18 plays: {black_reply}")

    # Also get master's analysis of Black's position for reference
    master.command("isready", wait_for="readyok")
    master._write(master._position_cmd(board))
    m_lines = master.command(f"go nodes {args.nodes_analysis}", wait_for="bestmove")
    m_best = next(line for line in reversed(m_lines) if line.startswith("bestmove"))
    master_black = m_best.split()[1]
    print(f"Master's prediction for Black: {master_black}")

    # Get top 2 Black replies to consider
    black_replies_to_consider = list({black_reply, master_black})
    print(f"Black replies to explore: {black_replies_to_consider}")

    # For each Black reply, mine White's responses
    for br in black_replies_to_consider:
        child = board.copy()
        try:
            child.push_uci(br)
        except (ValueError, chess.IllegalMoveError):
            print(f"Illegal reply {br}, skipping")
            continue

        if child.is_game_over():
            print(f"Game over after {br}: {child.result()}")
            continue

        if child.turn == chess.WHITE:
            print(f"\n--- Mining after Black plays {br} ---")
            mine_node(master, sf18, child, book, args, depth=1)

    # Save final book
    book.save()
    print(f"\n=== Mining complete ===")
    print(f"Book saved to: {args.book_path}")
    print(book.summary())

    # Cleanup
    master.process.terminate() if master.process else None
    sf18.process.terminate() if sf18.process else None
    book.close()


if __name__ == "__main__":
    main()
