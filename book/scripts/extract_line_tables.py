#!/usr/bin/env python3
"""Extract line tables from the repertoire manifest and repo data.

Reads content/repertoire_manifest.json and generates Markdown tables
suitable for inclusion in Quarto chapters.

Usage:
    python extract_line_tables.py [--output ../assets/line_tables.md]
"""

import argparse
import json
from pathlib import Path

MANIFEST = Path(__file__).resolve().parent.parent.parent / "content" / "repertoire_manifest.json"


def load_manifest() -> dict:
    with open(MANIFEST) as f:
        return json.load(f)


def format_seed_line(line: str) -> list[dict]:
    """Parse a seed line string into a table of moves."""
    rows = []
    tokens = line.split()
    move_num = 0
    white_move = ""
    for tok in tokens:
        if tok.endswith("."):
            try:
                move_num = int(tok[:-1])
            except ValueError:
                pass
            continue
        if not white_move:
            white_move = tok
        else:
            rows.append({"move": move_num, "white": white_move, "black": tok})
            white_move = ""
    if white_move:
        rows.append({"move": move_num, "white": white_move, "black": "—"})
    return rows


def render_table(name: str, entry: dict) -> str:
    """Render a repertoire entry as a Markdown table."""
    lines = [f"### {name}\n"]
    lines.append(f"**Opening:** {entry['name']}  ")
    lines.append(f"**ECO:** {entry.get('eco', 'N/A')}  ")
    lines.append(f"**Specialist:** {entry['specialist_type']} ({entry['specialist_size']})  ")
    lines.append(f"**Evaluation at exit:** {entry.get('evaluation_at_exit', 'N/A')}  ")
    lines.append("")

    rows = format_seed_line(entry["seed_line"])
    lines.append("| Move | White | Black |")
    lines.append("|------|-------|-------|")
    for r in rows:
        lines.append(f"| {r['move']} | {r['white']} | {r['black']} |")

    lines.append("")
    lines.append(f"**Exit FEN:** `{entry['exit_fen']}`  ")
    lines.append(f"**Exit EPD:** `{entry['exit_epd']}`  ")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract line tables from repertoire manifest")
    parser.add_argument("--output", default=None,
                        help="Output file (default: stdout)")
    args = parser.parse_args()

    manifest = load_manifest()
    current = manifest["current_repertoire"]

    output = []
    output.append("# Repertoire Line Tables\n")
    output.append("> Auto-generated from `content/repertoire_manifest.json`\n")

    output.append(render_table("White Repertoire", current["white"]))
    output.append(render_table("Black Repertoire", current["black"]))

    if "legacy_repertoire" in manifest:
        legacy = manifest["legacy_repertoire"]
        output.append("---\n")
        output.append("## Legacy Lines (v1)\n")
        output.append(f"> {legacy.get('note', '')}\n")
        for side in ["white", "black"]:
            if side in legacy:
                entry = legacy[side]
                minimal = {
                    "name": entry["name"],
                    "eco": entry.get("eco", "N/A"),
                    "specialist_type": "N/A",
                    "specialist_size": "N/A",
                    "seed_line": entry["seed_line"],
                    "exit_fen": entry["exit_fen"],
                    "exit_epd": entry.get("exit_epd", "N/A"),
                }
                output.append(render_table(f"Legacy {side.title()}", minimal))

    text = "\n".join(output)

    if args.output:
        Path(args.output).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output).write_text(text)
        print(f"Written to {args.output}")
    else:
        print(text)


if __name__ == "__main__":
    main()
