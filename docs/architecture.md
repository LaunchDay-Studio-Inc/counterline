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
   and whether the book has exited at the exact fixed-suite FEN.
2. `repertoire.py` and `repertoire_db.py`
   Load and persist position memory keyed by Polyglot hash so repeated suite
   nodes can reuse prior good moves.
3. `rollout.py`
   Generates a narrow candidate set rather than searching arbitrary positions.
4. `plan_score.py`
   Adds structure-specific heuristics for Carlsbad and Petroff positions.
5. `determine.py` and `fortify.py`
   Compare the challenger against the base Stockfish move and reject fragile
   overrides.
6. `telemetry.py`
   Emits JSONL records for every decision.
7. `uci_app.py`
   Speaks UCI, tracks the live board state, and surfaces the final `bestmove`.

## Subprocess Model

`engine_pool.py` manages Stockfish subprocesses through a minimal UCI client.
The wrapper currently uses:

- `bin/stockfish-master` as the base engine
- `bin/stockfish-sf_18` as an optional fallback baseline

This keeps all evaluation authority inside Stockfish binaries while the wrapper
adds fixed-suite selection logic around them.

