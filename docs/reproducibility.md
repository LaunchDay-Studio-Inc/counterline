# Reproducibility

This document describes what is committed to git, what is generated at build
time, and how to reproduce any result from a clean checkout.

## What is committed

- Source code (`src/`, `wrapper/`, `scripts/`, `opening_suites/`)
- Configuration (`configs/`, `pyproject.toml`, `Makefile`)
- Devcontainer definition (`.devcontainer/`)
- CI workflows (`.github/workflows/`)
- Documentation (`docs/`)
- Tiny test fixtures (`tests/`)
- Git-tracked results metadata (`results/README.md`, `results/telemetry.jsonl`)

## What is NOT committed

The `.gitignore` excludes:

- Compiled binaries (`src/stockfish*`, `bin/`)
- NNUE network files (`*.nnue`)
- Python virtualenv (`.venv/`)
- Package build artifacts (`dist/`, `build/`, `*.egg-info/`)
- Generated match results (`results/matches/`, `results/baseline/`)
- Repertoire databases (`data/repertoire/*.sqlite*`)
- Cache files (`data/cache/*`)
- fastchess binary (`tools/fastchess/*`)
- Book render output (`docs/book/_book/`)
- Large PGN, log, or binary artifacts

## Reproducing from scratch

```bash
# 1. Open in Codespaces or rebuild the devcontainer
#    The postCreateCommand runs bootstrap automatically.

# 2. Or manually bootstrap:
make setup

# 3. Build Stockfish (master + sf_18):
make build-all

# 4. Fetch fastchess:
make fetch-fastchess

# 5. Run tests:
make test

# 6. Run UCI smoke test:
make smoke

# 7. Run a suite:
make white-suite   # or black-suite, combined-suite, fixed-suite

# 8. Render the book:
make book
```

## Verifying the toolchain

```bash
make toolchain
# or
./scripts/check_toolchain.sh
```

This checks every required tool and prints versions.  It exits non-zero if
anything critical is missing.

## Verifying document pipeline

```bash
pandoc --version | head -1
tectonic --version
quarto --version

# Render book:
quarto render docs/book/ --to pdf
quarto render docs/book/ --to epub
```

## Upstream attribution

This repository is a fork of [Stockfish](https://github.com/official-stockfish/Stockfish).
The upstream remote is configured as `upstream`.  The Stockfish engine source in
`src/` is used under the GPL-3.0-or-later license.  See `Copying.txt` for the
full license text and `AUTHORS` for contributors.
