# Best Configuration Snapshot

## Current Best

- **Branch**: `exp/fixed-suite-wrapper`
- **Commit**: `ab00ecd3` (fix: restrict wrapper overrides to critical positions only)
- **Date**: 2026-03-26
- **Tag**: `best/before-tuning` (baseline at `c4c1b078`)

## Wrapper Mode

- Mode: `selective` (default)
- Opening lock: enabled
- Max candidates: 3
- Probe nodes: 10000 (verification), 20000 (rollout)
- Override threshold: repertoire move must be within 5cp of base engine move
- Time budget guard: wrapper disabled when clock < 5 seconds

## Engine Configuration

- Base engine: `bin/stockfish-master` (upstream commit `d173a065`)
- Threads: 1
- Hash: 64 MB
- `ucinewgame`: forwarded between games only (hash preserved within games)
- Info forwarding: all engine `info` lines with scores relayed to GUI

## Suite

- Combined: `opening_suites/combined/suite_fixed.epd`
  - White: QGD Exchange Carlsbad exit (`r1bqrnk1/pp2bppp/2p2n2/3p2B1/3P4/2NBP3/PPQ1NPPP/R4RK1 w`)
  - Black: Petroff Main Line exit (`rnbq1rk1/pp2bppp/2p5/3p4/2PP4/2PB1N2/P4PPP/R1BQ1RK1 b`)
- Time control: 10+0.1

## Repertoire DB

- 63 entries across 18 positions (L0-L2 tree)
- White QGD priors: f2f3 (score_cp=18), a2a3 (12), a1c1 (10), h2h3 (6)
- Black Petroff priors: c8g4 (score_cp=18), d5c4 (12), b8d7 (10)
- Stored in SQLite at `data/repertoire/counterline.sqlite`
- Wrapper only activates at `is_critical` positions (L0 exit FENs)

## Fortification Thresholds

- `min_gain_cp`: 5 (minimum combined score advantage to override)
- `max_regression_cp`: 8 (maximum allowed engine eval drop)
- Instability thresholds: 0.3 (stable), 0.7 (critical)

## Match Evidence

| Match | Games | W-L-D | Score% | Elo | LOS |
| --- | --- | --- | --- | --- | --- |
| CL vs Master (combined) | 50 | 5-0-45 | 55% | **+34.9** | **97%** |
| CL vs SF18 (combined) | 50 | 2-2-46 | 50% | 0 | 50% |

### By Color (vs Master)

| Side | W-L-D | Score |
| --- | --- | --- |
| CL as White (QGD) | 4-0-21 | 58% |
| CL as Black (Petroff) | 1-0-24 | 52% |

## Rerun Commands

```bash
# Build/install
source .venv/bin/activate

# CL vs SF18 (combined, 50 games)
ROUNDS=25 bash scripts/run_fixed_suite.sh \
  --engine2-cmd bin/stockfish-sf18 \
  --engine2-name StockfishSF18 \
  --rounds 25

# CL vs Master (combined, 50 games)
ROUNDS=25 bash scripts/run_fixed_suite.sh --rounds 25

# CL vs SF18 (white suite only)
ROUNDS=25 bash scripts/run_white_suite.sh \
  --engine2-cmd bin/stockfish-sf18 \
  --engine2-name StockfishSF18 \
  --rounds 25

# CL vs SF18 (black suite only)
ROUNDS=25 bash scripts/run_black_suite.sh \
  --engine2-cmd bin/stockfish-sf18 \
  --engine2-name StockfishSF18 \
  --rounds 25
```
