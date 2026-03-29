# CounterLine Repository Audit

**Date:** 2026-03-28
**Branch:** `exp/integrated-sf18-killer`
**Auditor:** Automated (Claude Opus 4.6)

---

## What Exists

### Engine Wrapper
- UCI-compliant Python wrapper in `wrapper/` (15 modules)
- Two specialists:
  - **White:** Vienna Gambit Accepted — `PolicyTree` (61-node learned book)
  - **Black:** Caro-Kann Classical 4...Bf5 — `EmpiricalDB` (7,893 mined positions)
- Fallback to underlying Stockfish master for all unrecognized positions
- Build system: `Makefile`, devcontainer with Quarto + Tectonic + GCC

### Fixed Suites
- White exit EPD: `rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq -`
- Black exit EPD: `r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq -`
- Suite generator: `opening_suites/generate_suite.py`
- Per-family EPDs in `opening_suites/white/` and `opening_suites/black/`
- Combined suite: `opening_suites/final/combined/combined.epd`

### Proof Matrix (9 matches, 40 games each)
- PGN files: `results/proof_matrix/M{1-9}_*/games.pgn`
- JSON results: `results/proof_matrix/M{1-9}_*/results.json`
- Machine-readable matrix: `results/proof_matrix/proof_matrix.json`
- Human-readable summary: `results/proof_matrix/summary.md`

### Additional Match Results
- 66 files across 22 match directories in `results/matches/`
- Baseline matches, black_killer results, white_killer results
- Historical experiment log: `docs/experiment-log.md`

### Documentation
- `docs/claims.md` — claim policy with allowed/forbidden claims
- `docs/suite-spec.md` — fixed suite specification
- `docs/testing.md` — test procedures
- `docs/final-proof.md` — integrated SF18 killer proof
- `docs/final-line-selection.md` — 28-candidate screening results
- `docs/architecture.md`, `docs/reproducibility.md`, `docs/toolchain.md`

### Tests
- 15 Python unit tests in `tests/`
- Smoke test: `make smoke`
- Fixed-suite match: `make fixed-suite`

### Book (existing)
- 14 Quarto chapters + bibliography
- 12 board diagrams (SVG + PNG)
- Rendering scripts (`render_board.py`, `generate_all_diagrams.py`)
- Built outputs: PDF and EPUB in `book/dist/`

### Repertoire Data
- SQLite database: `data/repertoire/counterline.sqlite` (60 KB)
- v1 lines: QGD Exchange/Carlsbad (White), Petroff (Black) — in configs
- v2 lines: Vienna Gambit (White), Caro-Kann Classical (Black) — in specialists

---

## What Does NOT Exist

1. **200+ game proof match** — needed for 95% statistical confidence
2. **Multi-threaded test results** — all tests at 1 thread only
3. **Longer time control results** — all tests at 1+0.1 (blitz)
4. **Human playtest results** — all results are engine-vs-engine
5. **Claims manifest** — no machine-readable single source of truth for marketing claims
6. **Marketing materials** — no back-cover copy, landing page, or email sequences
7. **Content/ directory** — no manifests or structured claim governance layer
8. **Published proof at 95% confidence** — LOS 92-93% is below threshold

---

## What Is Publicly Verifiable

- All PGN match files in `results/proof_matrix/`
- Machine-readable `proof_matrix.json` with W-L-D for each match
- Suite EPD files matching published exit FENs
- Reproduction commands in `docs/final-proof.md`
- Source code for wrapper, specialist, and suite generator
- Fastchess command lines for exact reproduction

---

## Claims Currently Justified

| Claim | Evidence | Confidence |
|---|---|---|
| CL Combined scores 52.50% vs SF18 on combined suite | M9: 12W-10L-18D, proof_matrix.json | LOS ~93% (trend) |
| Black specialist scores 53.75% on Caro-Kann exit | M6: 4W-1L-35D, 3W-0L-17D as Black | LOS ~92% (trend) |
| Vienna exit is ~100% White-winning regardless of engine | M1-M3: all engines score ~50% overall | Structural (verified) |
| Null-wrapper does not explain gains | M5=47.5%, M8=50.0% vs M6=53.75%, M9=52.50% | Controlled comparison |
| White specialist value is in line selection only | M3: 48.75% (below Master's 52.50%) | Verified negative |
| CL is derived from Stockfish and adds a Python wrapper | Source code inspection | Verified |

---

## Claims Forbidden

| Forbidden Claim | Reason |
|---|---|
| "CounterLine beats Stockfish 18" | LOS 92-93% < 95% threshold; trend not proof |
| "CounterLine is stronger than Stockfish" | Edge is suite-specific, not general |
| "Killer" in headline/title | Implies decisive superiority not supported by evidence |
| "Proven edge over SF18" | 52.50% is a positive trend, not proven at 95% |
| "White specialist outperforms SF18" | 48.75% is inconclusive; position is inherently won |
| "Human players will beat Stockfish with this" | Engine-vs-engine only |
| "Results transfer to classical time controls" | Untested |
| "Results transfer to multi-threaded play" | Untested |
| Any claim hiding Stockfish derivation | Legal/ethical requirement |

---

## Public Fixed-Suite Proof File Status

**A public proof file exists:** `results/proof_matrix/proof_matrix.json` contains
machine-readable results for all 9 matches. PGN files for every game are stored
in `results/proof_matrix/M{1-9}_*/games.pgn`.

**However:** The evidence reaches LOS 92-93%, which is **below the 95%
significance threshold** usually required for statistical proof. The results
show a positive trend but are not conclusive proof. 200+ game matches would be
needed to reach 95% confidence.

**Assessment:** The proof matrix is real, reproducible, and publicly available.
The results are honest. But they do not constitute proof at conventional
significance levels. All marketing and book materials must reflect this
distinction: *positive trend*, not *proven superiority*.
