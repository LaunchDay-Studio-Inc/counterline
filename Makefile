SHELL := /bin/bash

.PHONY: setup toolchain build-stockfish build-wrapper build-all smoke test \
        fetch-fastchess white-suite black-suite combined-suite fixed-suite \
        book clean-results sync-upstream

# ── Setup & Toolchain ────────────────────────────────────────────────────────
setup:
	./scripts/bootstrap_codespace.sh

toolchain:
	./scripts/check_toolchain.sh

# ── Build ────────────────────────────────────────────────────────────────────
build-stockfish:
	./scripts/build_stockfish.sh

build-wrapper:
	./scripts/build_wrapper.sh

build-all:
	./scripts/build_all.sh

# ── Test & Smoke ─────────────────────────────────────────────────────────────
smoke:
	./scripts/smoke_uci.sh

test:
	. .venv/bin/activate && pytest -n auto tests/test_*.py

# ── Fastchess ────────────────────────────────────────────────────────────────
fetch-fastchess:
	./scripts/fetch_fastchess.sh

# ── Suite runs ───────────────────────────────────────────────────────────────
white-suite:
	./scripts/run_white_suite.sh

black-suite:
	./scripts/run_black_suite.sh

combined-suite:
	./scripts/run_white_suite.sh
	./scripts/run_black_suite.sh

fixed-suite:
	./scripts/run_fixed_suite.sh

# ── Book production ──────────────────────────────────────────────────────────
book:
	@echo "[counterline] rendering book via Quarto..."
	quarto render docs/book/ --to pdf
	quarto render docs/book/ --to epub
	@echo "[counterline] book outputs in docs/book/_book/"

# ── Clean ────────────────────────────────────────────────────────────────────
clean-results:
	rm -rf results/matches/* results/baseline/*
	@echo "[counterline] results cleaned (README.md preserved)"

# ── Upstream ─────────────────────────────────────────────────────────────────
sync-upstream:
	./scripts/sync_upstream.sh
