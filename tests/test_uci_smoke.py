from __future__ import annotations

from io import StringIO
from pathlib import Path

from wrapper.uci_app import UciApp, load_config


class FakeEnginePool:
    def __init__(self, bestmove: str) -> None:
        self._bestmove = bestmove

    def bestmove(self, board, movetime_ms=None):  # noqa: ANN001, ARG002
        return self._bestmove, None

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
        "position startpos moves d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5 c1g5 c7c6 e2e3 f8e7 f1d3 b8d7 d1c2 e8g8 g1e2 f8e8 e1g1 f6f8\n"
        "go movetime 1\nquit\n"
    )
    stdout = StringIO()

    app = UciApp(config, stdin=stdin, stdout=stdout, engine_pool=FakeEnginePool("a2a3"))
    app.apply_position(
        "position startpos moves d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5 c1g5 c7c6 e2e3 f8e7 f1d3 b8d7 d1c2 e8g8 g1e2 f8e8 e1g1 f6f8"
    )
    app.repertoire.remember(app.board, "qgd_exchange_carlsbad", "f2f3", 18, note="test")
    app.board.reset()
    app.loop()

    assert "bestmove f2f3" in stdout.getvalue()
