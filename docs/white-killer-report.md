# White Killer Specialist — Experiment Report

## Overview

**Branch:** `exp/white-sf18-killer`
**Target:** Beat Stockfish 18 as White from the Vienna Gambit Accepted line (W06)
**Profile:** `white_killer`

### Selected Line

```
1. e4 e5 2. Nc3 Nf6 3. f4 exf4 4. e5 Ng8 5. Nf3 d6 6. d4 dxe5
7. Qe2 Bb4 8. Qxe5+ Qe7 9. Bxf4 Bxc3+ 10. bxc3
```

**Exit EPD:** `rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq -`

---

## Architecture

The specialist consists of three layers:

1. **PolicyTree** (`wrapper/policy_tree.py`) — Compact JSON-based book of
   candidate moves mined by `scripts/mine_white_killer.py`. Stores per-move
   `MoveStats` (master eval, empirical W/D/L, predicted replies) keyed by
   full UCI move history.

2. **EmpiricalDB** (`wrapper/empirical_db.py`) — SQLite database tracking
   playout results for each candidate. Populated during mining.

3. **TargetedBook** (`wrapper/targeted_book.py`) — Integrates the tree and
   DB. Detects whether the current board is within the seed line, at the
   exit position, or past exit. Provides `get_seed_move()` for pre-exit
   positions and `lookup_book_move()` for post-exit specialist moves.

The wrapper (`wrapper/uci_app.py`) activates specialist logic when
`profile == "white_killer"`. Move selection priority:

```
1. In seed line         → return hard-coded seed move (instant)
2. Post-exit, White     → learned book move if available
3. Post-exit, Black     → delegate to Stockfish master
4. Post-exit, no book   → root-corrected engine search
5. Fallback             → standard CounterLine pipeline
```

---

## Book Mining

**Script:** `scripts/mine_white_killer.py`

| Metric | Value |
|---|---|
| Mined nodes | 61 |
| Candidate moves | 244 |
| Total playout visits | 488 |
| Mining depth | Up to 8 half-moves from exit |
| Analysis nodes | 50,000 per MultiPV |
| Playout nodes | 20,000 per mini-duel |

**Best depth-1 move:** `Qxe7+` (e5e7) — master eval +171 cp, 75% empirical.

---

## Match Results

All matches: 1+0.1 time control, 1 thread, concurrency 1, from exit EPD.

### Baselines (20 games per side)

| Engine as White | W | D | L | Score |
|---|---|---|---|---|
| Stockfish master | 18 | 2 | 0 | 95.0% |
| NullWrapper | 19 | 1 | 0 | 97.5% |

### Specialist Matches

| Match | Hash | Rounds | W | D | L | Score | Notes |
|---|---|---|---|---|---|---|---|
| v1 (pre-FEN-fix) | 16 MB | 20 | 20 | 0 | 0 | 100.0% | No overrides fired (bug) |
| v2 (post-FEN-fix) | 16 MB | 20 | 18 | 2 | 0 | 95.0% | Overrides confirmed |
| v3 (final) | 64 MB | 40 | 37 | 3 | 0 | 96.2% | **Proof match** |

> **v1 caveat:** The 100% in v1 was achieved without any specialist overrides
> firing — it was pure Stockfish master play. The FEN-detection bug meant
> `is_past_exit()` never triggered for games started from the exit EPD.

### v3 Telemetry Breakdown (40 rounds, 80 games)

| Event | Count |
|---|---|
| Total telemetry records | 10,388 |
| `learned_book` decisions | 160 |
| `root_correction` decisions | 5,064 |
| `black_turn_delegate` decisions | 5,164 |
| `targeted_override` (book ≠ base) | 2,561 |

### Draw Analysis (v3)

All 3 draws are deep endgame positions, not opening-related:

| Game | Final half-move | Final position |
|---|---|---|
| 1 | 71 (move 37) | R+B vs R+N, complex rook ending |
| 7 | 128 (move 65) | K+B vs K — insufficient material |
| 45 | 165 (move 84) | K+B vs K+N+P — fortress draw |

---

## Configuration

**Best config:** `configs/white-killer.yml`

```yaml
engine:
  base_path: bin/stockfish-master
  target_path: bin/stockfish-sf18
  threads: 1
  hash_mb: 64

wrapper:
  use_learned_book: true
  book_depth_limit: 30
  empirical_weight: 0.7
  wdl_weight: 0.3
  decisive_bonus: 0.1
  reply_bundle_size: 2
```

---

## Entrypoints

### Run the specialist

```bash
bin/counterline-white
```

Or equivalently:

```bash
.venv/bin/python -m wrapper --profile white_killer
```

### Re-run proof match

```bash
./tools/fastchess/fastchess \
  -engine cmd=bin/counterline-white name=CLWhite \
  -engine cmd=bin/stockfish-sf18 name=SF18 \
  -each tc=1+0.1 -each option.Threads=1 -each option.Hash=64 \
  -openings file=opening_suites/final/white/exit.epd format=epd order=sequential \
  -rounds 40 -repeat \
  -pgnout file=results/white_killer/proof_match.pgn \
  -concurrency 1
```

### Re-run baselines

```bash
# Master baseline
./tools/fastchess/fastchess \
  -engine cmd=bin/stockfish-master name=Master \
  -engine cmd=bin/stockfish-sf18 name=SF18 \
  -each tc=1+0.1 -each option.Threads=1 -each option.Hash=16 \
  -openings file=opening_suites/final/white/exit.epd format=epd order=sequential \
  -rounds 20 -repeat \
  -pgnout file=results/white_killer/baseline.pgn \
  -concurrency 1

# NullWrapper baseline
./tools/fastchess/fastchess \
  -engine cmd=bin/counterline-uci name=NullWrapper \
  -engine cmd=bin/stockfish-sf18 name=SF18 \
  -each tc=1+0.1 -each option.Threads=1 -each option.Hash=16 \
  -openings file=opening_suites/final/white/exit.epd format=epd order=sequential \
  -rounds 20 -repeat \
  -pgnout file=results/white_killer/null_wrapper.pgn \
  -concurrency 1
```

### Re-run mining

```bash
.venv/bin/python scripts/mine_white_killer.py
```

### Run tests

```bash
.venv/bin/python -m pytest tests/ -v
```

---

## Files Created / Modified

| File | Action | Purpose |
|---|---|---|
| `wrapper/policy_tree.py` | Created | Compact policy tree with MoveStats |
| `wrapper/empirical_db.py` | Created | SQLite empirical results database |
| `wrapper/targeted_book.py` | Created | Integrates tree + DB, line detection |
| `wrapper/uci_app.py` | Modified | White killer profile, specialist options |
| `wrapper/__main__.py` | Modified | `--profile` flag support |
| `wrapper/__init__.py` | Modified | New module exports |
| `bin/counterline-white` | Created | Launcher script |
| `scripts/mine_white_killer.py` | Created | Book mining pipeline |
| `tests/test_white_killer.py` | Created | 19 specialist tests |
| `configs/white-killer.yml` | Created | Best configuration |
| `data/white_killer/book/vienna.json` | Generated | 61-node policy tree |
| `data/white_killer/db/empirical.sqlite` | Generated | Empirical playout data |

---

## Test Suite

34 tests pass (15 existing + 19 new):

```
tests/test_white_killer.py::TestPrefixDetection (5 tests)
tests/test_white_killer.py::TestLearnedBookLookup (4 tests)
tests/test_white_killer.py::TestFallbackToMaster (1 test)
tests/test_white_killer.py::TestUciWhiteKiller (3 tests)
tests/test_white_killer.py::TestIntegrationFromExit (2 tests)
tests/test_white_killer.py::TestPolicyTree (1 test)
tests/test_white_killer.py::TestEmpiricalDB (2 tests)
tests/test_white_killer.py::TestReplyBundle (1 test)
```

---

## Key Bug Fixes

### FEN-based detection for games starting from EPD

Games started by fastchess from exit EPD have an empty `move_stack`.
The original line-detection logic checked for the seed move prefix in
`board.move_stack`, which always failed. Fix: added `_is_exit_root()`
to check `board.root()` FEN against the exit FEN, and updated
`move_history_key()` to prepend seed UCIs when the root matches exit FEN.

---

## Limitations

- **Endgame draws:** The specialist cannot prevent draws arising from
  insufficient material or repetition at move 60+. These are engine-strength
  issues, not opening preparation gaps.
- **Single line:** The specialist only covers the Vienna Gambit Accepted.
  Other openings fall through to standard CounterLine logic.
- **Time control:** Tested only at 1+0.1. Longer time controls may show
  different results as SF18 gets more time to find defensive resources.
