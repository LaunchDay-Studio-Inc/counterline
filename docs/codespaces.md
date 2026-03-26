# Codespaces and Devcontainer

The repo includes `.devcontainer/` for a fully self-contained Linux environment
sized for engine builds, suite mining, fastchess matches, and book production.

## Host requirements

| Resource | Minimum | Preferred |
|----------|---------|-----------|
| CPUs     | 8       | 16        |
| Memory   | 16 GB   | 32 GB     |
| Storage  | 64 GB   | 128 GB    |

When creating a GitHub Codespace, select an 8-core (or larger) machine type.

## Boot

1. Open the repository in GitHub Codespaces or VS Code Dev Containers.
2. The `postCreateCommand` runs `scripts/bootstrap_codespace.sh` automatically.
   This creates `.venv`, installs Python deps, verifies tools, fetches
   fastchess, and confirms Stockfish compiles.
3. Build everything with `make build-all`.

## Rebuilding the container

If you change the Dockerfile or devcontainer.json:

```bash
# In VS Code: Ctrl+Shift+P → "Dev Containers: Rebuild Container"
# Or from the command line (requires devcontainer CLI):
devcontainer build --workspace-folder .
devcontainer up --workspace-folder .
```

## What the Dockerfile installs

- Build tools: gcc, g++, clang, make, cmake, ninja
- Utilities: git, curl, wget, jq, rsync, sqlite3, shellcheck, graphviz
- Python: python3, pip, venv, pipx
- Document pipeline: pandoc, tectonic (PDF engine), Quarto CLI

See [toolchain.md](toolchain.md) for the full list and extensions.

## Inside the container

- Python dependencies live in `.venv/`
- Wrapper entrypoint: `.venv/bin/counterline-uci`
- Engine binaries go to `bin/`
- Match results go to `results/`
- Repertoire databases go to `data/repertoire/`
- fastchess binary goes to `tools/fastchess/`
- Book output goes to `docs/book/_book/`

## Common commands

```bash
make setup              # bootstrap (runs automatically on container create)
make toolchain          # verify all tools are present
make build-stockfish    # build master + sf_18 binaries
make build-wrapper      # install Python wrapper in .venv
make build-all          # both of the above
make smoke              # UCI smoke test
make test               # pytest suite
make fetch-fastchess    # download/build fastchess
make white-suite        # run white opening suite
make black-suite        # run black opening suite
make combined-suite     # run both suites
make book               # render PDF + EPUB via Quarto
make clean-results      # delete match/baseline results
```

## Fetching fastchess

```bash
make fetch-fastchess
# or
./scripts/fetch_fastchess.sh
```

This downloads a pre-built Linux binary.  If the download fails, it
automatically falls back to building from source.

## Verifying document toolchain

```bash
pandoc --version | head -1
tectonic --version
quarto --version
quarto render docs/book/ --to pdf
quarto render docs/book/ --to epub
```

