"""CounterLine UCI frontend."""

from __future__ import annotations

import os
from contextlib import suppress
from pathlib import Path
from typing import TextIO

import chess
import typer
import yaml

from wrapper.determine import determine_bestmove
from wrapper.engine_pool import EnginePool
from wrapper.opening_lock import detect_opening_family, is_book_complete, lock_color_for_family, opening_lock
from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB
from wrapper.rollout import annotate_with_engine_scores, generate_candidates
from wrapper.telemetry import TelemetryLogger
from wrapper.types import AppConfig, CandidateMove

cli = typer.Typer(add_completion=False)


def load_config(path: Path | None) -> AppConfig:
    """Load YAML configuration or use defaults."""

    if path is None:
        config = AppConfig()
    else:
        with path.open("r", encoding="utf-8") as handle:
            payload = yaml.safe_load(handle) or {}
        config = AppConfig.model_validate(payload)
    if engine_path := os.getenv("COUNTERLINE_ENGINE_PATH"):
        config.engine.base_path = Path(engine_path)
    if fallback_path := os.getenv("COUNTERLINE_FALLBACK_PATH"):
        config.engine.fallback_path = Path(fallback_path)
    if db_path := os.getenv("COUNTERLINE_DB_PATH"):
        config.wrapper.db_path = Path(db_path)
    if telemetry_path := os.getenv("COUNTERLINE_TELEMETRY_PATH"):
        config.wrapper.telemetry_path = Path(telemetry_path)
    return config


class UciApp:
    """Minimal UCI protocol implementation for CounterLine."""

    def __init__(
        self,
        config: AppConfig,
        *,
        stdin: TextIO,
        stdout: TextIO,
        engine_pool: EnginePool | None = None,
    ) -> None:
        self.config = config
        self.stdin = stdin
        self.stdout = stdout
        self.board = chess.Board()
        self.repertoire_db = RepertoireDB(config.wrapper.db_path)
        self.repertoire_db.initialize()
        self.repertoire = Repertoire(self.repertoire_db)
        self.telemetry = TelemetryLogger(config.wrapper.telemetry_path)
        self.engine_pool = engine_pool or EnginePool(config.engine)

    def send(self, line: str) -> None:
        self.stdout.write(f"{line}\n")
        self.stdout.flush()

    def apply_position(self, command: str) -> None:
        tokens = command.split()
        self.board = chess.Board()
        if len(tokens) < 2:
            return
        index = 1
        if tokens[index] == "startpos":
            self.board.reset()
            index += 1
        elif tokens[index] == "fen":
            fen = " ".join(tokens[index + 1 : index + 7])
            self.board = chess.Board(fen)
            index += 7
        if index < len(tokens) and tokens[index] == "moves":
            for uci in tokens[index + 1 :]:
                self.board.push_uci(uci)

    def _fallback_move(self) -> tuple[str, str | None]:
        move = next(iter(self.board.legal_moves), None)
        if move is None:
            return "0000", None
        return move.uci(), None

    @staticmethod
    def parse_movetime(command: str) -> int | None:
        tokens = command.split()
        if "movetime" in tokens:
            index = tokens.index("movetime")
            if index + 1 < len(tokens):
                return int(tokens[index + 1])
        return None

    def choose_move(self, movetime_ms: int | None = None) -> tuple[str, str | None, str, bool]:
        in_suite, family = opening_lock(self.board)
        lock_color = lock_color_for_family(family)
        side_name = "white" if self.board.turn == chess.WHITE else "black"

        try:
            base_move, base_ponder = self.engine_pool.bestmove(self.board, movetime_ms)
        except Exception:
            base_move, base_ponder = self._fallback_move()

        base_candidate = CandidateMove(move_uci=base_move, engine_score_cp=0, combined_score_cp=0, source="base")
        used_wrapper = False
        reason = "outside_suite"

        if in_suite and is_book_complete(self.board) and lock_color == side_name:
            repertoire_moves = self.repertoire.candidate_moves(self.board, family)
            rollout_moves = generate_candidates(self.board, family, limit=4)
            engine_scores = {candidate.move_uci: candidate.plan_score_cp for candidate in rollout_moves}
            merged = annotate_with_engine_scores(repertoire_moves or rollout_moves, engine_scores)
            challenger = merged[0] if merged else None
            decision = determine_bestmove(family=family, base=base_candidate, challenger=challenger)
            used_wrapper = decision.used_wrapper
            reason = decision.reason
            if challenger is not None and decision.used_wrapper:
                self.repertoire.remember(
                    self.board,
                    family,
                    challenger.move_uci,
                    challenger.combined_score_cp,
                    note="accepted_by_wrapper",
                )
            return decision.bestmove, base_ponder, reason, used_wrapper

        if in_suite:
            reason = "book_or_color_lock"
        return base_move, base_ponder, reason, used_wrapper

    def loop(self) -> int:
        for raw in self.stdin:
            command = raw.strip()
            if not command:
                continue
            if command == "uci":
                self.send("id name CounterLine")
                self.send("id author LaunchDay Studio Inc.")
                self.send("option name CounterLineConfig type string default configs/engines.yml")
                self.send("uciok")
            elif command == "isready":
                self.send("readyok")
            elif command == "ucinewgame":
                self.board.reset()
            elif command.startswith("position"):
                self.apply_position(command)
            elif command.startswith("go"):
                bestmove, ponder, reason, used_wrapper = self.choose_move(self.parse_movetime(command))
                self.telemetry.log(
                    {
                        "fen": self.board.fen(en_passant="fen"),
                        "family": detect_opening_family(self.board),
                        "bestmove": bestmove,
                        "reason": reason,
                        "used_wrapper": used_wrapper,
                    }
                )
                if ponder:
                    self.send(f"bestmove {bestmove} ponder {ponder}")
                else:
                    self.send(f"bestmove {bestmove}")
            elif command == "stop":
                self.send("bestmove 0000")
            elif command == "quit":
                break
            elif command.startswith("setoption"):
                continue
            elif command == "d":
                self.send(self.board.unicode())
        self.close()
        return 0

    def close(self) -> None:
        with suppress(Exception):
            self.engine_pool.close()


@cli.command()
def main(config: Path | None = typer.Option(None, "--config", exists=False, dir_okay=False)) -> None:
    """Run the CounterLine UCI application."""

    app = UciApp(load_config(config), stdin=typer.get_text_stream("stdin"), stdout=typer.get_text_stream("stdout"))
    raise typer.Exit(app.loop())


if __name__ == "__main__":
    main()
