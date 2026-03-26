#!/usr/bin/env python3
"""Initialize or seed the CounterLine repertoire database."""

from __future__ import annotations

from pathlib import Path

import chess
import chess.polyglot
import typer

from wrapper.opening_lock import BLACK_EXIT_FEN, WHITE_EXIT_FEN
from wrapper.repertoire import Repertoire
from wrapper.repertoire_db import RepertoireDB

cli = typer.Typer(add_completion=False)


def seed_entries(db: RepertoireDB, repo: Repertoire) -> None:
    white = chess.Board(WHITE_EXIT_FEN)
    black = chess.Board(BLACK_EXIT_FEN)

    # Seed nodes
    db.upsert_node(white, "qgd_exchange_carlsbad", is_critical=True, structure_tags="carlsbad,minority_attack")
    db.upsert_node(black, "petroff_mainline", is_critical=True, structure_tags="petroff,symmetric_center")

    # Seed move priors for white (QGD Exchange)
    repo.remember(white, "qgd_exchange_carlsbad", "f2f3", 18, note="e4_break_prep")
    repo.remember(white, "qgd_exchange_carlsbad", "a2a3", 12, note="minority_attack_prep")
    repo.remember(white, "qgd_exchange_carlsbad", "a1c1", 10, note="c_file_pressure")
    repo.remember(white, "qgd_exchange_carlsbad", "h2h3", 6, note="luft")

    # Seed move priors for black (Petroff)
    repo.remember(black, "petroff_mainline", "c8e6", 18, note="bishop_development")
    repo.remember(black, "petroff_mainline", "d5c4", 12, note="c_pawn_exchange")
    repo.remember(black, "petroff_mainline", "b8d7", 10, note="knight_development")
    repo.remember(black, "petroff_mainline", "f8e8", 8, note="e_file_pressure")


def dump_db(db: RepertoireDB) -> None:
    data = db.dump_all()
    for table_name, rows in data.items():
        typer.echo(f"\n=== {table_name} ({len(rows)} rows) ===")
        for row in rows:
            typer.echo(f"  {row}")


@cli.command()
def main(
    db_path: Path = typer.Option(Path("data/repertoire/counterline.sqlite"), "--db-path"),
    seed_defaults: bool = typer.Option(True, "--seed-defaults/--no-seed-defaults"),
    dump: bool = typer.Option(False, "--dump"),
) -> None:
    db = RepertoireDB(db_path)
    db.initialize()
    repo = Repertoire(db)
    if seed_defaults:
        seed_entries(db, repo)
        typer.echo(f"seeded repertoire db at {db_path}")
    if dump:
        dump_db(db)
    if not seed_defaults and not dump:
        typer.echo(f"initialized repertoire db at {db_path}")


if __name__ == "__main__":
    cli()

