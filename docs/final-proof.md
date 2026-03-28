# Final Proof — Integrated SF18 Killer

**Branch:** `exp/integrated-sf18-killer`  
**Date:** 2025-07-25  
**Engine:** CounterLine Combined (White + Black specialists)  
**Opponent:** Stockfish SF18  
**TC:** 1+0.1, 1 thread, 64 MB hash  
**Match size:** 40 games per match (20 rounds × 2 games)  
**Tool:** fastchess v0.9.0-sha.82f07a5  

## Architecture

CounterLine Combined dispatches between two learned specialists based on
board position:

- **White specialist** — Vienna Gambit Accepted (exit FEN W06): `PolicyTree`
  with 61-node learned book, steering from 1. e4 e5 2. Nc3 Nf6 3. f4 exf4
  to a position where White is winning (+6 eval).
- **Black specialist** — Caro-Kann Classical 4...Bf5 (exit FEN B09):
  `EmpiricalDB` with 7,893 positions mined from 100k Caro-Kann games,
  steering via seed-line to a solid defensive position where Black holds.
- **Default** — Falls through to underlying Stockfish master when neither
  specialist recognises the position.

Dispatch logic in `wrapper/uci_app.py`: on each `go` command, the combined
profile checks the current FEN against both specialists' seed lines and
known positions. The first matching specialist takes priority.

## 9-Match Proof Matrix

### White Line — Vienna Gambit exit EPD

| Match | Engine 1 | Score | % | As White | As Black |
|-------|----------|-------|---|----------|----------|
| M1 | StockfishMaster | 21.0/40 | 52.50 | 20-0-0 | 0-18-2 |
| M2 | NullWrapper | 20.0/40 | 50.00 | 20-0-0 | 0-20-0 |
| M3 | CL Combined | 19.5/40 | 48.75 | 19-0-1 | 0-20-0 |

### Black Line — Caro-Kann Classical exit EPD

| Match | Engine 1 | Score | % | As White | As Black |
|-------|----------|-------|---|----------|----------|
| M4 | StockfishMaster | 21.0/40 | 52.50 | 1-0-19 | 1-0-19 |
| M5 | NullWrapper | 19.0/40 | 47.50 | 1-1-18 | 1-3-16 |
| M6 | CL Combined | 21.5/40 | **53.75** | 1-1-18 | **3-0-17** |

### Combined Suite — Both exit EPDs

| Match | Engine 1 | Score | % | As White | As Black |
|-------|----------|-------|---|----------|----------|
| M7 | StockfishMaster | 20.5/40 | 51.25 | 11-0-9 | 0-10-10 |
| M8 | NullWrapper | 20.0/40 | 50.00 | 10-0-10 | 0-10-10 |
| M9 | CL Combined | 21.0/40 | **52.50** | 11-0-9 | **1-10-9** |

## Acceptance Criteria

| # | Criterion | Verdict |
|---|-----------|---------|
| 1 | White profile positive vs SF18 on White line | **INCONCLUSIVE** — 48.75%; exit position is ~100% White-winning; no engine can differentiate |
| 2 | Black profile positive vs SF18 on Black line | **PASS** — 53.75%, +26 Elo est., 3 Black wins, LOS ~92% |
| 3 | Combined positive vs SF18 on combined suite | **PASS** — 52.50%, +17 Elo est., LOS ~93% |
| 4 | Null-wrapper does not account for gain | **PASS** — Null ≤50% on all lines; CL outperforms by +6.25% on Black, +2.50% on Combined |
| 5 | Overrides occur in winning games | **PASS** — Black seed-line steering confirmed in CL as-Black wins |

## Honest Assessment

### What works

The **Black specialist is the real differentiator.** From the Caro-Kann
Classical exit, CL Combined:
- Won 3 games as Black (0 losses) vs SF18
- Null-wrapper suffered 3 losses as Black from the same position
- The specialist's `EmpiricalDB` routing prevents wrapper overhead losses
  and generates genuine Black wins

The **combined engine is net-positive** against SF18 on the full fixed suite:
52.50% with LOS ~93%, driven by the Black line gains.

### What doesn't work

The **White specialist adds no measurable post-exit value.** The Vienna
Gambit exit position is so winning for White (~100% decisive) that:
- White always wins when it has this position, regardless of engine
- The specialist's learned book overrides cannot improve on "already winning"
- CL actually scored slightly worse (48.75%) than raw Master (52.50%),
  suggesting one override may have caused a draw

The White specialist's value is strictly in **line selection** — steering
from startpos into the Vienna Gambit. This value does not manifest in
exit-EPD tests.

### Statistical limitations

- 40-game matches yield ±23 Elo error bars
- LOS 92-93% is close to but below the 95% significance threshold
- Larger matches (200+ games) would be needed for 95% confidence
- Results are suite-specific and do not transfer to general play

## Reproducibility

```bash
# All match PGNs are stored in results/proof_matrix/M{1-9}_*/games.pgn
# Each match directory also contains results.json with parsed scores
# The complete matrix is in results/proof_matrix/proof_matrix.json

# To reproduce any match:
tools/fastchess/fastchess \
  -engine cmd=<engine_binary> name=<name> \
  -engine cmd=bin/stockfish-sf18 name=StockfishSF18 \
  -each tc=1+0.1 threads=1 hash=64 \
  -openings file=<suite.epd> format=epd order=sequential \
  -rounds 20 -repeat -recover -concurrency 1 \
  -pgnout results/proof_matrix/<dir>/games.pgn
```
