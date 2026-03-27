# Black Killer Line: Caro-Kann Classical 4...Bf5 (B09)

## Line
```
1. e4 c6 2. d4 d5 3. Nc3 dxe4 4. Nxe4 Bf5 5. Ng3 Bg6
6. h4 h6 7. Nf3 Nd7 8. h5 Bh7 9. Bd3 Bxd3 10. Qxd3 e6
```

## Exit Position
**FEN:** `r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq - 0 11`
**EPD:** `r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq - id "B09_caro_kann_classical_exit";`
**Side to move:** White (our opponent)

## ECO
B18-B19 — Caro-Kann Classical Variation

## Provenance
One of Black's most solid defenses. Extensively used by Anand and Karpov
at the highest levels. The 4...Bf5 Classical line leads to a strategically
rich middlegame where Black has a resilient pawn structure.

## Screening Results (40-game match, TC 1+0.1, sf18 vs master)
- **Raw score (sf18 perspective):** 0W-3L-37D = 46.25%
- **Black-side score:** 1W-21D-0L = **52.3%** (+16 Elo)
- Master never lost as Black in this position. One demonstrated Black win
  (game 31: master mated sf18 while playing Black).

## Why This Line Was Selected
1. **Highest Black-side score** of all 14 candidates (52.3%)
2. **Zero losses as Black** — the position is fortress-solid for the defender
3. The Caro-Kann Classical structure gives Black:
   - Exchanged light-squared bishops (no bad bishop problem)
   - Solid c6-e6 pawn chain
   - Clear development plan (...Ngf6, ...Be7, ...O-O)
   - No structural weaknesses
4. White's h4-h5 push commits to a plan that doesn't generate a concrete
   attack, leaving Black with long-term stability

## Reality Check
Unlike the White killer line (W06 Vienna Gambit at 95%), no Black candidate
showed dominant Black-side performance. The inherent first-move advantage at
engine level (especially at fast time controls) means Black killer lines are
fundamentally harder to mine. B09 was selected as the safest, most reliable
Black position — one that master never loses from.

## Complete Black Screening Results (side-specific, master as Black)

| Rank | Candidate | Score | W | D | L | Elo |
|------|-----------|-------|---|---|---|-----|
| 1 | B09 Caro-Kann Classical | 52.3% | 1 | 21 | 0 | +16 |
| 2 | B04 Grünfeld Exchange | 50.0% | 0 | 21 | 0 | ±0 |
| 3 | B05 Nimzo-Indian Rubinstein | 50.0% | 0 | 20 | 0 | ±0 |
| 4 | B12 Semi-Slav Meran | 50.0% | 0 | 20 | 0 | ±0 |
| 5 | B03 French Winawer | 47.6% | 0 | 20 | 1 | -17 |
| 6 | B08 Dutch Stonewall | 47.5% | 0 | 19 | 1 | -17 |
| 7 | B11 Petroff Steinitz | 45.0% | 0 | 18 | 2 | -35 |
| 8 | B01 Najdorf 6.Bg5 | 42.5% | 0 | 17 | 3 | -53 |
| 9 | B07 Modern Benoni | 42.5% | 0 | 17 | 3 | -53 |
| 10 | B02 Sveshnikov | 40.0% | 1 | 14 | 5 | -70 |
| 11 | B06 KID Mar del Plata | 40.0% | 0 | 16 | 4 | -70 |
| 12 | B13 Benko Gambit | 40.0% | 1 | 14 | 5 | -70 |
| 13 | B14 Scandinavian 3...Qd6 | 40.0% | 1 | 14 | 5 | -70 |
| 14 | B10 Taimanov | 25.0% | 1 | 8 | 11 | -191 |

## Files
- `seed.pgn` — Full PGN of the opening line from move 1
- `exit.epd` — EPD position where engines take over
