#!/usr/bin/env python3
"""Summarize PGN match results from Fastchess output."""

from __future__ import annotations

from collections import Counter
from pathlib import Path

import chess.pgn
import typer

cli = typer.Typer(add_completion=False)


def load_results(path: Path) -> Counter[str]:
    counter: Counter[str] = Counter()
    with path.open("r", encoding="utf-8") as handle:
        while True:
            game = chess.pgn.read_game(handle)
            if game is None:
                break
            counter[game.headers.get("Result", "*")] += 1
    return counter


@cli.command()
def main(pgn_path: Path = typer.Argument(..., exists=True, dir_okay=False)) -> None:
    counts = load_results(pgn_path)
    total = sum(counts.values())
    typer.echo(f"games={total}")
    typer.echo(f"white_wins={counts['1-0']}")
    typer.echo(f"black_wins={counts['0-1']}")
    typer.echo(f"draws={counts['1/2-1/2']}")
    typer.echo(f"unfinished={counts['*']}")


if __name__ == "__main__":
    cli()

