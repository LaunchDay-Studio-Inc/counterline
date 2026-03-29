# Engine Integration — Design Document

## Overview

CounterLine embeds a Stockfish-derived NNUE engine as a native Android library,
exposed through a minimal JNI bridge. The engine enriches the training experience
with on-demand position evaluation, move comparison, and controlled practice play.
It does **not** replace the curated course content — it supplements it.

---

## What Analysis Is Needed On-Device

| Capability | Use Case | Depth / Budget |
|---|---|---|
| **Single-position evaluation** | Show centipawn/WDL for current position in drill review, mistake review, model game replay | Depth 16–20, ~1–2 s |
| **Best move** | Compare user's move to engine's top choice during practice play and deviation drills | Depth 16–20, ~1–2 s |
| **Top-N moves** (MultiPV) | "Why not this move?" explanations; show alternative candidate moves when user deviates | MultiPV 3–5, depth 14–18 |
| **Line analysis** | Evaluate a short sequence of moves to show how a deviation leads to trouble | Depth 12–16, ~1–3 s per position |
| **Strength-limited play** | Practice mode opponent; engine plays at controlled Elo via Skill Level + move time limits | Real-time, movetime 100–2000 ms |

## What Is NOT Needed On-Device

| Capability | Reason |
|---|---|
| Deep analysis (depth 30+) | Battery-prohibitive; the desktop toolchain handles this |
| Tablebase probing | Endgame tables are too large for mobile storage |
| Multi-engine comparison | The Python wrapper's engine pool pattern is for desktop proof runs |
| Book building / mining | Data pipeline runs on server or desktop |
| Repertoire DB population | Content is pre-baked into assets at build time |
| Full UCI protocol loop | The app speaks to the engine via JNI, not stdin/stdout pipes |
| Opening book lookup | Repertoire data already contains the book; no need for Polyglot/CTG |

## When the Engine Should Run

| Trigger | Context |
|---|---|
| User taps "Analyze" on a position | Drill review, mistake review, model game position |
| User makes a move in Practice mode | Engine responds as the opponent |
| User deviates from repertoire | Engine evaluates the deviation vs. the repertoire move |
| "Why not?" button pressed | MultiPV analysis of user's incorrect move choice |
| Expert/Master or Elite Lab skill level | Deeper analysis thresholds unlocked |

## When the Engine Should NOT Run

| Situation | Reason |
|---|---|
| Browsing the repertoire list | Static content; no compute needed |
| Reading plans and patterns | Text-only; engine adds no value |
| Home screen / progress dashboard | No active position to analyze |
| During SRS scheduling | Pure arithmetic; no engine needed |
| Screen is backgrounded or locked | Cancel immediately; save battery |
| Intermediate/Advanced Club recall drills | Show repertoire answer, not engine output |
| Loading or transition animations | Waste of compute |

## How Claims Remain Evidence-Gated

The engine's output is **never presented as a repertoire claim**. All repertoire claims
(move choices, evaluations at exit, proof-match results) come from the pre-computed
content manifests (`proof_manifest.json`, `claims_manifest.json`).

Engine output on-device is labeled distinctly:

- **"Engine says:"** prefix for live evaluations
- **"Live analysis"** badge on any engine-generated annotation
- Engine evaluation numbers are shown with a "~" prefix to indicate approximation
- The app never overwrites a repertoire evaluation with a live engine result

When the engine disagrees with the repertoire move:

- The repertoire move is still shown as the correct answer
- The engine's opinion is presented as supplementary information
- A note explains: "The engine may prefer a different move at this depth.
  The repertoire move was validated at higher depth in the proof matrix."

This separation ensures that on-device analysis (limited depth, single-thread)
never undermines the desktop-validated repertoire claims.

---

## Architecture

```
┌─────────────────────────────────────────┐
│  Kotlin App (Compose UI)                │
│  ┌────────────────────────────────────┐ │
│  │ StockfishEngine (Kotlin)           │ │
│  │  - evaluateFen(fen, depth)         │ │
│  │  - getBestMove(fen, movetime)      │ │
│  │  - getTopMoves(fen, n, depth)      │ │
│  │  - analyzeLine(fens, depth)        │ │
│  │  - setStrengthProfile(profile)     │ │
│  │  - startSession() / stopSession()  │ │
│  │  - isReady()                       │ │
│  └──────────┬─────────────────────────┘ │
│             │ JNI                        │
│  ┌──────────▼─────────────────────────┐ │
│  │ stockfish_bridge.cpp (C++)         │ │
│  │  - Manages Stockfish::Engine       │ │
│  │  - Parses UCI info lines           │ │
│  │  - Thread-safe session lifecycle   │ │
│  └──────────┬─────────────────────────┘ │
│             │ links                      │
│  ┌──────────▼─────────────────────────┐ │
│  │ libstockfish.so (NDK, arm64-v8a)   │ │
│  │  - NNUE network embedded           │ │
│  │  - Single-thread search            │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Strength Profiles

| Profile | Skill Level | Threads | Hash (MB) | Depth Limit | Move Time (ms) |
|---|---|---|---|---|---|
| `TRAINING_EASY` | ~1200 Elo | 1 | 16 | 8 | 200 |
| `TRAINING_MEDIUM` | ~1600 Elo | 1 | 16 | 12 | 500 |
| `TRAINING_HARD` | ~2000 Elo | 1 | 32 | 16 | 1000 |
| `ANALYSIS` | Full strength | 1 | 64 | 20 | 2000 |
| `DEEP_ANALYSIS` | Full strength | 1 | 64 | 24 | 5000 |

These are honest training presets. The app does not claim fake Elo ratings for presets.
The Skill Level UCI option controls actual playing strength, not a cosmetic label.

### NNUE Network

The NNUE network file (`.nnue`) is embedded in the native library at compile time
via `incbin`. No separate asset file is needed at runtime. This matches upstream
Stockfish's default embedding behavior.

### ABI Targets

- **Primary:** `arm64-v8a` (required for release)
- **Secondary:** `armeabi-v7a` (if justified by install-base data)
- **Debug only:** `x86_64` (emulator testing)
