# CounterLine

CounterLine is a fixed-opening specialist wrapper layered on top of the
official [Stockfish](https://github.com/official-stockfish/Stockfish) source
tree in this repository. It is derived from Stockfish and distributed under
GPL-3.0 terms; see [Copying.txt](Copying.txt) and [docs/legal.md](docs/legal.md).

CounterLine does not claim to outperform Stockfish everywhere. Its claim scope
is narrow by design: fixed-suite specialization for v1 on two opening families.

- White suite: QGD Exchange / Carlsbad
- Black suite: Petroff Main Line

The wrapper keeps the Stockfish core in [src](src) and adds a Python UCI
frontend in [wrapper](wrapper). The decision flow is:

1. Opening lock
2. Family detection
3. Selective candidate generation
4. Rollout
5. Fortification
6. Fallback to base Stockfish

## Quick Start

### Codespaces / devcontainer

```bash
make setup
make build-all
counterline-uci --config configs/engines.yml
```

See [docs/codespaces.md](docs/codespaces.md) for the container workflow.

### Local Linux

```bash
python3 -m venv .venv
source .venv/bin/activate
make setup
make build-stockfish
make build-wrapper
counterline-uci --config configs/engines.yml
```

## Build

Build the Stockfish core binaries:

```bash
make build-stockfish
```

This produces:

- `bin/stockfish-master`
- `bin/stockfish-sf_18`

Build the Python wrapper:

```bash
make build-wrapper
```

Build everything:

```bash
make build-all
```

## Run As A UCI Engine

The wrapper speaks UCI on stdin/stdout and can be pointed at any compatible
Stockfish binary via config or environment variables.

```bash
counterline-uci --config configs/engines.yml
```

Optional overrides:

```bash
COUNTERLINE_ENGINE_PATH=bin/stockfish-master counterline-uci
COUNTERLINE_DB_PATH=data/repertoire/counterline.sqlite counterline-uci
```

## Fixed-Suite Matches

Generate and validate the suite:

```bash
python3 opening_suites/generate_suite.py
```

Run the combined fixed suite with Fastchess:

```bash
make fixed-suite
```

Run one side only:

```bash
make white-suite
make black-suite
```

See [docs/suite-spec.md](docs/suite-spec.md) and
[docs/testing.md](docs/testing.md) for the exact suite and test commands.

## Claim Discipline

CounterLine is allowed to make fixed-suite claims only. It must not claim to be
"better than Stockfish" in general, at equal settings, or outside the published
suite boundaries. See [docs/claims.md](docs/claims.md).

## Repository Layout

- [src](src): upstream Stockfish core, left intact
- [wrapper](wrapper): Python wrapper engine
- [configs](configs): engine and threshold config
- [opening_suites](opening_suites): fixed suite assets
- [scripts](scripts): build, smoke, and match helpers
- [docs](docs): architecture, claims, legal, testing
