# Fixed Suite Specification

## White Seed

QGD Exchange / Carlsbad:

`1.d4 d5 2.c4 e6 3.Nc3 Nf6 4.cxd5 exd5 5.Bg5 c6 6.e3 Be7 7.Bd3 Nbd7 8.Qc2 O-O 9.Nge2 Re8 10.O-O Nf8`

- Book exits after Black's 10th move, `10...Nf8`.
- White is the first side allowed to make a post-book decision.
- Exit FEN:
  `r1bqr1k1/pp2bppp/2p2n2/3p2B1/3P4/3BPN2/PPQ2PPP/R4RK1 w - - 0 11`

## Black Seed

Petroff Main Line:

`1.e4 e5 2.Nf3 Nf6 3.Nxe5 d6 4.Nf3 Nxe4 5.d4 d5 6.Bd3 Be7 7.O-O O-O 8.c4 c6 9.Nc3 Nxc3 10.bxc3`

- Book exits after White's 10th move, `10.bxc3`.
- Black is the first side allowed to make a post-book decision.
- Exit FEN:
  `r1bq1rk1/pp2bppp/2p5/3p4/2PP4/2PB4/P4PPP/R1BQ1RK1 b - - 0 10`

## Validation Rules

- `opening_suites/generate_suite.py` must reconstruct both seed lines with
  `python-chess`.
- The generated board FENs must match the published exit FENs exactly.
- `opening_suites/combined/suite_fixed.epd` is derived from the per-family EPD
  files rather than edited by hand.

