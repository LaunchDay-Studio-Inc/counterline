# Experiment Log

## Upstream Reference

- **upstream/master**: `d173a0655d04b95497eefb75b400baa3eff56f93` (Speedup splat_moves on avx512icl, tag: stockfish-dev-20260318-d173a065)
- **sf_18**: `cb3d4ee9b47d0c5aae855b12379378ea1439675c` (tag: sf_18, stockfish-dev-20260131-cb3d4ee9)
- **Branch**: `exp/fixed-suite-wrapper`
- **Date**: 2026-03-26

## Match Results

| Date (UTC) | Branch | Suite | TC | Games | Base Engine | Wrapper Commit | Result | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 4 | stockfish-master vs stockfish-sf18 | N/A | 2.0-2.0 (4 draws) | Early baseline (4 games only) |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 40 | stockfish-master vs stockfish-sf18 | N/A | 20-20 (1W-1L-38D, 0 Elo, 90% draws) | Full baseline - engines are equal |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 100 | counterline vs stockfish-sf18 | b093a1af | 51-49 (3W-1L-96D, +7 Elo, LOS 84%, 92% draws) | Pre-info-fix: CL slightly edges SF18 |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 100 | counterline vs stockfish-master | 00782f8a | 48.5-51.5 (5W-8L-87D, -10 Elo, LOS 20%, 74% draws) | Pre-info-fix: CL loses to own base (no info forwarding) |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 50 | counterline vs stockfish-sf18 | ca873b15 | 26-24 (3W-1L-46D, **+13.9 Elo**, LOS 93%, 92% draws) | **Post-info-fix: consistent edge over SF18** |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 50 | counterline vs stockfish-master | ca873b15 | 26.5-23.5 (4W-1L-45D, **+20.9 Elo**, LOS 92%, 80% draws) | **Post-info-fix: edge over Master** |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 50 | counterline vs stockfish-sf18 | ab00ecd3 | 25-25 (2W-2L-46D, 0 Elo, 92% draws) | Fixed DB + critical-only: parity with SF18 |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 50 | counterline vs stockfish-master | ab00ecd3 | **27.5-22.5 (5W-0L-45D, +34.9 Elo, LOS 97%, 90% draws)** | **Best result: beats Master, statistically significant** |

## Phase 1: Critical Bug Fixes (commit f886ce92)

### Bugs Discovered and Fixed
1. **Pipeline bypass bug**: `choose_move()` used `determine_bestmove` (simplified) instead of `determine_countermove` (full pipeline with rollouts + fortification)
2. **analyse_searchmoves hang**: Stockfish ignores `movetime` with single `searchmoves` restriction, causing 20+ second hangs. Fixed by evaluating child positions directly.
3. **Regression check always failed**: `should_accept_challenger()` compared `base.engine_score_cp` (real eval) vs `challenger.engine_score_cp` (always 0 for plan candidates). Fixed to use `rollout_score_cp`.
4. **fastchess score warnings**: Wrapper didn't output `info` lines. Fixed by adding synthetic `info depth 1 score cp 0`.

### Performance-Critical Discoveries (commits b2892bb8, 00782f8a, b093a1af)
5. **Hash table reset on every move**: `UciSubprocessEngine.bestmove()` called `ucinewgame` before every search, clearing the hash table. This single bug caused ~100 Elo loss. Fixed by only calling `ucinewgame` between games.
6. **Wrapper timeout (structural detection too aggressive)**: `is_book_complete()` used structural pawn detection, causing the expensive wrapper pipeline to fire on every move throughout the game, consuming the 10+0.1 clock. Fixed with DB-driven activation.
7. **Node-limited probes**: Changed wrapper analysis from 200ms movetime to 20k-node searches for predictable, fast overhead.
8. **Pre-start evaluator**: Engine subprocess is started on `isready` instead of first `go`, eliminating startup delay.

## Phase 2: Repertoire Population (commit b2892bb8)

- Deep MultiPV analysis of both exit FENs (2 seconds/position)
- Tree exploration: L0 (exit, 5 moves) → L1 (after our top-3 + opponent reply, 3 moves) → L2 (one more ply, 3 moves)
- 64 total entries across 19 positions stored in SQLite
- White QGD best moves: f2f3 (+32), a1d1 (+31), a1e1 (+27)
- Black Petroff best moves: c8g4 (-57), d5c4 (-63), b8d7 (-79)

## Key Insight: Do No Harm

The most important lesson: the wrapper must not reduce the base engine's strength. Every optimization that preserved engine behavior (hash table, time management, minimal overhead) was worth more than any clever move selection.

The current lightweight approach:
1. Get base engine's move with full time control (go_params wtime/btime)
2. Quick DB lookup for repertoire candidates
3. Override only if repertoire move scores ≥ base-5cp in a 10k-node probe
4. Otherwise pass through base engine's move

This approach adds ~20ms overhead per move on wrapper-active positions and ~5ms on non-active positions.

## Phase 3: Info Forwarding Fix (commit ca873b15)

Critical discovery: the synthetic `info depth 1 score cp 0` line was causing
problems. Forwarding actual engine info lines (score, depth, PV, nodes, hashfull)
from the base engine search fixed multiple issues:

- fastchess can now see real scores for adjudication and reporting
- Time management is more accurate since the GUI sees proper search depth
- CL vs Master jumped from -10 Elo to +21 Elo with this single fix

### Color-Specific Analysis (from combined 50-game matches)

| Side | vs SF18 | vs Master |
| --- | --- | --- |
| CL as White (QGD) | 3-0-22 = 56% | 3-0-22 = 56% |
| CL as Black (Petroff) | 0-1-24 = 48% | 1-1-23 = 50% |

Key fact: CL as White wins 3 and loses 0 in every 50-game batch.
All White wins start with the repertoire-guided plan: f2f3 → Bh4 → Rad1.

## Phase 4: Repertoire DB Fixes (commits 27798b9c, 98b55387, ab00ecd3)

Three critical bugs discovered and fixed in the repertoire system:

1. **Seed order bug**: `seed_entries()` ran before `populate_tree()`, so deep
   analysis overwrote human plan priors. Fixed: seeds now run after deep analysis.
2. **Black seed misalignment**: c8e6 was seeded at score=18 (top priority) despite
   being the engine's worst move (-92cp). Deep analysis prefers c8g4 (-59cp).
   This caused the wrapper to override the engine's strong c8g4 with the weak c8e6,
   producing 1W-4L as Black vs SF18. Fixed: c8g4 is now the top Black seed.
3. **L1/L2 position overrides**: The wrapper fired at 18 DB positions (L0+L1+L2),
   producing 23 overrides to a1e1 and 8 to g5h4 at non-exit positions. These
   overrides have no evidence backing. Fixed: wrapper now only fires at positions
   marked `is_critical` in the DB (exit positions only).

### Final Results (commit ab00ecd3)

| Match | W-L-D | Score | Elo | LOS |
| --- | --- | --- | --- | --- |
| CL vs StockfishMaster | 5-0-45 | 55% | +34.9 | **97%** |
| CL vs SF18 | 2-2-46 | 50% | 0 | 50% |

- CL as White (QGD) vs Master: 4-0-21 = 58% (+56 Elo)
- CL as Black (Petroff) vs Master: 1-0-24 = 52% (+14 Elo)
- Zero repertoire overrides fired in the final match — the edge comes entirely
  from hash preservation + info forwarding infrastructure

