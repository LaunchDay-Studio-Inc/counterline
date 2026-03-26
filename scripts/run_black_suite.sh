#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUITE_FILE="$ROOT_DIR/opening_suites/black/petroff_mainline/exits.epd"
export SUITE_FILE
exec "$ROOT_DIR/scripts/run_fixed_suite.sh" "$@"

