# Claim Policy

CounterLine may make only fixed-suite claims.

## Current Status (2026-03-26)

Based on 100-game matches at 10+0.1, 1 thread, 64 MB hash:

- **CounterLine vs SF18**: +7 Elo (3W-1L-96D, 51%, LOS 84%, 92% draw rate)
  - Positive but not yet statistically significant (LOS < 95%)
- **CounterLine vs StockfishMaster**: Testing in progress; wrapper adds ~10 Elo overhead
- **Baseline (Master vs SF18)**: 0 Elo (1W-1L-38D, 50%, 90% draw rate)

**Honest assessment**: CounterLine performs at rough parity with both SF18 and
StockfishMaster on this suite. The wrapper's primary achievement is avoiding
self-harm (from earlier bugs that caused -98 to -215 Elo penalties).

## Allowed Claims

- "CounterLine is specialized for the published fixed opening suite."
- "CounterLine v1 targets QGD Exchange / Carlsbad as White."
- "CounterLine v1 targets Petroff Main Line as Black."
- "CounterLine results are reported only for the exact suite and time controls
  that were tested."
- "CounterLine performs at approximate parity with SF18 on the fixed suite."

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

