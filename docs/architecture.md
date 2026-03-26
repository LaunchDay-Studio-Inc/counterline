# CounterLine Architecture

## Core Boundaries

- `src/` remains the upstream Stockfish core and is not modified by the wrapper.
- `wrapper/` is a Python UCI frontend that decides when to trust Stockfish
  directly and when to apply fixed-suite corrections.
- `configs/` stores engine paths, thresholds, and per-opening suite settings.
- `opening_suites/` stores the seed lines and EPD exits that define claim scope.
- `scripts/` provides repeatable build, smoke, and match entrypoints.

## Wrapper Flow

1. `opening_lock.py`
   Detects whether the current game is still on one of the supported seed lines
   and whether the book has exited at the exact fixed-suite FEN. Also provides
   structural detection (pawn structure matching) for family identification
   in post-exit positions.
2. `repertoire.py` and `repertoire_db.py`
   Load and persist position memory keyed by Polyglot hash. The DB stores
   nodes (positions), move_priors (pre-computed evaluations), match_results,
   and overrides. Populated by `scripts/populate_repertoire_db.py` which runs
   deep MultiPV analysis from exit FENs building a 3-level tree.
3. `rollout.py`
   Generates a narrow candidate set rather than searching arbitrary positions.
   Includes half_step_rollout, one_step_rollout, and reply_bundle_rollout
   for candidate evaluation. Used by the full pipeline but NOT the lightweight
   override path (see Performance Notes below).
4. `plan_score.py`
   Adds structure-specific heuristics for Carlsbad and Petroff positions.
5. `determine.py` and `fortify.py`
   Compare the challenger against the base Stockfish move and reject fragile
   overrides. `determine_countermove` implements the full pipeline with
   node-limited probes (20k nodes default).
6. `telemetry.py`
   Emits JSONL records for every decision.
7. `uci_app.py`
   Speaks UCI, tracks the live board state, and surfaces the final `bestmove`.
   Implements the lightweight override path (see below).

## Performance-Critical Design Decisions

### Hash Table Preservation
The evaluator subprocess does NOT call `ucinewgame` between moves within a
game. This preserves the transposition table, which is worth ~100 Elo.
`ucinewgame` is only forwarded when the GUI/match runner sends it between games.

### Lightweight Override Path
The production wrapper does NOT use the full `determine_countermove` pipeline
during games (too expensive at 10+0.1 TC). Instead, `choose_move` uses:
1. Get base move from evaluator with proper time control (wtime/btime)
2. Quick SQLite lookup for repertoire candidates at this position
3. If top repertoire move differs from base AND has prior ≥ 3:
   - Run 10k-node verification probe on both moves
   - Override only if repertoire move scores ≥ (base - 5cp)
4. Otherwise pass through base engine's move

This adds ~5-20ms overhead vs ~3000ms for the full pipeline.

### Activation Guards
The wrapper only fires when ALL conditions are met:
- Position is in a recognized opening family (structural or exact FEN)
- It's the wrapper's side to move (White for QGD, Black for Petroff)
- Remaining time > 5 seconds
- Position has either: exact exit FEN match, OR repertoire DB entries

## Subprocess Model

`engine_pool.py` manages Stockfish subprocesses through a minimal UCI client.
The wrapper currently uses:

- `bin/stockfish-master` as the base engine
- `bin/stockfish-sf_18` as an optional fallback baseline

This keeps all evaluation authority inside Stockfish binaries while the wrapper
adds fixed-suite selection logic around them.

