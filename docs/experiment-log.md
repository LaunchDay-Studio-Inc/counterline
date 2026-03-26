# Experiment Log

## Upstream Reference

- **upstream/master**: `d173a0655d04b95497eefb75b400baa3eff56f93` (Speedup splat_moves on avx512icl, tag: stockfish-dev-20260318-d173a065)
- **sf_18**: `cb3d4ee9b47d0c5aae855b12379378ea1439675c` (tag: sf_18, stockfish-dev-20260131-cb3d4ee9)
- **Branch**: `exp/fixed-suite-wrapper`
- **Date**: 2026-03-26

## Match Results

| Date (UTC) | Branch | Suite | TC | Games | Base Engine | Wrapper Commit | Result | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 4 | stockfish-master vs stockfish-sf18 | N/A | 2.0-2.0 (4 draws) | Baseline, no wrapper |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 4 | counterline vs stockfish-sf18 | 3e0d0927 | 2.0-2.0 (4 draws) | Wrapper active, FEN positions |
| 2026-03-26 | exp/fixed-suite-wrapper | suite_fixed.epd | 10+0.1 | 4 | counterline vs stockfish-master | 3e0d0927 | 2.0-2.0 (4 draws) | Wrapper active, FEN positions |

## Phase 6 Notes

- All three matches used 1 thread, 64 MB hash, 10+0.1 TC
- Suite has 2 positions: QGD Exchange Carlsbad (white) and Petroff Mainline (black)
- Each match: 2 rounds × 2 games/round with color swap = 4 games
- The "info string with score not found" warnings from fastchess are expected since the CounterLine wrapper does not forward Stockfish info lines
- Fixed critical bug: `_position_cmd()` was always sending `position startpos` even for FEN-initialized boards, causing illegal moves
- All matches resulted in draws, consistent with the positions being well-known theoretical lines at equal evaluation

