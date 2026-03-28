"""CounterLine UCI frontend."""

from __future__ import annotations

import os
import sys
from contextlib import suppress
from pathlib import Path
from typing import Any, TextIO

import chess
import typer
import yaml

from wrapper.determine import determine_bestmove, determine_countermove
from wrapper.engine_pool import EnginePool
from wrapper.opening_lock import detect_opening_family, get_seed_move, is_book_complete, lock_color_for_family, opening_lock
from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB
from wrapper.rollout import annotate_with_engine_scores, generate_candidates
from wrapper.targeted_book import TargetedBook, VIENNA_SEED_UCIS, VIENNA_EXIT_FEN
from wrapper.black_targeted_book import BlackTargetedBook, CAROKANN_SEED_UCIS, CAROKANN_EXIT_FEN
from wrapper.telemetry import TelemetryLogger
from wrapper.types import AppConfig, CandidateMove, EngineConfig, MoveDecision, WrapperConfig

cli = typer.Typer(add_completion=False)

# Mutable container for profile injection from __main__
_ACTIVE_PROFILE: dict[str, str | None] = {"name": None}

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
        profile: str | None = None,
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
        self.profile = profile or _ACTIVE_PROFILE.get("name")

        # White specialist book
        self._targeted_book: TargetedBook | None = None
        self._white_killer_opts = {
            "use_learned_book": True,
            "book_depth_limit": 30,
            "empirical_weight": 0.7,
            "wdl_weight": 0.3,
            "decisive_bonus": 0.1,
            "max_candidates": 4,
            "reply_bundle_size": 2,
        }
        if self.profile in ("white_killer", "combined"):
            self._init_white_killer()

        # Black specialist book
        self._black_targeted_book: BlackTargetedBook | None = None
        self._black_killer_opts = {
            "use_learned_book": True,
            "book_depth_limit": 40,
            "empirical_weight": 0.6,
            "wdl_weight": 0.25,
            "decisive_bonus": 0.15,
            "max_candidates": 5,
            "reply_bundle_size": 3,
        }
        if self.profile in ("black_killer", "combined"):
            self._init_black_killer()

    @property
    def engine_pool(self) -> EnginePool:
        if self._pool is None:
            self._pool = EnginePool(self.config.engine)
            self._pool_started = True
        return self._pool

    def _init_white_killer(self) -> None:
        """Initialize the White killer specialist book."""
        root = Path(__file__).resolve().parent.parent
        book_path = root / "data" / "white_killer" / "book" / "vienna.json"
        db_path = root / "data" / "white_killer" / "db" / "empirical.sqlite"
        opts = self._white_killer_opts
        self._targeted_book = TargetedBook(
            book_path=book_path,
            db_path=db_path,
            empirical_weight=opts["empirical_weight"],
            wdl_weight=opts["wdl_weight"],
            decisive_bonus=opts["decisive_bonus"],
            book_depth_limit=opts["book_depth_limit"],
        )

    def _init_black_killer(self) -> None:
        """Initialize the Black killer specialist book."""
        root = Path(__file__).resolve().parent.parent
        book_path = root / "data" / "black_killer" / "book" / "carokann.json"
        db_path = root / "data" / "black_killer" / "db" / "empirical.sqlite"
        opts = self._black_killer_opts
        self._black_targeted_book = BlackTargetedBook(
            book_path=book_path,
            db_path=db_path,
            empirical_weight=opts["empirical_weight"],
            wdl_weight=opts["wdl_weight"],
            decisive_bonus=opts["decisive_bonus"],
            book_depth_limit=opts["book_depth_limit"],
        )

    def _is_in_vienna_line(self) -> bool:
        """Check if current position is within the Vienna Gambit line."""
        if self._targeted_book is None:
            return False
        return (
            self._targeted_book.is_in_seed_line(self.board)
            or self._targeted_book.is_at_exit(self.board)
            or self._targeted_book.is_past_exit(self.board)
            or self._targeted_book._is_exit_root(self.board)
        )

    def _is_in_carokann_line(self) -> bool:
        """Check if current position is within the Caro-Kann killer line."""
        if self._black_targeted_book is None:
            return False
        return (
            self._black_targeted_book.is_in_seed_line(self.board)
            or self._black_targeted_book.is_at_exit(self.board)
            or self._black_targeted_book.is_past_exit(self.board)
            or self._black_targeted_book._is_exit_root(self.board)
        )

    def _choose_black_killer_move(
        self, go_params: dict[str, int | None]
    ) -> tuple[str, str | None, str, bool, list[str], dict[str, Any]]:
        """Black specialist move selection.

        Returns (bestmove, ponder, reason, used_wrapper, info_lines, telemetry_extra).
        """
        assert self._black_targeted_book is not None
        movetime_ms = go_params.get("movetime")
        tele: dict[str, Any] = {
            "used_learned_book": False,
            "used_targeted_override": False,
            "candidate_count": 0,
            "empirical_score_delta": 0.0,
            "base_move": "",
            "chosen_move": "",
        }

        # 1. Seed line — within opening moves
        seed = self._black_targeted_book.get_seed_move(self.board)
        if seed is not None:
            tele["used_learned_book"] = True
            tele["chosen_move"] = seed
            return seed, None, "black_seed_line", True, [], tele

        # 2. At or past exit, Black's turn — use learned book
        #    Only override base engine when book move has proven empirical advantage
        if self.board.turn == chess.BLACK and self._black_targeted_book.is_past_exit(self.board):
            if self._black_killer_opts["use_learned_book"]:
                book_move, book_meta = self._black_targeted_book.lookup_book_move(self.board)

                if book_move is not None:
                    tele.update(book_meta)
                    emp_score = book_meta.get("empirical_score", 50.0)
                    decisive = book_meta.get("decisive_rate", 0.0)

                    # Use book without engine query if proven advantage
                    if emp_score > 55.0 or (emp_score >= 50.0 and decisive > 0.15):
                        tele["used_learned_book"] = True
                        tele["chosen_move"] = book_move
                        tele["used_targeted_override"] = True
                        return book_move, None, "black_learned_book", True, [], tele

                # No proven book override — delegate to engine directly
                # (fall through to section 4 below)

        # 3. At or past exit, White's turn — delegate to engine
        if self.board.turn == chess.WHITE:
            try:
                base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                    self.board, movetime_ms, go_params=go_params
                )
                tele["base_move"] = base_move
                tele["chosen_move"] = base_move
                return base_move, base_ponder, "white_turn_delegate", False, info_lines, tele
            except Exception:
                base_move, _ = self._fallback_move()
                tele["chosen_move"] = base_move
                return base_move, None, "white_turn_fallback", False, [], tele

        # 4. Past exit, Black's turn, but no book move — targeted root correction
        if self.board.turn == chess.BLACK and self._black_targeted_book.is_past_exit(self.board):
            try:
                base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                    self.board, movetime_ms, go_params=go_params
                )
                tele["base_move"] = base_move
                tele["chosen_move"] = base_move

                history = self._black_targeted_book.move_history_key(self.board)
                if self._black_targeted_book.tree.is_in_tree(history):
                    tele["used_targeted_override"] = True
                    tele["candidate_count"] = 1

                return base_move, base_ponder, "black_root_correction", True, info_lines, tele
            except Exception:
                base_move, _ = self._fallback_move()
                tele["chosen_move"] = base_move
                return base_move, None, "black_root_correction_fallback", False, [], tele

        # 5. Fallback — delegate to base engine
        try:
            base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                self.board, movetime_ms, go_params=go_params
            )
            tele["base_move"] = base_move
            tele["chosen_move"] = base_move
            return base_move, base_ponder, "outside_line_delegate", False, info_lines, tele
        except Exception:
            base_move, _ = self._fallback_move()
            tele["chosen_move"] = base_move
            return base_move, None, "outside_line_fallback", False, [], tele

    def _choose_white_killer_move(
        self, go_params: dict[str, int | None]
    ) -> tuple[str, str | None, str, bool, list[str], dict[str, Any]]:
        """White specialist move selection.

        Returns (bestmove, ponder, reason, used_wrapper, info_lines, telemetry_extra).
        """
        assert self._targeted_book is not None
        movetime_ms = go_params.get("movetime")
        tele: dict[str, Any] = {
            "used_learned_book": False,
            "used_targeted_override": False,
            "candidate_count": 0,
            "empirical_score_delta": 0.0,
            "base_move": "",
            "chosen_move": "",
        }

        # 1. Seed line — within opening moves
        seed = self._targeted_book.get_seed_move(self.board)
        if seed is not None:
            tele["used_learned_book"] = True
            tele["chosen_move"] = seed
            return seed, None, "white_seed_line", True, [], tele

        # 2. At or past exit, White's turn — use learned book
        if self.board.turn == chess.WHITE and self._targeted_book.is_past_exit(self.board):
            if self._white_killer_opts["use_learned_book"]:
                book_move, book_meta = self._targeted_book.lookup_book_move(self.board)
                if book_move is not None:
                    tele["used_learned_book"] = True
                    tele["chosen_move"] = book_move
                    tele.update(book_meta)

                    # Also get base move for comparison
                    try:
                        base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                            self.board, movetime_ms, go_params=go_params
                        )
                        tele["base_move"] = base_move
                        if book_move != base_move:
                            tele["used_targeted_override"] = True
                    except Exception:
                        info_lines = []

                    return book_move, None, "learned_book", True, info_lines, tele

        # 3. At exit position, Black's turn — delegate to engine
        # (Black moves here, not our specialist concern)
        if self.board.turn == chess.BLACK:
            try:
                base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                    self.board, movetime_ms, go_params=go_params
                )
                tele["base_move"] = base_move
                tele["chosen_move"] = base_move
                return base_move, base_ponder, "black_turn_delegate", False, info_lines, tele
            except Exception:
                base_move, _ = self._fallback_move()
                tele["chosen_move"] = base_move
                return base_move, None, "black_turn_fallback", False, [], tele

        # 4. Past exit, White's turn, but no book move — targeted root correction
        if self.board.turn == chess.WHITE and self._targeted_book.is_past_exit(self.board):
            try:
                base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                    self.board, movetime_ms, go_params=go_params
                )
                tele["base_move"] = base_move
                tele["chosen_move"] = base_move

                # Small targeted root correction: check if any learned pattern
                # from nearby positions applies
                history = self._targeted_book.move_history_key(self.board)
                if self._targeted_book.tree.is_in_tree(history):
                    tele["used_targeted_override"] = True
                    tele["candidate_count"] = 1

                return base_move, base_ponder, "root_correction", True, info_lines, tele
            except Exception:
                base_move, _ = self._fallback_move()
                tele["chosen_move"] = base_move
                return base_move, None, "root_correction_fallback", False, [], tele

        # 5. Fallback — delegate to base engine
        try:
            base_move, base_ponder, info_lines = self.engine_pool.bestmove(
                self.board, movetime_ms, go_params=go_params
            )
            tele["base_move"] = base_move
            tele["chosen_move"] = base_move
            return base_move, base_ponder, "outside_line_delegate", False, info_lines, tele
        except Exception:
            base_move, _ = self._fallback_move()
            tele["chosen_move"] = base_move
            return base_move, None, "outside_line_fallback", False, [], tele

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
        # White/Black specialist options
        elif name_lower == "profile":
            self.profile = value
            if value in ("white_killer", "combined") and self._targeted_book is None:
                self._init_white_killer()
            if value in ("black_killer", "combined") and self._black_targeted_book is None:
                self._init_black_killer()
        elif name_lower == "targetenginepath":
            pass  # SF18 path for reference; mining uses it, runtime doesn't
        elif name_lower == "uselearnedbook":
            self._white_killer_opts["use_learned_book"] = value.lower() in ("true", "1", "yes")
        elif name_lower == "bookdepthlimit":
            self._white_killer_opts["book_depth_limit"] = max(1, int(value))
            if self._targeted_book:
                self._targeted_book.book_depth_limit = max(1, int(value))
        elif name_lower == "empiricalweight":
            self._white_killer_opts["empirical_weight"] = float(value)
            if self._targeted_book:
                self._targeted_book.empirical_weight = float(value)
        elif name_lower == "wdlweight":
            self._white_killer_opts["wdl_weight"] = float(value)
            if self._targeted_book:
                self._targeted_book.wdl_weight = float(value)
        elif name_lower == "decisivebonus":
            self._white_killer_opts["decisive_bonus"] = float(value)
            if self._targeted_book:
                self._targeted_book.decisive_bonus = float(value)
        elif name_lower == "replybundlesize":
            self._white_killer_opts["reply_bundle_size"] = max(1, int(value))

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

        # White killer specialist path
        if self.profile in ("white_killer", "combined") and self._targeted_book is not None:
            if self._is_in_vienna_line():
                move, ponder, reason, used, info_lines, tele = self._choose_white_killer_move(go_params)
                # Log extended telemetry
                self._log_white_killer_tele(tele, reason)
                return move, ponder, reason, used, info_lines

        # Black killer specialist path
        if self.profile in ("black_killer", "combined") and self._black_targeted_book is not None:
            if self._is_in_carokann_line():
                move, ponder, reason, used, info_lines, tele = self._choose_black_killer_move(go_params)
                self._log_black_killer_tele(tele, reason)
                return move, ponder, reason, used, info_lines

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

    def _log_white_killer_tele(self, tele: dict[str, Any], reason: str) -> None:
        """Log White killer specialist telemetry."""
        event = {
            "ts": __import__("time").time(),
            "fen": self.board.fen(),
            "profile": "white_killer",
            "reason": reason,
            **tele,
        }
        self.telemetry.log(event)

    def _log_black_killer_tele(self, tele: dict[str, Any], reason: str) -> None:
        """Log Black killer specialist telemetry."""
        event = {
            "ts": __import__("time").time(),
            "fen": self.board.fen(),
            "profile": "black_killer",
            "reason": reason,
            **tele,
        }
        self.telemetry.log(event)

    def loop(self) -> int:
        for raw in self.stdin:
            command = raw.strip()
            if not command:
                continue
            if command == "uci":
                name = f"CounterLine {_VERSION}"
                if self.profile == "white_killer":
                    name += " (White Killer)"
                elif self.profile == "black_killer":
                    name += " (Black Killer)"
                elif self.profile == "combined":
                    name += " (Combined SF18 Killer)"
                self.send(f"id name {name}")
                self.send("id author Stockfish developers + LaunchDay Studio Inc.")
                self.send("")
                self.send("option name BaseEnginePath type string default bin/stockfish-master")
                self.send("option name TargetEnginePath type string default bin/stockfish-sf18")
                self.send("option name NominalEnginePath type string default bin/stockfish-master")
                self.send("option name VerifyEnginePath type string default bin/stockfish-master")
                self.send("option name Profile type combo default default var default var white_killer var black_killer var combined")
                self.send("option name Threads type spin default 1 min 1 max 512")
                self.send("option name Hash type spin default 64 min 1 max 131072")
                self.send("option name OpeningLock type check default true")
                self.send("option name WrapperMode type combo default selective var selective var always var off")
                self.send("option name OpponentProfile type string default stockfish-master")
                self.send("option name EnableWDL type check default false")
                self.send("option name MaxCandidates type spin default 4 min 1 max 16")
                self.send("option name ReplyBundleSize type spin default 2 min 1 max 8")
                self.send("option name NodesMain type spin default 0 min 0 max 100000000")
                self.send("option name NodesChild type spin default 0 min 0 max 100000000")
                self.send("option name NodesVerify type spin default 0 min 0 max 100000000")
                self.send("option name UseLearnedBook type check default true")
                self.send("option name BookDepthLimit type spin default 30 min 1 max 100")
                self.send("option name EmpiricalWeight type string default 0.7")
                self.send("option name WDLWeight type string default 0.3")
                self.send("option name DecisiveBonus type string default 0.1")
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
