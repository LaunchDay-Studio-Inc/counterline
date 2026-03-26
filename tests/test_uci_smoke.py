from __future__ import annotations

from io import StringIO
from pathlib import Path

from wrapper.uci_app import UciApp, load_config


class FakeEnginePool:
    def __init__(self, bestmove: str) -> None:
        self._bestmove = bestmove

    def bestmove(self, board, movetime_ms=None, go_params=None):  # noqa: ANN001, ARG002
        return self._bestmove, None, []

    def analyse_root(self, board, **kwargs):  # noqa: ANN001, ARG002
        from wrapper.types import NodeScore
        return NodeScore(score_cp=0)

    def analyse_searchmoves(self, board, moves, **kwargs):  # noqa: ANN001, ARG002
        from wrapper.types import NodeScore
        return {m: NodeScore(score_cp=0) for m in moves}

    def predict_reply(self, board, **kwargs):  # noqa: ANN001, ARG002
        import chess
        from wrapper.types import NodeScore
        move = next(iter(board.legal_moves), chess.Move.null())
        return move.uci(), NodeScore(score_cp=0)

    def verify_duel(self, board, challenger, base, **kwargs):  # noqa: ANN001, ARG002
        from wrapper.types import NodeScore
        return NodeScore(score_cp=0), NodeScore(score_cp=0)

    def close(self) -> None:
        return None


def test_uci_handshake_and_go(tmp_path: Path) -> None:
    config = load_config(None)
    config.wrapper.db_path = tmp_path / "counterline.sqlite"
    config.wrapper.telemetry_path = tmp_path / "telemetry.jsonl"
    stdin = StringIO("uci\nisready\nposition startpos\ngo movetime 1\nquit\n")
    stdout = StringIO()

    app = UciApp(config, stdin=stdin, stdout=stdout, engine_pool=FakeEnginePool("e2e4"))
    exit_code = app.loop()
    output = stdout.getvalue()

    assert exit_code == 0
    assert "uciok" in output
    assert "readyok" in output
    assert "bestmove e2e4" in output


def test_uci_wrapper_override_on_white_exit(tmp_path: Path) -> None:
    config = load_config(None)
    config.wrapper.db_path = tmp_path / "counterline.sqlite"
    config.wrapper.telemetry_path = tmp_path / "telemetry.jsonl"
    stdin = StringIO(
        "position startpos moves d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5 c1g5 c7c6 e2e3 f8e7 f1d3 b8d7 d1c2 e8g8 g1e2 f8e8 e1g1 d7f8\n"
        "go movetime 1\nquit\n"
    )
    stdout = StringIO()

    app = UciApp(config, stdin=stdin, stdout=stdout, engine_pool=FakeEnginePool("a2a3"))
    app.apply_position(
        "position startpos moves d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5 c1g5 c7c6 e2e3 f8e7 f1d3 b8d7 d1c2 e8g8 g1e2 f8e8 e1g1 d7f8"
    )
    app.repertoire.remember(app.board, "qgd_exchange_carlsbad", "f2f3", 18, note="test")
    app.board.reset()
    app.loop()

    assert "bestmove f2f3" in stdout.getvalue()


def test_uci_options_listed(tmp_path: Path) -> None:
    config = load_config(None)
    config.wrapper.db_path = tmp_path / "counterline.sqlite"
    config.wrapper.telemetry_path = tmp_path / "telemetry.jsonl"
    stdin = StringIO("uci\nquit\n")
    stdout = StringIO()

    app = UciApp(config, stdin=stdin, stdout=stdout, engine_pool=FakeEnginePool("e2e4"))
    app.loop()
    output = stdout.getvalue()

    assert "CounterLine" in output
    assert "BaseEnginePath" in output
    assert "NominalEnginePath" in output
    assert "VerifyEnginePath" in output
    assert "Threads" in output
    assert "Hash" in output
    assert "OpeningLock" in output
    assert "WrapperMode" in output
    assert "MaxCandidates" in output
    assert "NodesMain" in output
    assert "NodesChild" in output
    assert "NodesVerify" in output
    assert "LogPath" in output


def test_uci_seed_line_plays_immediately(tmp_path: Path) -> None:
    """When inside seed line prefix, wrapper should play the seed move immediately."""
    config = load_config(None)
    config.wrapper.db_path = tmp_path / "counterline.sqlite"
    config.wrapper.telemetry_path = tmp_path / "telemetry.jsonl"
    # Position after 1.d4 d5 - should play c2c4 immediately
    stdin = StringIO("position startpos moves d2d4 d7d5\ngo movetime 1\nquit\n")
    stdout = StringIO()

    app = UciApp(config, stdin=stdin, stdout=stdout, engine_pool=FakeEnginePool("e2e4"))
    app.loop()
    output = stdout.getvalue()

    assert "bestmove c2c4" in output
