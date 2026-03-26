"""Subprocess management for base Stockfish engines."""

from __future__ import annotations

import re
import subprocess
from pathlib import Path

import chess

from wrapper.types import EngineConfig, NodeScore


class EngineProtocolError(RuntimeError):
    """Raised when a UCI subprocess does not behave as expected."""


class UciSubprocessEngine:
    """Minimal UCI client for a single Stockfish subprocess."""

    def __init__(
        self,
        path: Path,
        threads: int = 1,
        hash_mb: int = 64,
        show_wdl: bool = False,
    ) -> None:
        self.path = path
        self.threads = threads
        self.hash_mb = hash_mb
        self.show_wdl = show_wdl
        self.process: subprocess.Popen[str] | None = None

    def start(self) -> None:
        if self.process is not None and self.process.poll() is None:
            return
        self.process = subprocess.Popen(
            [str(self.path)],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        self.command("uci", wait_for="uciok")
        self.command(f"setoption name Threads value {self.threads}")
        self.command(f"setoption name Hash value {self.hash_mb}")
        if self.show_wdl:
            self.command("setoption name UCI_ShowWDL value true")
        self.command("isready", wait_for="readyok")

    def _ensure_alive(self) -> None:
        if self.process is None or self.process.poll() is not None:
            self.process = None
            self.start()

    def _write(self, line: str) -> None:
        self._ensure_alive()
        assert self.process is not None and self.process.stdin is not None
        self.process.stdin.write(f"{line}\n")
        self.process.stdin.flush()

    def command(self, line: str, wait_for: str | None = None) -> list[str]:
        self.start()
        self._write(line)
        if wait_for is None:
            return []
        return self.read_until(wait_for)

    def read_until(self, token: str) -> list[str]:
        assert self.process is not None and self.process.stdout is not None
        lines: list[str] = []
        while True:
            line = self.process.stdout.readline()
            if line == "":
                raise EngineProtocolError(f"unexpected EOF while waiting for {token!r}")
            stripped = line.strip()
            lines.append(stripped)
            if stripped == token or stripped.startswith(token):
                return lines

    def _position_cmd(self, board: chess.Board) -> str:
        moves = " ".join(move.uci() for move in board.move_stack)
        # If the board was constructed from a FEN (not startpos), use FEN encoding
        root = board.root()
        if root != chess.Board():
            fen = root.fen()
            if moves:
                return f"position fen {fen} moves {moves}"
            return f"position fen {fen}"
        if moves:
            return f"position startpos moves {moves}"
        return "position startpos"

    def _parse_score_from_info(self, lines: list[str]) -> NodeScore:
        score_cp = 0
        wdl = None
        depth = 0
        pv: list[str] = []
        nodes = 0
        for line in reversed(lines):
            if not line.startswith("info") or "score" not in line:
                continue
            m = re.search(r"score cp (-?\d+)", line)
            if m:
                score_cp = int(m.group(1))
            m = re.search(r"score mate (-?\d+)", line)
            if m:
                mate_in = int(m.group(1))
                score_cp = 30000 - abs(mate_in) if mate_in > 0 else -(30000 - abs(mate_in))
            m = re.search(r"depth (\d+)", line)
            if m:
                depth = int(m.group(1))
            m = re.search(r"nodes (\d+)", line)
            if m:
                nodes = int(m.group(1))
            m = re.search(r"wdl (\d+) (\d+) (\d+)", line)
            if m:
                wdl = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
            m = re.search(r" pv (.+)", line)
            if m:
                pv = m.group(1).split()
            break
        return NodeScore(score_cp=score_cp, wdl=wdl, depth=depth, pv=pv, nodes=nodes)

    def analyse_root(
        self,
        board: chess.Board,
        *,
        movetime_ms: int | None = None,
        nodes: int | None = None,
    ) -> NodeScore:
        # Use isready only - preserves hash table for better performance
        self.command("isready", wait_for="readyok")
        self._write(self._position_cmd(board))
        go = "go"
        if nodes:
            go += f" nodes {nodes}"
        elif movetime_ms:
            go += f" movetime {movetime_ms}"
        else:
            go += " movetime 200"
        lines = self.command(go, wait_for="bestmove")
        return self._parse_score_from_info(lines)

    def analyse_searchmoves(
        self,
        board: chess.Board,
        moves: list[str],
        *,
        movetime_ms: int | None = None,
        nodes: int | None = None,
    ) -> dict[str, NodeScore]:
        """Evaluate specific moves by analysing the position after each one."""
        results: dict[str, NodeScore] = {}
        for move_uci in moves:
            move = chess.Move.from_uci(move_uci)
            if move not in board.legal_moves:
                results[move_uci] = NodeScore()
                continue
            child = board.copy()
            child.push(move)
            # Evaluate from opponent's perspective
            score = self.analyse_root(child, movetime_ms=movetime_ms, nodes=nodes)
            # Negate to get score from our perspective
            results[move_uci] = NodeScore(
                score_cp=-score.score_cp,
                wdl=score.wdl,
                depth=score.depth,
                pv=[move_uci] + score.pv,
                nodes=score.nodes,
            )
        return results

    def predict_reply(
        self,
        board: chess.Board,
        *,
        movetime_ms: int | None = None,
        nodes: int | None = None,
    ) -> tuple[str, NodeScore]:
        self.command("isready", wait_for="readyok")
        self._write(self._position_cmd(board))
        go = "go"
        if nodes:
            go += f" nodes {nodes}"
        elif movetime_ms:
            go += f" movetime {movetime_ms}"
        else:
            go += " movetime 100"
        lines = self.command(go, wait_for="bestmove")
        best_line = next(line for line in reversed(lines) if line.startswith("bestmove"))
        best_uci = best_line.split()[1]
        score = self._parse_score_from_info(lines)
        return best_uci, score

    def verify_duel(
        self,
        board: chess.Board,
        challenger_uci: str,
        base_uci: str,
        *,
        movetime_ms: int | None = None,
        nodes: int | None = None,
    ) -> tuple[NodeScore, NodeScore]:
        scores = self.analyse_searchmoves(
            board,
            [challenger_uci, base_uci],
            movetime_ms=movetime_ms,
            nodes=nodes,
        )
        return scores.get(challenger_uci, NodeScore()), scores.get(base_uci, NodeScore())

    def bestmove(
        self,
        board: chess.Board,
        movetime_ms: int = 200,
        nodes: int | None = None,
        go_params: dict[str, int | None] | None = None,
    ) -> tuple[str, str | None, list[str]]:
        """Return (bestmove_uci, ponder_uci, info_lines)."""
        # Do NOT call ucinewgame here - preserve hash table across moves
        self.command("isready", wait_for="readyok")
        self._write(self._position_cmd(board))
        # If full go_params provided (wtime/btime), forward them for proper TC
        if go_params and any(go_params.get(k) for k in ("wtime", "btime")):
            parts = ["go"]
            for k in ("wtime", "btime", "winc", "binc", "movestogo", "depth", "nodes", "movetime"):
                v = go_params.get(k)
                if v is not None:
                    parts.extend([k, str(v)])
            go = " ".join(parts)
        elif nodes:
            go = f"go nodes {nodes}"
        else:
            go = f"go movetime {movetime_ms}"
        lines = self.command(go, wait_for="bestmove")
        best_line = next(line for line in reversed(lines) if line.startswith("bestmove"))
        parts = best_line.split()
        bestmove = parts[1]
        ponder = parts[3] if len(parts) >= 4 and parts[2] == "ponder" else None
        # Collect info lines for forwarding to GUI
        info_lines = [l for l in lines if l.startswith("info") and "score" in l]
        return bestmove, ponder, info_lines

    def quit(self) -> None:
        if self.process is None:
            return
        try:
            self._write("quit")
        except Exception:
            pass
        try:
            self.process.wait(timeout=5)
        except Exception:
            self.process.kill()
        self.process = None


class EnginePool:
    """Wrapper-level engine accessor with evaluator, nominal, and verifier roles."""

    def __init__(self, config: EngineConfig) -> None:
        self.config = config
        threads_per = max(1, config.threads // 3) if config.threads >= 3 else 1

        self.evaluator = UciSubprocessEngine(
            config.base_path, threads_per, config.hash_mb, config.show_wdl
        )
        nominal_path = config.nominal_path or config.base_path
        self.nominal = UciSubprocessEngine(
            nominal_path, threads_per, config.hash_mb, config.show_wdl
        )
        verify_path = config.verify_path or config.base_path
        self.verifier = UciSubprocessEngine(
            verify_path, threads_per, config.hash_mb, config.show_wdl
        )

    def analyse_root(
        self, board: chess.Board, *, nodes: int | None = None
    ) -> NodeScore:
        return self.evaluator.analyse_root(
            board,
            movetime_ms=self.config.movetime_ms,
            nodes=nodes or self.config.nodes_main,
        )

    def analyse_searchmoves(
        self, board: chess.Board, moves: list[str], *, nodes: int | None = None
    ) -> dict[str, NodeScore]:
        return self.evaluator.analyse_searchmoves(
            board,
            moves,
            movetime_ms=self.config.movetime_ms,
            nodes=nodes or self.config.nodes_child,
        )

    def predict_reply(
        self, board: chess.Board, *, nodes: int | None = None
    ) -> tuple[str, NodeScore]:
        return self.nominal.predict_reply(
            board,
            movetime_ms=self.config.movetime_ms,
            nodes=nodes or self.config.nodes_child,
        )

    def verify_duel(
        self,
        board: chess.Board,
        challenger_uci: str,
        base_uci: str,
        *,
        nodes: int | None = None,
    ) -> tuple[NodeScore, NodeScore]:
        return self.verifier.verify_duel(
            board,
            challenger_uci,
            base_uci,
            movetime_ms=self.config.movetime_ms,
            nodes=nodes or self.config.nodes_verify,
        )

    def bestmove(
        self, board: chess.Board, movetime_ms: int | None = None,
        go_params: dict[str, int | None] | None = None,
    ) -> tuple[str, str | None, list[str]]:
        return self.evaluator.bestmove(
            board,
            movetime_ms or self.config.movetime_ms,
            self.config.nodes_main,
            go_params=go_params,
        )

    def close(self) -> None:
        self.evaluator.quit()
        self.nominal.quit()
        self.verifier.quit()
