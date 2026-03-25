# Fixed-Suite Strategy

CounterLine narrows scope aggressively instead of attempting generic search
improvements across all openings.

## Principles

- Reduce state space by locking play to exact seed lines.
- Reuse repeated nodes through a repertoire database keyed by Polyglot hash.
- Focus on root move correction only; the underlying Stockfish binary still
  performs the heavy tactical search.
- Evaluate against both `sf_18` and the current `master` build.

## Why This Shape

The two supported families create repeatable middle-game structures:

- QGD Exchange / Carlsbad positions as White
- Petroff Main Line positions as Black

Those structures allow targeted heuristics and persistent move memory without
overstating what the system can do elsewhere.

