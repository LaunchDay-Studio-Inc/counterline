# Testing

## Python Tests

```bash
source .venv/bin/activate   # or: make setup
python -m pytest tests/ -q
```

15 tests covering:
- `test_determine_fortification.py` — 3 tests (fortification thresholds, duel decisions)
- `test_opening_lock.py` — 6 tests (family detection, seed moves, book completion)
- `test_repertoire_detection.py` — 2 tests (repertoire DB ops)
- `test_uci_smoke.py` — 4 tests (UCI handshake, options, seed line play, go commands)

## Wrapper Smoke

```bash
make build-all
make smoke
```

## Fixed-Suite Match

```bash
# Build binaries first
make build-all

# Fetch match runner
./scripts/fetch_fastchess.sh

# Run matches (uses environment vars for engine configuration)
ENGINE1_CMD=bin/counterline-uci ENGINE1_NAME=counterline \
ENGINE2_CMD=bin/stockfish-sf18 ENGINE2_NAME=stockfish-sf18 \
bash scripts/run_fixed_suite.sh
```

Results are saved under `results/matches/<engine1>-vs-<engine2>/` with PGN, console log, and JSON summary.

## Notes

- Unit tests do not require a Stockfish binary; they use fakes and mocks.
- Smoke and match scripts require built Stockfish binaries in `bin/`.
- The `counterline-uci` launcher at `bin/counterline-uci` wraps the Python UCI app.
- Fastchess v1.8.0-alpha is used for automated matches.

