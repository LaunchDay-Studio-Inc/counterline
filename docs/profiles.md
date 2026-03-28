# CounterLine Profiles

CounterLine uses a profile system to activate side-specific specialist
behavior against a target opponent (Stockfish 18).

## Available Profiles

| Profile | Launcher | Side | Opening | Behavior |
|---------|----------|------|---------|----------|
| `default` | `bin/counterline-uci` | — | — | Pure Stockfish master passthrough |
| `white_killer` | `bin/counterline-white` | White | Vienna Gambit Accepted | Learned book overrides post-exit |
| `black_killer` | `bin/counterline-black` | Black | Caro-Kann Classical 4...Bf5 | Seed line steering + conservative book |
| `combined` | `bin/counterline-combined` | Both | Vienna (W) + Caro-Kann (B) | Auto-dispatch by position |

## Combined Profile Dispatch Logic

When `profile=combined`, the engine initializes **both** specialist books
and dispatches based on position detection:

```
1. Is position in Vienna Gambit line?
   → Use White killer specialist (seed line / learned book / root correction)
2. Is position in Caro-Kann Classical line?
   → Use Black killer specialist (seed line / learned book / delegation)
3. Neither?
   → Delegate to Stockfish master (no overrides)
```

This is a pure union: the combined engine behaves identically to
`counterline-white` on White lines and identically to `counterline-black`
on Black lines. There is no cross-contamination between profiles.

## Opening Lines

### White: Vienna Gambit Accepted (W06)

```
1. e4 e5 2. Nc3 Nf6 3. f4 exf4 4. e5 Ng8 5. Nf3 d6 6. d4 dxe5
7. Qe2 Bb4 8. Qxe5+ Qe7 9. Bxf4 Bxc3+ 10. bxc3
```

Exit EPD: `rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq -`

### Black: Caro-Kann Classical 4...Bf5 (B09)

```
1. e4 c6 2. d4 d5 3. Nc3 dxe4 4. Nxe4 Bf5 5. Ng3 Bg6 6. h4 h6
7. Nf3 Nd7 8. h5 Bh7 9. Bd3 Bxd3 10. Qxd3 e6
```

Exit EPD: `r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq -`

## Configuration Files

- `configs/profiles/white-killer.yml` — White specialist parameters
- `configs/profiles/black-killer.yml` — Black specialist parameters
- `configs/profiles/default.yml` — Default passthrough settings

## Setting the Profile

Via launcher script:
```bash
bin/counterline-combined    # combined profile
bin/counterline-white       # white_killer profile
bin/counterline-black       # black_killer profile
```

Via UCI option:
```
setoption name Profile value combined
```

Via command line:
```bash
python -m wrapper --profile combined
```

## Combined Suite

The combined fixed suite for proof matches:
```
opening_suites/final/combined/combined.epd
```

Contains both the Vienna Gambit exit (White plays) and Caro-Kann Classical
exit (Black plays). With `--games 2` in fastchess, each engine plays both
sides from each position.
