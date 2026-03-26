# Toolchain

CounterLine requires a specific set of tools for building Stockfish, running
the wrapper, executing matches, and producing book artifacts.

## Minimum host requirements

| Resource | Minimum | Preferred |
|----------|---------|-----------|
| CPUs     | 8       | 16        |
| Memory   | 16 GB   | 32 GB     |
| Storage  | 64 GB   | 128 GB    |

The devcontainer `hostRequirements` enforce the minimum.  16 cores / 32 GB is
recommended for profile-guided Stockfish builds and multi-game fastchess
matches.

## Installed via devcontainer

These are installed by the Dockerfile and are available immediately when the
container starts:

### Build tools
- `build-essential`, `gcc`, `g++`, `clang`
- `make`, `cmake`, `ninja-build`

### System utilities
- `git`, `curl`, `wget`, `unzip`, `jq`, `rsync`
- `sqlite3`, `shellcheck`
- `graphviz`, `librsvg2-bin`

### Python
- `python3`, `python3-pip`, `python3-venv`, `pipx`

### Document toolchain
- `pandoc` — format conversion
- `tectonic` — self-contained TeX/PDF engine (no full TeX Live needed)
- Quarto CLI — orchestrates Markdown → PDF / EPUB rendering

## Fetched by scripts

- **fastchess** — downloaded or built from source by `scripts/fetch_fastchess.sh`

## Verification

```bash
# Quick check of all tools:
make toolchain

# Or run the script directly:
./scripts/check_toolchain.sh
```

## VS Code / Codespaces extensions

The devcontainer installs these automatically:

| Extension | ID |
|-----------|----|
| C/C++ | `ms-vscode.cpptools` |
| Clangd | `llvm-vs-code-extensions.vscode-clangd` |
| Makefile Tools | `ms-vscode.makefile-tools` |
| Python | `ms-python.python` |
| Pylance | `ms-python.vscode-pylance` |
| Ruff | `charliermarsh.ruff` |
| ShellCheck | `timonwong.shellcheck` |
| YAML | `redhat.vscode-yaml` |
| Markdown All in One | `yzhang.markdown-all-in-one` |
| Quarto | `quarto.quarto` |
