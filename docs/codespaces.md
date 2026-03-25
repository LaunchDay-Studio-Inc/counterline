# Codespaces And Devcontainer

The repo includes `.devcontainer/` for a Linux environment sized for engine
builds and short Fastchess experiments.

## Boot

1. Open the repository in GitHub Codespaces or VS Code Dev Containers.
2. Let the post-create step run `make setup`.
3. Build everything with `make build-all`.

## Inside The Container

- Python dependencies live in `.venv/`
- Wrapper logs go to `results/`
- Repertoire databases go to `data/repertoire/`
- Engine binaries go to `bin/`

## Common Commands

```bash
make build-stockfish
make build-wrapper
make smoke
make fixed-suite
```

