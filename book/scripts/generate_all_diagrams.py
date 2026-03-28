#!/usr/bin/env python3
"""Generate all board diagrams for the CounterLine book."""

import sys
sys.path.insert(0, ".")
from render_board import render_board

DIAGRAMS = [
    # Vienna Gambit: starting position
    {
        "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "output": "../diagrams/startpos",
        "label": "Starting position",
    },
    # Vienna Gambit: after 1.e4 e5 2.Nc3 Nf6 3.f4
    {
        "fen": "rnbqkb1r/pppp1ppp/5n2/4p3/4PP2/2N5/PPPP2PP/R1BQKBNR b KQkq f3 0 3",
        "output": "../diagrams/vienna_3f4",
        "label": "After 3. f4 — the Vienna Gambit",
    },
    # Vienna Gambit: after 3...exf4 4.e5 Ng8 5.Nf3
    {
        "fen": "rnbqkbnr/pppp1ppp/8/4P3/5p2/2N2N2/PPPP2PP/R1BQKB1R b KQkq - 1 5",
        "output": "../diagrams/vienna_5Nf3",
        "label": "After 5. Nf3 — Black's knight retreats",
    },
    # Vienna Gambit: exit position after 10. bxc3
    {
        "fen": "rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10",
        "output": "../diagrams/vienna_exit",
        "label": "Vienna Gambit Exit — after 10. bxc3",
        "lastmove": "b2c3",
    },
    # Vienna Gambit: after Qxe7+ exchange
    {
        "fen": "rnb1k1nr/ppp1Qppp/8/8/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10",
        "output": "../diagrams/vienna_qxe7",
        "label": "After Qxe7+ — the queen exchange",
        "lastmove": "e5e7",
    },
    # Caro-Kann: after 1.e4 c6
    {
        "fen": "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
        "output": "../diagrams/carokann_1c6",
        "label": "The Caro-Kann Defence — 1...c6",
    },
    # Caro-Kann Classical: after 4...Bf5
    {
        "fen": "rn1qkbnr/pp2pppp/2p5/5b2/3PN3/8/PPP2PPP/R1BQKBNR w KQkq - 1 5",
        "output": "../diagrams/carokann_4Bf5",
        "label": "Caro-Kann Classical — 4...Bf5",
    },
    # Caro-Kann: after 6.h4 h6 7.Nf3 Nd7
    {
        "fen": "r2qkbnr/pp1npppp/2p3bp/8/3PN1PP/5N2/PPP2P2/R1BQKB1R w KQkq - 0 8",
        "output": "../diagrams/carokann_7Nd7",
        "label": "After 7...Nd7 — the Classical tabiya",
    },
    # Caro-Kann: exit position after 10...e6
    {
        "fen": "r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq - 0 11",
        "output": "../diagrams/carokann_exit",
        "label": "Caro-Kann Exit — after 10...e6",
        "lastmove": "e7e6",
    },
    # Model game: CL winning endgame (R1 game 1, move 38 position)
    {
        "fen": "8/8/8/3B4/1k6/8/P5PP/6K1 w - - 0 38",
        "output": "../diagrams/model_endgame",
        "label": "White's winning endgame — bishop + pawns",
    },
    # Caro-Kann: Black fortress after ...Be7, ...O-O, ...Ngf6
    {
        "fen": "r4rk1/pp1nbpp1/2p1pn1p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQ - 4 13",
        "output": "../diagrams/carokann_fortress",
        "label": "Black's solid Caro-Kann fortress",
    },
    # Caro-Kann: Black fortress from Black's perspective
    {
        "fen": "r4rk1/pp1nbpp1/2p1pn1p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQ - 4 13",
        "output": "../diagrams/carokann_fortress_black",
        "label": "Black's fortress — Black's view",
        "flip": True,
    },
]


def main() -> None:
    for d in DIAGRAMS:
        print(f"Rendering: {d['label']}")
        render_board(
            fen=d["fen"],
            output_base=d["output"],
            size=400,
            flip=d.get("flip", False),
            lastmove=d.get("lastmove"),
            arrows=d.get("arrows"),
        )
    print(f"\nDone — {len(DIAGRAMS)} diagrams generated.")


if __name__ == "__main__":
    main()
