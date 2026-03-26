"""CounterLine UCI frontend."""

from __future__ import annotations

import os
import sys
from contextlib import suppress
from pathlib import Path
from typing import TextIO

import chess
import typer
import yaml

from wrapper.determine import determine_bestmove, determine_countermove
from wrapper.engine_pool import EnginePool
from wrapper.opening_lock import detect_opening_family, get_seed_move, is_book_complete, lock_color_for_family, opening_lock
from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB
from wrapper.rollout import annotate_with_engine_scores, generate_candidates
from wrapper.telemetry import TelemetryLogger
from wrapper.types import AppConfig, CandidateMove, EngineConfig, MoveDecision, WrapperConfig

cli = typer.Typer(add_completion=False)

_VERSION = "0.1.0"


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
    if nominal_path := os.getenv("COUNTERLINE_NOMINAL_PATH"):
        config.engine.nominal_path = Path(nominal_path)
    if verify_path := os.getenv("COUNTERLINE_VERIFY_PATH"):
        config.engine.verify_path = Path(verify_path)
    if db_path := os.getenv("COUNTERLINE_DB_PATH"):
        config.wrapper.db_path = Path(db_path)
    if telemetry_path := os.getenv("COUNTERLINE_TELEMETRY_PATH"):
        config.wrapper.telemetry_path = Path(telemetry_path)
    return config


class UciApp:
    """Full UCI protocol implementation for CounterLine."""

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
        self._pool: EnginePool | None = engine_pool
        self._pool_started = engine_pool is not None

    @property
    def engine_pool(self) -> EnginePool:
        if self._pool is None:
            self._pool = EnginePool(self.config.engine)
            self._pool_started = True
        return self._pool

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
    def parse_go_params(command: str) -> dict[str, int | None]:
        tokens = command.split()
        params: dict[str, int | None] = {"movetime": None, "nodes": None, "depth": None}
        for key in params:
            if key in tokens:
                idx = tokens.index(key)
                if idx + 1 < len(tokens):
                    try:
                        params[key] = int(tokens[idx + 1])
                    except ValueError:
                        pass
        # Parse wtime/btime/winc/binc for time management
        for key in ["wtime", "btime", "winc", "binc"]:
            if key in tokens:
                idx = tokens.index(key)
                if idx + 1 < len(tokens):
                    try:
                        params[key] = int(tokens[idx + 1])
                    except ValueError:
                        pass
        return params

    def _apply_setoption(self, command: str) -> None:
        # Parse "setoption name X value Y"
        parts = command.split()
        if "name" not in parts:
            return
        name_idx = parts.index("name") + 1
        if "value" in parts:
            val_idx = parts.index("value")
            name = " ".join(parts[name_idx:val_idx])
            value = " ".join(parts[val_idx + 1:])
        else:
            name = " ".join(parts[name_idx:])
            value = ""

        name_lower = name.lower()
        if name_lower == "baseenginepath":
            self.config.engine.base_path = Path(value)
        elif name_lower == "nominalenginepath":
            self.config.engine.nominal_path = Path(value)
        elif name_lower == "verifyenginepath":
            self.config.engine.verify_path = Path(value)
        elif name_lower == "threads":
            self.config.engine.threads = max(1, int(value))
        elif name_lower == "hash":
            self.config.engine.hash_mb = max(1, int(value))
        elif name_lower == "openinglock":
            self.config.wrapper.opening_lock = value.lower() in ("true", "1", "yes")
        elif name_lower == "wrappermode":
            self.config.wrapper.wrapper_mode = value
        elif name_lower == "opponentprofile":
            self.config.wrapper.opponent_profile.name = value
        elif name_lower == "enablewdl":
            self.config.engine.show_wdl = value.lower() in ("true", "1", "yes")
        elif name_lower == "maxcandidates":
            self.config.wrapper.max_candidates = max(1, int(value))
        elif name_lower == "nodesmain":
            self.config.engine.nodes_main = int(value) if value else None
        elif name_lower == "nodeschild":
            self.config.engine.nodes_child = int(value) if value else None
        elif name_lower == "nodesverify":
            self.config.engine.nodes_verify = int(value) if value else None
        elif name_lower == "logpath":
            self.config.wrapper.telemetry_path = Path(value)
            self.telemetry = TelemetryLogger(Path(value))

    def _available_time_ms(self, go_params: dict[str, int | None]) -> int | None:
        """Estimate available time from UCI go params."""
        if self.board.turn == chess.WHITE:
            t = go_params.get("wtime")
        else:
            t = go_params.get("btime")
        return t

    def choose_move(self, go_params: dict[str, int | None] | None = None) -> tuple[str, str | None, str, bool, list[str]]:
        """Return (bestmove, ponder, reason, used_wrapper, info_lines)."""
        go_params = go_params or {}
        movetime_ms = go_params.get("movetime")

        # Check seed move first (opening lock)
        if self.config.wrapper.opening_lock:
            seed = get_seed_move(self.board)
            if seed is not None:
                return seed, None, "seed_line", True, []

        in_suite, family = opening_lock(self.board)
        lock_color = lock_color_for_family(family)
        side_name = "white" if self.board.turn == chess.WHITE else "black"

        # Time budget guard: don't run wrapper if clock is too low
        avail = self._available_time_ms(go_params)
        has_time = avail is None or avail > 5000  # need >5s to justify wrapper overhead

        # Only run wrapper when: in suite, book complete, our color, sufficient time,
        # AND we have repertoire data for this position (or it's the exact exit FEN)
        has_rep_data = bool(self.repertoire.candidate_moves(self.board, family)) if family != "unknown" else False
        is_critical = self.repertoire_db.is_critical(self.board) if has_rep_data else False
        at_exit = self.board.fen(en_passant="fen") in (
            "r1bqrnk1/pp2bppp/2p2n2/3p2B1/3P4/2NBP3/PPQ1NPPP/R4RK1 w - - 9 11",
            "rnbq1rk1/pp2bppp/2p5/3p4/2PP4/2PB1N2/P4PPP/R1BQ1RK1 b - - 0 10",
        )
        should_use_wrapper = (
            in_suite
            and lock_color == side_name
            and has_time
            and (
                (is_book_complete(self.board) and at_exit)
                or (has_rep_data and is_critical)
            )
        )

        if should_use_wrapper:
            # Lightweight approach: get base move with time control, then check
            # if we should override with a repertoire-backed move.
            try:
                base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                    self.board, movetime_ms, go_params=go_params
                )
            except Exception:
                base_move, base_ponder = self._fallback_move()
                info_lines = []

            # Check if repertoire strongly recommends a different move
            rep_candidates = self.repertoire.candidate_moves(self.board, family)
            if rep_candidates:
                top_rep = rep_candidates[0]
                if top_rep.move_uci != base_move and top_rep.plan_score_cp >= 3:
                    # Quick verification: evaluate both moves with a small probe
                    try:
                        scores = self.engine_pool.analyse_searchmoves(
                            self.board,
                            [top_rep.move_uci, base_move],
                            nodes=10000,
                        )
                        rep_score = scores.get(top_rep.move_uci)
                        base_score = scores.get(base_move)
                        if (
                            rep_score is not None
                            and base_score is not None
                            and rep_score.score_cp >= base_score.score_cp - 5
                        ):
                            return top_rep.move_uci, None, "repertoire_override", True, info_lines
                    except Exception:
                        pass

            return base_move, base_ponder, "base_with_rep_check", False, info_lines

        # Outside suite: just use base engine (forward info lines directly)
        try:
            base_move, base_ponder, info_lines = self.engine_pool.bestmove(self.board, movetime_ms, go_params=go_params)
        except Exception:
            base_move, base_ponder = self._fallback_move()
            info_lines = []

        return base_move, base_ponder, "outside_suite", False, info_lines

    def loop(self) -> int:
        for raw in self.stdin:
            command = raw.strip()
            if not command:
                continue
            if command == "uci":
                self.send(f"id name CounterLine {_VERSION}")
                self.send("id author Stockfish developers + LaunchDay Studio Inc.")
                self.send("")
                self.send("option name BaseEnginePath type string default bin/stockfish-master")
                self.send("option name NominalEnginePath type string default bin/stockfish-master")
                self.send("option name VerifyEnginePath type string default bin/stockfish-master")
                self.send("option name Threads type spin default 1 min 1 max 512")
                self.send("option name Hash type spin default 64 min 1 max 131072")
                self.send("option name OpeningLock type check default true")
                self.send("option name WrapperMode type combo default selective var selective var always var off")
                self.send("option name OpponentProfile type string default stockfish-master")
                self.send("option name EnableWDL type check default false")
                self.send("option name MaxCandidates type spin default 4 min 1 max 16")
                self.send("option name NodesMain type spin default 0 min 0 max 100000000")
                self.send("option name NodesChild type spin default 0 min 0 max 100000000")
                self.send("option name NodesVerify type spin default 0 min 0 max 100000000")
                self.send("option name LogPath type string default results/telemetry.jsonl")
                self.send("uciok")
            elif command == "isready":
                # Pre-start the evaluator engine so first go has no startup delay
                if not self._pool_started:
                    _ = self.engine_pool  # triggers lazy init + start
                self.send("readyok")
            elif command == "ucinewgame":
                self.board.reset()
                # Forward ucinewgame to the evaluator so it clears its hash
                # between games (but NOT between moves within a game)
                if self._pool_started and self._pool is not None:
                    try:
                        self._pool.evaluator.command("ucinewgame")
                        self._pool.evaluator.command("isready", wait_for="readyok")
                    except Exception:
                        pass
            elif command.startswith("position"):
                self.apply_position(command)
            elif command.startswith("setoption"):
                self._apply_setoption(command)
            elif command.startswith("go"):
                go_params = self.parse_go_params(command)
                bestmove, ponder, reason, used_wrapper, info_lines = self.choose_move(go_params)

                family = detect_opening_family(self.board)
                decision = MoveDecision(
                    bestmove=bestmove,
                    ponder=ponder,
                    family=family,
                    reason=reason,
                    used_wrapper=used_wrapper,
                    base_move=bestmove if not used_wrapper else None,
                )

                self.telemetry.log_decision(
                    fen=self.board.fen(en_passant="fen"),
                    family=family,
                    decision=decision,
                    thread_budget=self.config.engine.threads,
                    source_of_priors="repertoire" if used_wrapper else "base",
                )

                if used_wrapper:
                    self.send(f"info string CounterLine override: {reason}")
                # Forward engine info lines (search depth, score, PV, etc.)
                for info_line in info_lines:
                    self.send(info_line)
                # If no info lines captured, emit a minimal one
                if not info_lines:
                    self.send("info depth 1 score cp 0 nodes 1 pv " + bestmove)
                if ponder:
                    self.send(f"bestmove {bestmove} ponder {ponder}")
                else:
                    self.send(f"bestmove {bestmove}")
            elif command == "stop":
                pass
            elif command == "quit":
                break
            elif command == "d":
                self.send(self.board.unicode())
        self.close()
        return 0

    def close(self) -> None:
        if self._pool_started:
            with suppress(Exception):
                self.engine_pool.close()


@cli.command()
def main(
    config: str = typer.Option("", "--config", help="Path to YAML config file"),
) -> None:
    """Run the CounterLine UCI application."""

    config_path = Path(config) if config else None
    app = UciApp(
        load_config(config_path),
        stdin=sys.stdin,
        stdout=sys.stdout,
    )
    raise typer.Exit(app.loop())


def entrypoint() -> None:
    """Console script entry point — must go through Typer's CLI runner."""
    cli()


if __name__ == "__main__":
    cli()
