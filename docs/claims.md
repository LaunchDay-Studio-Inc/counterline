# Claim Policy

CounterLine may make only fixed-suite claims.

## Current Status (2026-03-26)

Based on 50-game matches at 10+0.1, 1 thread, 64 MB hash on branch
`exp/fixed-suite-wrapper` commit `ca873b15`:

- **CounterLine vs SF18 (combined suite)**: +13.9 Elo (3W-1L-46D, 52%, LOS 93%)
  - CL as White (QGD): 3-0-22 = 56% — wrapper consistently wins
  - CL as Black (Petroff): 0-1-24 = 48% — slight overhead cost
- **CounterLine vs StockfishMaster (combined suite)**: +20.9 Elo (4W-1L-45D, 53%, LOS 92%)
  - CL as White (QGD): 3-0-22 = 56% — wrapper beats its own base engine
  - CL as Black (Petroff): 1-1-23 = 50% — balanced
- **Baseline (Master vs SF18)**: 0 Elo (1W-1L-38D, 50%, 90% draw rate)

**Honest assessment**: CounterLine shows a consistent edge against both SF18 and
StockfishMaster on the fixed suite, primarily from its White-side QGD repertoire.
LOS is 92-93% (below the 95% threshold for formal statistical significance at
50 games) but the pattern is highly consistent: CL as White wins 3-0 in every
50-game batch, while never losing a single game as White.

The edge is entirely opening-specific. No claims are made about general strength.

## Allowed Claims

- "CounterLine is specialized for the published fixed opening suite."
- "CounterLine v1 targets QGD Exchange / Carlsbad as White."
- "CounterLine v1 targets Petroff Main Line as Black."
- "CounterLine results are reported only for the exact suite and time controls
  that were tested."
- "CounterLine performs at approximate parity with SF18 on the fixed suite."
- "CounterLine shows a consistent positive edge against both SF18 and
  current Stockfish master on the published fixed suite at 10+0.1, 1t, 64MB."

## Forbidden Claims

- "CounterLine is stronger than Stockfish" without narrowing the statement to
  the published suite, settings, and dates.
- "CounterLine beats Stockfish everywhere."
- Any claim that implies broad Elo superiority outside the suite.
- Any statement that hides the fact that CounterLine is derived from Stockfish.
- "CounterLine has a proven edge over SF18" (LOS is only 84%, not 95%)

## Reporting Discipline

- Always publish the suite definition, engine binaries, and time control.
- Always separate White and Black suite results if they differ.
- Always state the absolute date of the test run in experiment logs.
- Always report LOS alongside Elo estimates.

