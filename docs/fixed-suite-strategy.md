# Fixed-Suite Strategy

CounterLine narrows scope aggressively instead of attempting generic search
improvements across all openings.

## Principles

- Reduce state space by locking play to exact seed lines.
- Reuse repeated nodes through a repertoire database keyed by Polyglot hash.
- Focus on root move correction only; the underlying Stockfish binary still
  performs the heavy tactical search.
- Evaluate against both `sf_18` and the current `master` build.
- Forward all engine info lines (score, depth, PV) for proper GUI/adjudication.
- Preserve hash tables within a game; clear only between games.

## Why This Shape

The two supported families create repeatable middle-game structures:

- QGD Exchange / Carlsbad positions as White
- Petroff Main Line positions as Black

Those structures allow targeted heuristics and persistent move memory without
overstating what the system can do elsewhere.

## Architecture Decisions

### Lightweight Override (current best approach)

The wrapper takes a "do no harm" approach:

1. Let the base engine play with full time control (wtime/btime forwarded)
2. Check repertoire DB for a known-good alternative move
3. Run a quick 10k-node verification probe comparing the two
4. Override only if the repertoire move is within 5cp of the base move

This adds ~20ms overhead per wrapper-active move and ~5ms on passthrough moves.

### Key Findings

- **White QGD**: The repertoire consistently guides CL to `f2f3` (e4-break
  preparation), which creates a slightly more dynamic position than alternatives.
  This accounts for all of CL's decisive wins.
- **Black Petroff**: The repertoire suggests solid but not game-changing moves.
  The main value is avoiding overhead rather than active overrides.
- **Info forwarding**: Forwarding actual engine search info (depth, score, PV)
  was critical for proper time management and GUI interaction.
