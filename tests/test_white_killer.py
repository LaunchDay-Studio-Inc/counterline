"""Tests for the White killer specialist (Vienna Gambit)."""

from __future__ import annotations

import json
from io import StringIO
from pathlib import Path

import chess
import pytest

from wrapper.policy_tree import MoveStats, PolicyNode, PolicyTree
from wrapper.targeted_book import VIENNA_EXIT_FEN, VIENNA_SEED_UCIS, TargetedBook
from wrapper.uci_app import UciApp, load_config


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

class FakeEnginePool:
    """Minimal engine pool stub for testing."""

    def __init__(self, bestmove: str = "e5e7") -> None:
        self._bestmove = bestmove

    def bestmove(self, board, movetime_ms=None, go_params=None):  # noqa: ANN001, ARG002
        return self._bestmove, None, []

    def analyse_root(self, board, **kwargs):  # noqa: ANN001, ARG002
        from wrapper.types import NodeScore
        return NodeScore(score_cp=150)

    def analyse_searchmoves(self, board, moves, **kwargs):  # noqa: ANN001, ARG002
        from wrapper.types import NodeScore
        return {m: NodeScore(score_cp=0) for m in moves}

    def predict_reply(self, board, **kwargs):  # noqa: ANN001, ARG002
        move = next(iter(board.legal_moves), chess.Move.null())
        from wrapper.types import NodeScore
        return move.uci(), NodeScore(score_cp=0)

    def verify_duel(self, board, challenger, base, **kwargs):  # noqa: ANN001, ARG002
        from wrapper.types import NodeScore
        return NodeScore(score_cp=0), NodeScore(score_cp=0)

    def close(self) -> None:
        return None


@pytest.fixture
def tmp_book(tmp_path: Path) -> Path:
    """Create a minimal book file for testing."""
    tree = PolicyTree()
    tree.exit_fen = VIENNA_EXIT_FEN
    tree.exit_moves = " ".join(VIENNA_SEED_UCIS)

    # Add a node at depth=1 (after Black plays Nc6 at exit)
    node = PolicyNode(
        fen="r1b1k1nr/ppp1qppp/2n5/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R w KQkq - 1 11",
        move_history=" ".join(VIENNA_SEED_UCIS) + " b8c6",
        depth_from_exit=1,
        candidate_moves={
            "e5e7": MoveStats(
                master_eval_cp=171,
                empirical_score=75.0,
                wdl_score=85.0,
                decisive_rate=0.5,
                visits=4,
                wins=3,
                draws=1,
                losses=0,
            ),
            "f1b5": MoveStats(
                master_eval_cp=155,
                empirical_score=50.0,
                wdl_score=80.0,
                visits=2,
                wins=1,
                draws=1,
                losses=0,
            ),
        },
    )
    tree.add_node(node)

    path = tmp_path / "vienna.json"
    tree.save(path)
    return path


@pytest.fixture
def tmp_db(tmp_path: Path) -> Path:
    return tmp_path / "empirical.sqlite"


def _make_white_killer_app(
    tmp_path: Path,
    tmp_book: Path,
    tmp_db: Path,
    bestmove: str = "e5e7",
) -> tuple[UciApp, StringIO]:
    config = load_config(None)
    config.wrapper.db_path = tmp_path / "counterline.sqlite"
    config.wrapper.telemetry_path = tmp_path / "telemetry.jsonl"
    stdout = StringIO()
    app = UciApp(
        config,
        stdin=StringIO(),
        stdout=stdout,
        engine_pool=FakeEnginePool(bestmove),
        profile="white_killer",
    )
    # Override the book with our test fixture
    app._targeted_book = TargetedBook(
        book_path=tmp_book,
        db_path=tmp_db,
    )
    return app, stdout


# ---------------------------------------------------------------------------
# Test: Exact prefix detection
# ---------------------------------------------------------------------------

class TestPrefixDetection:
    """Verify that the Vienna Gambit seed line is correctly detected."""

    def test_startpos_is_in_seed(self) -> None:
        board = chess.Board()
        board.push_uci("e2e4")
        book = TargetedBook.__new__(TargetedBook)
        book.tree = PolicyTree()
        book.tree.exit_moves = " ".join(VIENNA_SEED_UCIS)
        assert book.is_in_seed_line(board)

    def test_full_seed_line(self) -> None:
        board = chess.Board()
        for uci in VIENNA_SEED_UCIS:
            board.push_uci(uci)
        book = TargetedBook.__new__(TargetedBook)
        book.tree = PolicyTree()
        book.tree.exit_moves = " ".join(VIENNA_SEED_UCIS)
        assert book.is_in_seed_line(board)

    def test_wrong_prefix_rejected(self) -> None:
        board = chess.Board()
        board.push_uci("d2d4")  # Not Vienna
        book = TargetedBook.__new__(TargetedBook)
        book.tree = PolicyTree()
        book.tree.exit_moves = " ".join(VIENNA_SEED_UCIS)
        assert not book.is_in_seed_line(board)

    def test_past_exit_detected(self) -> None:
        board = chess.Board()
        for uci in VIENNA_SEED_UCIS:
            board.push_uci(uci)
        board.push_uci("b8c6")  # One move past exit
        book = TargetedBook.__new__(TargetedBook)
        book.tree = PolicyTree()
        book.tree.exit_moves = " ".join(VIENNA_SEED_UCIS)
        assert book.is_past_exit(board)

    def test_at_exit_detected(self) -> None:
        board = chess.Board()
        for uci in VIENNA_SEED_UCIS:
            board.push_uci(uci)
        book = TargetedBook.__new__(TargetedBook)
        book.tree = PolicyTree()
        book.tree.exit_moves = " ".join(VIENNA_SEED_UCIS)
        assert book.is_at_exit(board)


# ---------------------------------------------------------------------------
# Test: Learned book lookup
# ---------------------------------------------------------------------------

class TestLearnedBookLookup:
    """Test book moves are returned correctly."""

    def test_seed_move_returned(self, tmp_book: Path, tmp_db: Path) -> None:
        board = chess.Board()
        book = TargetedBook(book_path=tmp_book, db_path=tmp_db)
        move, meta = book.lookup_book_move(board)
        assert move == "e2e4"
        assert meta["source"] == "seed_line"
        book.close()

    def test_learned_move_at_depth1(self, tmp_book: Path, tmp_db: Path) -> None:
        board = chess.Board()
        for uci in VIENNA_SEED_UCIS:
            board.push_uci(uci)
        board.push_uci("b8c6")  # Black plays Nc6
        # Now it's White's turn — should get book move
        book = TargetedBook(book_path=tmp_book, db_path=tmp_db)
        move, meta = book.lookup_book_move(board)
        assert move == "e5e7"  # Best learned move
        assert meta["source"] == "learned_book"
        assert meta["visits"] == 4
        book.close()

    def test_no_book_for_black_turn(self, tmp_book: Path, tmp_db: Path) -> None:
        board = chess.Board()
        for uci in VIENNA_SEED_UCIS:
            board.push_uci(uci)
        # At exit, Black to move — no book move
        book = TargetedBook(book_path=tmp_book, db_path=tmp_db)
        move, meta = book.lookup_book_move(board)
        assert move is None
        book.close()

    def test_outside_line_no_book(self, tmp_book: Path, tmp_db: Path) -> None:
        board = chess.Board()
        board.push_uci("d2d4")  # Not Vienna
        book = TargetedBook(book_path=tmp_book, db_path=tmp_db)
        move, meta = book.lookup_book_move(board)
        # d2d4 is not in the seed line, so no seed move
        assert move is None
        book.close()


# ---------------------------------------------------------------------------
# Test: White-side fallback to master
# ---------------------------------------------------------------------------

class TestFallbackToMaster:
    """Verify fallback to base engine outside the learned line."""

    def test_outside_line_delegates(self, tmp_path: Path, tmp_book: Path, tmp_db: Path) -> None:
        app, stdout = _make_white_killer_app(tmp_path, tmp_book, tmp_db, bestmove="g1f3")
        app.config.wrapper.opening_lock = False  # Disable QGD seed line
        app.board = chess.Board()
        app.board.push_uci("d2d4")
        app.board.push_uci("d7d5")
        app.board.push_uci("g1f3")  # Not Vienna line
        # This is not the Vienna line, should delegate to engine
        move, ponder, reason, used, info, *_ = app.choose_move({})
        assert move == "g1f3"  # From FakeEnginePool
        assert not used or reason == "outside_suite"


# ---------------------------------------------------------------------------
# Test: UCI smoke with White killer profile
# ---------------------------------------------------------------------------

class TestUciWhiteKiller:
    """UCI handshake and go commands with white_killer profile."""

    def test_handshake(self, tmp_path: Path, tmp_book: Path, tmp_db: Path) -> None:
        app, stdout = _make_white_killer_app(tmp_path, tmp_book, tmp_db)
        stdin = StringIO("uci\nisready\nquit\n")
        app.stdin = stdin
        app.loop()
        output = stdout.getvalue()
        assert "White Killer" in output
        assert "uciok" in output
        assert "readyok" in output
        assert "UseLearnedBook" in output
        assert "Profile" in output

    def test_seed_line_move(self, tmp_path: Path, tmp_book: Path, tmp_db: Path) -> None:
        app, stdout = _make_white_killer_app(tmp_path, tmp_book, tmp_db)
        stdin = StringIO("uci\nisready\nposition startpos\ngo movetime 100\nquit\n")
        app.stdin = stdin
        app.loop()
        output = stdout.getvalue()
        assert "bestmove e2e4" in output
        assert "white_seed_line" in output

    def test_learned_book_move(self, tmp_path: Path, tmp_book: Path, tmp_db: Path) -> None:
        moves = " ".join(VIENNA_SEED_UCIS) + " b8c6"
        app, stdout = _make_white_killer_app(tmp_path, tmp_book, tmp_db)
        stdin = StringIO(f"uci\nisready\nposition startpos moves {moves}\ngo movetime 100\nquit\n")
        app.stdin = stdin
        app.loop()
        output = stdout.getvalue()
        assert "bestmove e5e7" in output
        assert "learned_book" in output


# ---------------------------------------------------------------------------
# Test: Integration from White exit position
# ---------------------------------------------------------------------------

class TestIntegrationFromExit:
    """Full integration test from the Vienna exit position."""

    def test_black_to_move_at_exit(self, tmp_path: Path, tmp_book: Path, tmp_db: Path) -> None:
        """At exit, BTM — should delegate to engine."""
        moves = " ".join(VIENNA_SEED_UCIS)
        app, stdout = _make_white_killer_app(tmp_path, tmp_book, tmp_db, bestmove="b8c6")
        stdin = StringIO(f"uci\nisready\nposition startpos moves {moves}\ngo movetime 100\nquit\n")
        app.stdin = stdin
        app.loop()
        output = stdout.getvalue()
        # Black to move at exit — delegate, then after Black plays and White gets turn,
        # should use book
        assert "bestmove" in output

    def test_white_after_queen_exchange(self, tmp_path: Path, tmp_book: Path, tmp_db: Path) -> None:
        """After ...Nc6, White should play Qxe7+ (learned book)."""
        moves = " ".join(VIENNA_SEED_UCIS) + " b8c6"
        app, stdout = _make_white_killer_app(tmp_path, tmp_book, tmp_db)
        stdin = StringIO(f"uci\nisready\nposition startpos moves {moves}\ngo movetime 100\nquit\n")
        app.stdin = stdin
        app.loop()
        output = stdout.getvalue()
        assert "bestmove e5e7" in output


# ---------------------------------------------------------------------------
# Test: Policy tree serialization
# ---------------------------------------------------------------------------

class TestPolicyTree:
    """Test tree save/load roundtrip."""

    def test_roundtrip(self, tmp_path: Path) -> None:
        tree = PolicyTree()
        tree.exit_fen = VIENNA_EXIT_FEN
        tree.exit_moves = "e2e4 e7e5"
        node = PolicyNode(
            fen="test_fen",
            move_history="e2e4 e7e5",
            depth_from_exit=0,
            candidate_moves={
                "b1c3": MoveStats(master_eval_cp=50, visits=3, wins=2, draws=1),
            },
        )
        tree.add_node(node)

        path = tmp_path / "tree.json"
        tree.save(path)
        loaded = PolicyTree.load(path)

        assert len(loaded) == 1
        assert loaded.exit_fen == VIENNA_EXIT_FEN
        n = loaded.get_node("e2e4 e7e5")
        assert n is not None
        assert "b1c3" in n.candidate_moves
        assert n.candidate_moves["b1c3"].wins == 2


# ---------------------------------------------------------------------------
# Test: Empirical DB
# ---------------------------------------------------------------------------

class TestEmpiricalDB:
    """Test the empirical database."""

    def test_record_and_retrieve(self, tmp_path: Path) -> None:
        from wrapper.empirical_db import EmpiricalDB
        db = EmpiricalDB(tmp_path / "test.sqlite")
        db.initialize()
        db.record_result("e2e4 e7e5", "b1c3", "test_fen", "1-0", eval_cp=150)
        db.record_result("e2e4 e7e5", "b1c3", "test_fen", "1/2-1/2", eval_cp=50)

        rec = db.get_record("e2e4 e7e5", "b1c3")
        assert rec is not None
        assert rec.wins == 1
        assert rec.draws == 1
        assert rec.losses == 0
        assert rec.score_pct == 75.0
        db.close()

    def test_get_all_for_position(self, tmp_path: Path) -> None:
        from wrapper.empirical_db import EmpiricalDB
        db = EmpiricalDB(tmp_path / "test.sqlite")
        db.initialize()
        db.record_result("h1", "m1", "f1", "1-0")
        db.record_result("h1", "m2", "f1", "0-1")
        records = db.get_all_for_position("h1")
        assert len(records) == 2
        db.close()


# ---------------------------------------------------------------------------
# Test: Reply bundle logic (targeted book structure)
# ---------------------------------------------------------------------------

class TestReplyBundle:
    """Test that predicted replies are stored and accessible."""

    def test_predicted_replies_stored(self, tmp_book: Path) -> None:
        tree = PolicyTree.load(tmp_book)
        # Get the depth-1 node
        for node in tree.nodes.values():
            if node.depth_from_exit == 1:
                stats = node.candidate_moves.get("e5e7")
                assert stats is not None
                # Check that predicted_replies can be empty (fixture may not have them)
                # but the field exists
                assert hasattr(stats, "predicted_replies")
                break
