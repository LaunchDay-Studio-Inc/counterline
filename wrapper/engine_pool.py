"""Subprocess management for base Stockfish engines."""

from __future__ import annotations

import subprocess
from pathlib import Path

import chess

from wrapper.types import EngineConfig


class EngineProtocolError(RuntimeError):
    """Raised when a UCI subprocess does not behave as expected."""


class UciSubprocessEngine:
    """Minimal UCI client for a single Stockfish subprocess."""

    def __init__(self, path: Path, threads: int = 1, hash_mb: int = 64) -> None:
        self.path = path
        self.threads = threads
        self.hash_mb = hash_mb
        self.process: subprocess.Popen[str] | None = None

    def start(self) -> None:
        if self.process is not None:
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
        self.command("isready", wait_for="readyok")

    def _write(self, line: str) -> None:
        if self.process is None or self.process.stdin is None:
            raise EngineProtocolError("engine process is not running")
        self.process.stdin.write(f"{line}\n")
        self.process.stdin.flush()

    def command(self, line: str, wait_for: str | None = None) -> list[str]:
        self.start()
        self._write(line)
        if wait_for is None:
            return []
        return self.read_until(wait_for)

    def read_until(self, token: str) -> list[str]:
        if self.process is None or self.process.stdout is None:
            raise EngineProtocolError("engine process is not running")
        lines: list[str] = []
        while True:
            line = self.process.stdout.readline()
            if line == "":
                raise EngineProtocolError(f"unexpected EOF while waiting for {token!r}")
            stripped = line.strip()
            lines.append(stripped)
            if stripped == token or stripped.startswith(token):
                return lines

    def bestmove(self, board: chess.Board, movetime_ms: int, nodes: int | None = None) -> tuple[str, str | None]:
        moves = " ".join(move.uci() for move in board.move_stack)
        self.command("ucinewgame")
        self.command("isready", wait_for="readyok")
        if moves:
            self.command(f"position startpos moves {moves}")
        else:
            self.command("position startpos")
        go = f"go movetime {movetime_ms}"
        if nodes:
            go = f"{go} nodes {nodes}"
        lines = self.command(go, wait_for="bestmove")
        best_line = next(line for line in reversed(lines) if line.startswith("bestmove"))
        parts = best_line.split()
        bestmove = parts[1]
        ponder = parts[3] if len(parts) >= 4 and parts[2] == "ponder" else None
        return bestmove, ponder

    def quit(self) -> None:
        if self.process is None:
            return
        try:
            self._write("quit")
        finally:
            self.process.wait(timeout=5)
            self.process = None


class EnginePool:
    """Wrapper-level engine accessor."""

    def __init__(self, config: EngineConfig) -> None:
        self.config = config
        self.base = UciSubprocessEngine(config.base_path, config.threads, config.hash_mb)
        self.fallback = (
            UciSubprocessEngine(config.fallback_path, config.threads, config.hash_mb)
            if config.fallback_path
            else None
        )

    def bestmove(self, board: chess.Board, movetime_ms: int | None = None) -> tuple[str, str | None]:
        return self.base.bestmove(board, movetime_ms or self.config.movetime_ms, self.config.nodes)

    def bestmove_fallback(self, board: chess.Board, movetime_ms: int | None = None) -> tuple[str, str | None]:
        if self.fallback is None:
            return self.bestmove(board, movetime_ms)
        return self.fallback.bestmove(board, movetime_ms or self.config.movetime_ms, self.config.nodes)

    def close(self) -> None:
        self.base.quit()
        if self.fallback is not None:
            self.fallback.quit()
