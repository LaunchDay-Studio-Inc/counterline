# Testing

## Python Tests

```bash
make setup
make test
```

## Wrapper Smoke

```bash
make build-all
make smoke
```

## Fixed-Suite Smoke

```bash
python3 opening_suites/generate_suite.py
./scripts/fetch_fastchess.sh
./scripts/run_fixed_suite.sh --games 2
```

## Notes

- The unit tests do not require a Stockfish binary; they use fakes and mocks.
- The smoke and match scripts do require the built Stockfish binaries.

