SHELL := /bin/bash

.PHONY: setup build-stockfish build-wrapper build-all smoke white-suite black-suite fixed-suite sync-upstream test

setup:
	./scripts/build_wrapper.sh

build-stockfish:
	./scripts/build_stockfish.sh

build-wrapper:
	./scripts/build_wrapper.sh

build-all:
	./scripts/build_all.sh

smoke:
	./scripts/smoke_uci.sh

white-suite:
	./scripts/run_white_suite.sh

black-suite:
	./scripts/run_black_suite.sh

fixed-suite:
	./scripts/run_fixed_suite.sh

sync-upstream:
	./scripts/sync_upstream.sh

test:
	. .venv/bin/activate && pytest -n auto tests/test_*.py
