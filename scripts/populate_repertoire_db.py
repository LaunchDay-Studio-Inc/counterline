#!/usr/bin/env python3
"""Initialize or seed the CounterLine repertoire database."""

from __future__ import annotations

from pathlib import Path

import chess
import typer

from wrapper.opening_lock import BLACK_EXIT_FEN, WHITE_EXIT_FEN
from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB

cli = typer.Typer(add_completion=False)


def seed_entries(repo: Repertoire) -> None:
    white = chess.Board(WHITE_EXIT_FEN)
    black = chess.Board(BLACK_EXIT_FEN)
    repo.remember(white, "qgd_exchange_carlsbad", "f2f3", 18, note="default_white_seed")
    repo.remember(white, "qgd_exchange_carlsbad", "a2a3", 12, note="secondary_white_seed")
    repo.remember(black, "petroff_mainline", "c8e6", 18, note="default_black_seed")
    repo.remember(black, "petroff_mainline", "d5c4", 12, note="secondary_black_seed")


@cli.command()
def main(
    db_path: Path = typer.Option(Path("data/repertoire/counterline.sqlite"), "--db-path"),
    seed_defaults: bool = typer.Option(True, "--seed-defaults/--no-seed-defaults"),
) -> None:
    db = RepertoireDB(db_path)
    db.initialize()
    repo = Repertoire(db)
    if seed_defaults:
        seed_entries(repo)
    typer.echo(f"initialized repertoire db at {db_path}")


if __name__ == "__main__":
    cli()

