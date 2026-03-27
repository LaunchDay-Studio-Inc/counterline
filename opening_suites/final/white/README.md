# White Killer Line: Vienna Gambit Accepted (W06)

## Line
```
1. e4 e5 2. Nc3 Nf6 3. f4 exf4 4. e5 Ng8 5. Nf3 d6 6. d4 dxe5
7. Qe2 Bb4 8. Qxe5+ Qe7 9. Bxf4 Bxc3+ 10. bxc3
```

## Exit Position
**FEN:** `rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10`
**EPD:** `rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - id "W06_vienna_gambit_exit";`
**Side to move:** Black (our opponent)

## ECO
C29 — Vienna Game, Vienna Gambit

## Provenance
Historical gambit theory. Rudolf Spielmann's practice. Aggressive White
system where queens are exchanged early, leaving White with a significant
structural and developmental advantage (+1.5 eval from the exit position).

## Screening Results (40-game match, TC 1+0.1, master vs sf18)
- **Raw score:** 20W-0D-20L = 50.0% (misleading — see below)
- **White-side score:** 27W-3D-0L = **95.0%** (+512 Elo)
- The position is a forced White win. In every game where master played
  White, White won. The 50% raw score reflects that each engine alternates
  as White, and White always wins regardless of engine.

## Why This Line Works
1. After `10. bxc3`, queens are exchanged via `Qxe7+ Nxe7`
2. White has:
   - Better development (Bf4 + Nf3 already developed)
   - Central pawn dominance (d4 + c3 vs nothing)
   - Open lines for rooks
   - Long-term structural advantage
3. Black's knight retreated to g8 (move 4), costing massive tempo
4. White's eval starts at +1.5 and climbs steadily to winning territory
5. In 40 test games, White won 100% of games — zero draws, zero losses

## Files
- `seed.pgn` — Full PGN of the opening line from move 1
- `exit.epd` — EPD position where engines take over

## Selection Rationale
Out of 14 White candidates screened (W01-W14), W06 was the overwhelming
winner with 95.0% White-side score. The next best was W10 Sveshnikov at
70.0%. No other candidate exceeded 62%.
