#!/usr/bin/env python3
"""Generate candidate opening lines for the SF18-killer search.

Each candidate is a specific exact line from move 1, with:
- a seed PGN
- an exit EPD (the position where the match starts)
- provenance / rationale

We use python-chess to validate all lines and produce correct FENs.
"""

import chess
import chess.pgn
import io
import json
import os
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
LAB = REPO / "suite_lab"

# ─────────────────────────────────────────────────────────────
# WHITE CANDIDATES  (we play White, master vs sf18)
# ─────────────────────────────────────────────────────────────
WHITE_CANDIDATES = [
    {
        "id": "W01_sicilian_najdorf_english_attack",
        "name": "Sicilian Najdorf English Attack 6.Be3",
        "moves": "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 6. Be3 e5 7. Nb3 Be7 8. f3 Be6 9. Qd2 O-O 10. O-O-O Nbd7",
        "provenance": "Najdorf English Attack mainline. One of the sharpest and most theoretically important lines in chess. Opposite-side castling creates natural attacking chances. Source: standard ECO B90 theory.",
        "why": "High-imbalance, opposite-side castling, rich tactical play. White has clear attacking plans against the Black king."
    },
    {
        "id": "W02_sicilian_dragon_yugoslav",
        "name": "Sicilian Dragon Yugoslav Attack",
        "moves": "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 g6 6. Be3 Bg7 7. f3 O-O 8. Qd2 Nc6 9. Bc4 Bd7 10. O-O-O",
        "provenance": "Yugoslav Attack vs Dragon Sicilian. Arguably the most violent mainline in chess theory. ECO B78. Source: standard Dragon theory.",
        "why": "Maximum violence. Opposite-side castling, mutual attacks. White launches h4-h5 pawn storm."
    },
    {
        "id": "W03_ruy_lopez_marshall_anti",
        "name": "Ruy Lopez Anti-Marshall 8.a4",
        "moves": "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 O-O 8. a4 b4 9. d3 d6 10. a5",
        "provenance": "Anti-Marshall with 8.a4. Avoids the sharp Marshall Gambit while maintaining pressure. ECO C88. Source: modern GM practice, widely used by Caruana.",
        "why": "Solid pressure line. Avoids Marshall complications while keeping positional tension. Less drawish than main Marshall due to asymmetric pawn structure."
    },
    {
        "id": "W04_italian_giuoco_piano_slow",
        "name": "Italian Giuoco Piano with d3 and c3",
        "moves": "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. c3 Nf6 5. d3 d6 6. O-O a6 7. a4 Ba7 8. Nbd2 O-O 9. Re1 Be6 10. Bb3",
        "provenance": "Slow Italian / Giuoco Piano. Currently the most popular top-level opening. ECO C54. Source: Carlsen, Firouzja practice 2022-2024.",
        "why": "Rich strategic play with long-term pressure. Central tension maintained. Less likely to lead to sterile draws than symmetrical lines."
    },
    {
        "id": "W05_catalan_open",
        "name": "Catalan Open Variation",
        "moves": "1. d4 Nf6 2. c4 e6 3. g3 d5 4. Bg2 dxc4 5. Nf3 Be7 6. O-O O-O 7. Qc2 a6 8. a4 Bd7 9. Qxc4 Bc6 10. Bf4",
        "provenance": "Open Catalan mainline. White recovers the pawn and maintains long-diagonal pressure. ECO E05. Source: Ding Liren's repertoire.",
        "why": "Dynamic pressure with fianchettoed bishop. White has long-term structural advantage and initiative."
    },
    {
        "id": "W06_vienna_gambit",
        "name": "Vienna Gambit Accepted",
        "moves": "1. e4 e5 2. Nc3 Nf6 3. f4 exf4 4. e5 Ng8 5. Nf3 d6 6. d4 dxe5 7. Qe2 Bb4 8. Qxe5+ Qe7 9. Bxf4 Bxc3+ 10. bxc3",
        "provenance": "Vienna Gambit with 3.f4. Aggressive gambit leading to sharp play. ECO C29. Source: historical gambit theory, Spielmann.",
        "why": "Gambit play creating immediate tactical complications. Structural imbalance with open lines for White."
    },
    {
        "id": "W07_scotch_four_knights",
        "name": "Scotch Game Main Line",
        "moves": "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Nxd4 Nf6 5. Nxc6 bxc6 6. e5 Qe7 7. Qe2 Nd5 8. c4 Ba6 9. b3 g6 10. Ba3",
        "provenance": "Scotch Game mainline with 6.e5. Used extensively by Kasparov. ECO C45. Source: Kasparov's practice.",
        "why": "Asymmetric pawn structure, White has space advantage. Black's doubled c-pawns create long-term targets."
    },
    {
        "id": "W08_kings_indian_attack",
        "name": "King's Indian Attack vs French Setup",
        "moves": "1. e4 e6 2. d3 d5 3. Nd2 Nf6 4. Ngf3 Nc6 5. g3 Be7 6. Bg2 O-O 7. O-O e5 8. Re1 Re8 9. c3 a5 10. a4",
        "provenance": "King's Indian Attack vs French structure. Fischer's weapon. ECO C00. Source: Fischer's practice 1960s-70s.",
        "why": "Flexible setup with kingside attacking potential. Central tension maintained with e4-d3 vs d5-e5."
    },
    {
        "id": "W09_slav_exchange_with_bf4",
        "name": "Slav with Bf4 Pressure System",
        "moves": "1. d4 d5 2. c4 c6 3. Nf3 Nf6 4. Nc3 dxc4 5. a4 Bf5 6. e3 e6 7. Bxc4 Bb4 8. O-O O-O 9. Qe2 Nbd7 10. e4 Bg6",
        "provenance": "Slav Defense main line with 5.a4 Bf5. White pushes e4 gaining central space. ECO D15. Source: standard Slav theory.",
        "why": "Central space advantage for White after e4. Dynamic play with piece activity vs Black's solid but passive setup."
    },
    {
        "id": "W10_english_attack_sveshnikov",
        "name": "Sicilian Sveshnikov English Attack",
        "moves": "1. e4 c5 2. Nf3 Nc6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 e5 6. Ndb5 d6 7. Bg5 a6 8. Na3 b5 9. Nd5 Be7 10. Bxf6 Bxf6 11. c3",
        "provenance": "Sveshnikov Sicilian mainline. One of the most complex theoretical battlegrounds. ECO B33. Source: Carlsen, Caruana modern practice.",
        "why": "Extreme complexity and imbalance. White has the d5 outpost, Black has the bishop pair. Highly dynamic."
    },
    {
        "id": "W11_queens_gambit_ragozin",
        "name": "Queen's Gambit Ragozin with cxd5",
        "moves": "1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. Nc3 Bb4 5. cxd5 exd5 6. Bg5 h6 7. Bh4 c5 8. e3 Nc6 9. Bd3 cxd4 10. exd4 O-O",
        "provenance": "Ragozin Defense with exchange on d5, IQP positions. ECO D38. Source: standard QGD theory.",
        "why": "IQP positions offer real winning chances for both sides. White's piece activity compensates for the isolated d-pawn."
    },
    {
        "id": "W12_sicilian_rossolimo",
        "name": "Sicilian Rossolimo 3.Bb5",
        "moves": "1. e4 c5 2. Nf3 Nc6 3. Bb5 g6 4. Bxc6 dxc6 5. d3 Bg7 6. h3 Nf6 7. Nc3 O-O 8. Be3 b6 9. Qd2 e5 10. O-O-O",
        "provenance": "Rossolimo Sicilian with Bxc6 doubling pawns. Modern Anti-Sicilian weapon. ECO B31. Source: Caruana's repertoire.",
        "why": "Long-term structural advantage from doubled c-pawns. Opposite-side castling adds dynamic play. Less theoretical than Open Sicilian."
    },
    {
        "id": "W13_french_advance_milner_barry",
        "name": "French Advance Milner-Barry Gambit",
        "moves": "1. e4 e6 2. d4 d5 3. e5 c5 4. c3 Nc6 5. Nf3 Qb6 6. Bd3 cxd4 7. cxd4 Bd7 8. O-O Nxd4 9. Nxd4 Qxd4 10. Nc3",
        "provenance": "Milner-Barry Gambit in French Advance. White sacrifices d4 pawn for rapid development and attack. ECO C02. Source: classical gambit theory.",
        "why": "Gambit creating immediate attacking chances. White's development lead and open lines compensate for the pawn."
    },
    {
        "id": "W14_caro_kann_advance",
        "name": "Caro-Kann Advance with Short Castling Attack",
        "moves": "1. e4 c6 2. d4 d5 3. e5 Bf5 4. Nf3 e6 5. Be2 Nd7 6. O-O Ne7 7. Nbd2 h6 8. Nb3 Bg6 9. a4 Nf5 10. a5",
        "provenance": "Caro-Kann Advance Variation. White restricts queenside expansion. ECO B12. Source: Short, Shirov practice.",
        "why": "Space advantage and queenside restriction. Black must find active counterplay or suffer long-term pressure."
    },
]

# ─────────────────────────────────────────────────────────────
# BLACK CANDIDATES  (we play Black, sf18 plays White)
# ─────────────────────────────────────────────────────────────
BLACK_CANDIDATES = [
    {
        "id": "B01_sicilian_najdorf_6bg5",
        "name": "Sicilian Najdorf 6.Bg5 Main Line",
        "moves": "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 6. Bg5 e6 7. f4 Be7 8. Qf3 Qc7 9. O-O-O Nbd7 10. g4",
        "provenance": "Najdorf Poisoned Pawn / English Attack complex. The sharpest Sicilian for Black. ECO B96-B99. Source: Kasparov, Nakamura practice.",
        "why": "Maximum dynamism for Black. Opposite-side castling ensures decisive games. Black's counterplay is real and well-understood."
    },
    {
        "id": "B02_sicilian_sveshnikov",
        "name": "Sicilian Sveshnikov Main Line",
        "moves": "1. e4 c5 2. Nf3 Nc6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 e5 6. Ndb5 d6 7. Bg5 a6 8. Na3 b5 9. Bxf6 gxf6 10. Nd5 f5",
        "provenance": "Sveshnikov Sicilian with ...f5. Extremely dynamic for Black despite structural weaknesses. ECO B33. Source: Liren, Caruana practice.",
        "why": "Black accepts structural damage for dynamic compensation. Bishop pair and central activity. Can lead to sharp middlegames."
    },
    {
        "id": "B03_french_winawer",
        "name": "French Winawer Poisoned Pawn",
        "moves": "1. e4 e6 2. d4 d5 3. Nc3 Bb4 4. e5 c5 5. a3 Bxc3+ 6. bxc3 Ne7 7. Qg4 Qc7 8. Qxg7 Rg8 9. Qxh7 cxd4 10. Ne2",
        "provenance": "French Winawer Poisoned Pawn variation. Black sacrifices kingside pawns for queenside initiative and attack on White's weak c3/d4 pawns. ECO C18. Source: Botvinnik, Short, modern French theory.",
        "why": "Extreme imbalance. Material vs structure/initiative trade. Black's dark-square dominance and queenside attack create winning chances."
    },
    {
        "id": "B04_grunfeld_exchange",
        "name": "Grünfeld Exchange Variation",
        "moves": "1. d4 Nf6 2. c4 g6 3. Nc3 d5 4. cxd5 Nxd5 5. e4 Nxc3 6. bxc3 Bg7 7. Nf3 c5 8. Be3 Qa5 9. Qd2 O-O 10. Rc1 cxd4 11. cxd4 Qxd2+ 12. Kxd2",
        "provenance": "Grünfeld Exchange with endgame variation. Black pressures d4 pawn in the endgame. ECO D85. Source: Kasparov's main weapon as Black.",
        "why": "Black's pressure on the d4 pawn in the endgame is a proven winning method. The fianchettoed bishop is extremely powerful."
    },
    {
        "id": "B05_nimzo_indian_rubinstein",
        "name": "Nimzo-Indian Rubinstein 4.e3",
        "moves": "1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. e3 O-O 5. Bd3 d5 6. Nf3 c5 7. O-O dxc4 8. Bxc4 cxd4 9. exd4 b6 10. Bg5 Bb7",
        "provenance": "Nimzo-Indian Rubinstein variation. Black targets the IQP on d4. ECO E48-E49. Source: Karpov, Kramnik practice.",
        "why": "Black gets clear targets (isolated d4 pawn) and active piece play. A proven winning method for Black at the highest level."
    },
    {
        "id": "B06_kings_indian_mar_del_plata",
        "name": "King's Indian Mar del Plata Attack",
        "moves": "1. d4 Nf6 2. c4 g6 3. Nc3 Bg7 4. e4 d6 5. Nf3 O-O 6. Be2 e5 7. O-O Nc6 8. d5 Ne7 9. Ne1 Nd7 10. f3 f5",
        "provenance": "King's Indian Mar del Plata variation. Black's kingside attack with ...f5 is one of the most dynamic plans in chess. ECO E97. Source: Fischer, Kasparov, Radjabov practice.",
        "why": "Maximum dynamism. Black's kingside attack is extremely dangerous. Engines historically underestimate the long-term attacking potential."
    },
    {
        "id": "B07_benoni_modern",
        "name": "Modern Benoni Main Line",
        "moves": "1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6 6. e4 g6 7. Nf3 Bg7 8. Be2 O-O 9. O-O Re8 10. Nd2 Na6",
        "provenance": "Modern Benoni. Black accepts structural inferiority for dynamic counterplay. ECO A60-A79. Source: Tal, Kasparov practice.",
        "why": "Extreme strategic imbalance. White has space, Black has piece activity and the e5/b5 breaks. Decisive results very likely."
    },
    {
        "id": "B08_dutch_stonewall",
        "name": "Dutch Stonewall",
        "moves": "1. d4 f5 2. c4 Nf6 3. g3 e6 4. Bg2 Be7 5. Nf3 O-O 6. O-O d5 7. b3 c6 8. Bb2 Ne4 9. Nbd2 Nd7 10. Ne5 Nxe5 11. dxe5",
        "provenance": "Dutch Stonewall. Black gets a strong e4 outpost and kingside attacking chances. ECO A90-A99. Source: Kramnik, Carlsen practice in blitz.",
        "why": "Fixed central structure ensures long, strategically rich games. Black's e4 outpost is a permanent advantage."
    },
    {
        "id": "B09_caro_kann_classical",
        "name": "Caro-Kann Classical 4...Bf5",
        "moves": "1. e4 c6 2. d4 d5 3. Nc3 dxe4 4. Nxe4 Bf5 5. Ng3 Bg6 6. h4 h6 7. Nf3 Nd7 8. h5 Bh7 9. Bd3 Bxd3 10. Qxd3 e6",
        "provenance": "Caro-Kann Classical with 4...Bf5. Solid but with dynamic potential. ECO B18-B19. Source: Anand, Karpov main weapon.",
        "why": "Solid structure but with clear plans for Black. The h4-h5 push commits White, and Black can exploit the weakened kingside later."
    },
    {
        "id": "B10_sicilian_taimanov",
        "name": "Sicilian Taimanov 5...a6",
        "moves": "1. e4 c5 2. Nf3 e6 3. d4 cxd4 4. Nxd4 Nc6 5. Nc3 a6 6. Be3 Nf6 7. f4 Bb4 8. Bd3 Qa5 9. Qf3 d5 10. e5 Nd7",
        "provenance": "Taimanov Sicilian with ...a6 and ...Bb4 pin. Dynamic play with central tension. ECO B46. Source: Anand, Adams practice.",
        "why": "Active piece play for Black with pressure on e5 and the pin on Nc3. Central tension creates tactical opportunities."
    },
    {
        "id": "B11_petroff_steinitz",
        "name": "Petroff Steinitz Attack 5.Nc3",
        "moves": "1. e4 e5 2. Nf3 Nf6 3. Nxe5 d6 4. Nf3 Nxe4 5. Nc3 Nxc3 6. dxc3 Be7 7. Be3 Nc6 8. Qd2 Be6 9. O-O-O Qd7 10. Kb1 O-O-O",
        "provenance": "Petroff with Steinitz Attack (5.Nc3). Opposite-side castling creates sharp play. ECO C42. Source: Kramnik, Aronian practice.",
        "why": "Unlike the drawish main Petroff, this variation with opposite-side castling creates real attacking chances for both sides."
    },
    {
        "id": "B12_semi_slav_meran",
        "name": "Semi-Slav Meran Variation",
        "moves": "1. d4 d5 2. c4 c6 3. Nf3 Nf6 4. Nc3 e6 5. e3 Nbd7 6. Bd3 dxc4 7. Bxc4 b5 8. Bd3 Bb7 9. O-O a6 10. e4 c5",
        "provenance": "Semi-Slav Meran variation. One of the sharpest lines in 1.d4 chess. ECO D47-D49. Source: Kramnik, Anand match 2008.",
        "why": "Extreme tactical complexity. Queenside pawn storm with ...b5-c5 creates sharp play. Black's counterplay is potent and well-tested."
    },
    {
        "id": "B13_benko_gambit",
        "name": "Benko Gambit Accepted",
        "moves": "1. d4 Nf6 2. c4 c5 3. d5 b5 4. cxb5 a6 5. bxa6 g6 6. Nc3 Bxa6 7. Nf3 d6 8. e4 Bxf1 9. Kxf1 Bg7 10. g3 O-O",
        "provenance": "Benko Gambit accepted mainline. Black sacrifices a pawn for long-term queenside pressure on the a and b files. ECO A57-A59. Source: standard Benko theory.",
        "why": "Permanent positional compensation for the pawn. Black's pressure on a/b files and the fianchettoed bishop create lasting initiative."
    },
    {
        "id": "B14_scandinavian_3qd6",
        "name": "Scandinavian 3...Qd6 Gubinsky-Melts",
        "moves": "1. e4 d5 2. exd5 Qxd5 3. Nc3 Qd6 4. d4 Nf6 5. Nf3 a6 6. Be2 Bg4 7. O-O e6 8. h3 Bh5 9. Be3 Nc6 10. a3 Be7",
        "provenance": "Scandinavian Defense with 3...Qd6 (Gubinsky-Melts). Flexible setup avoiding the drawish ...Qa5 lines. ECO B01. Source: Tiviakov's practice.",
        "why": "Unusual but sound. Black gets solid development with ...Bg4, ...Nc6, ...e6 setup. Less theoretical, may catch engine preparation off guard."
    },
]


def parse_moves(move_text: str) -> list[str]:
    """Parse SAN moves from a string like '1. e4 c5 2. Nf3 d6 ...'"""
    tokens = move_text.split()
    moves = []
    for tok in tokens:
        # Skip move numbers and result markers
        if tok.endswith('.') or tok in ('*', '1-0', '0-1', '1/2-1/2'):
            continue
        moves.append(tok)
    return moves


def make_pgn_and_epd(candidate: dict, color: str) -> tuple[str, str, str]:
    """Create PGN string, exit FEN, and exit EPD from a candidate dict.
    
    Returns (pgn_string, fen_string, epd_string).
    """
    board = chess.Board()
    moves_san = parse_moves(candidate["moves"])
    
    game = chess.pgn.Game()
    game.headers["Event"] = f"CounterLine {color.title()} Candidate"
    game.headers["Site"] = "CounterLine Suite Lab"
    game.headers["Date"] = "2026.03.26"
    game.headers["Round"] = "-"
    game.headers["White"] = "Seed"
    game.headers["Black"] = "Seed"
    game.headers["Result"] = "*"
    
    node = game
    for san in moves_san:
        move = board.parse_san(san)
        node = node.add_variation(move)
        board.push(move)
    
    fen = board.fen()
    epd = board.epd()
    
    pgn_str = str(game)
    
    return pgn_str, fen, epd


def write_candidate(candidate: dict, color: str, output_dir: Path) -> dict:
    """Write a candidate to disk and return metadata."""
    cand_dir = output_dir / candidate["id"]
    cand_dir.mkdir(parents=True, exist_ok=True)
    
    pgn_str, fen, epd = make_pgn_and_epd(candidate, color)
    
    # Write PGN
    (cand_dir / "seed.pgn").write_text(pgn_str + "\n")
    
    # Write EPD
    epd_line = f'{epd} id "{candidate["id"]}_exit";'
    (cand_dir / "exit.epd").write_text(epd_line + "\n")
    
    # Write metadata
    meta = {
        "id": candidate["id"],
        "name": candidate["name"],
        "moves": candidate["moves"],
        "exit_fen": fen,
        "exit_epd": epd_line,
        "provenance": candidate["provenance"],
        "why": candidate["why"],
        "color": color,
    }
    (cand_dir / "meta.json").write_text(json.dumps(meta, indent=2) + "\n")
    
    return meta


def main():
    white_dir = LAB / "white_candidates"
    black_dir = LAB / "black_candidates"
    
    all_meta = []
    
    print("=== Generating White candidates ===")
    for cand in WHITE_CANDIDATES:
        try:
            meta = write_candidate(cand, "white", white_dir)
            print(f"  OK: {meta['id']} -> {meta['exit_fen']}")
            all_meta.append(meta)
        except Exception as e:
            print(f"  FAIL: {cand['id']}: {e}")
    
    print(f"\n=== Generating Black candidates ===")
    for cand in BLACK_CANDIDATES:
        try:
            meta = write_candidate(cand, "black", black_dir)
            print(f"  OK: {meta['id']} -> {meta['exit_fen']}")
            all_meta.append(meta)
        except Exception as e:
            print(f"  FAIL: {cand['id']}: {e}")
    
    # Write combined EPDs for screening
    white_epds = []
    black_epds = []
    for m in all_meta:
        if m["color"] == "white":
            white_epds.append(m["exit_epd"])
        else:
            black_epds.append(m["exit_epd"])
    
    (LAB / "white_candidates" / "all_exits.epd").write_text("\n".join(white_epds) + "\n")
    (LAB / "black_candidates" / "all_exits.epd").write_text("\n".join(black_epds) + "\n")
    
    # Write combined metadata
    (LAB / "results" / "all_candidates.json").write_text(json.dumps(all_meta, indent=2) + "\n")
    
    print(f"\nTotal: {len([m for m in all_meta if m['color']=='white'])} white, "
          f"{len([m for m in all_meta if m['color']=='black'])} black candidates generated.")
    print(f"Combined EPDs written to suite_lab/white_candidates/all_exits.epd and suite_lab/black_candidates/all_exits.epd")


if __name__ == "__main__":
    main()
