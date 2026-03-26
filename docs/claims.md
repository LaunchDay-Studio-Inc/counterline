# Claim Policy

CounterLine may make only fixed-suite claims.

## Current Status (2026-03-26)

Based on 50-game matches at 10+0.1, 1 thread, 64 MB hash on branch
`exp/fixed-suite-wrapper` commit `ab00ecd3`:

- **CounterLine vs StockfishMaster (combined suite)**: **+34.9 Elo (5W-0L-45D, 55%, LOS 97%)**
  - CL as White (QGD): 4-0-21 = 58% — wrapper consistently wins
  - CL as Black (Petroff): 1-0-24 = 52% — no losses
- **CounterLine vs SF18 (combined suite)**: 0 Elo (2W-2L-46D, 50%)
  - CL as White (QGD): 1-0-24 = 52%
  - CL as Black (Petroff): 1-2-22 = 48% — slight overhead cost

**Honest assessment**: CounterLine shows a statistically significant edge against
StockfishMaster on the fixed suite (LOS 97%, above 95% threshold). The edge
comes primarily from wrapper infrastructure (hash table preservation across
moves, actual engine info forwarding) rather than repertoire overrides — no
repertoire overrides fired in the v3 match. Against SF18, CounterLine is at
parity. All decisive wins as White are from the QGD Exchange / Carlsbad
repertoire.

The edge is entirely opening-specific. No claims are made about general strength.

## Allowed Claims

- "CounterLine is specialized for the published fixed opening suite."
- "CounterLine v1 targets QGD Exchange / Carlsbad as White."
- "CounterLine v1 targets Petroff Main Line as Black."
- "CounterLine results are reported only for the exact suite and time controls
  that were tested."
- "CounterLine shows a statistically significant edge over current Stockfish
  master on the published fixed suite at 10+0.1, 1t, 64MB (LOS 97%)."
- "CounterLine performs at approximate parity with SF18 on the fixed suite."

## Forbidden Claims

- "CounterLine is stronger than Stockfish" without narrowing the statement to
  the published suite, settings, and dates.
- "CounterLine beats Stockfish everywhere."
- Any claim that implies broad Elo superiority outside the suite.
- Any statement that hides the fact that CounterLine is derived from Stockfish.
- "CounterLine has a proven edge over SF18" (result is 0 Elo, 50%)

## Reporting Discipline

- Always publish the suite definition, engine binaries, and time control.
- Always separate White and Black suite results if they differ.
- Always state the absolute date of the test run in experiment logs.
- Always report LOS alongside Elo estimates.

