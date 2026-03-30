# Release Notes — CounterLine Android

## v1.0.0 (2026-03-30)

### Initial Release

CounterLine is an engine-tested chess opening repertoire trainer with two complete weapons:

**White:** Vienna Gambit Accepted (C29) — 1.e4 e5 2.Nc3 Nf6 3.f4 exf4 4.e5…
**Black:** Caro-Kann Classical 4…Bf5 (B18) — 1.e4 c6 2.d4 d5 3.Nc3 dxe4 4.Nxe4 Bf5…

### Features
- First-run onboarding with skill level selection and daily goal setup
- Complete move-by-move repertoire browser with chess board visualization
- Spaced-repetition drill system (SM-2 variant) with 6+ drill types
- Post-opening plans and strategic patterns for both sides
- Common deviation handling with recommended responses
- Annotated model games from real engine-vs-engine matches
- Quick Start cards with memory hooks and key actions
- Exam mode with certification badges
- Mistake review with SRS scheduling
- Practice mode with adjustable engine strength (on-device Stockfish)
- Line-lock sparring, deviation sparring, and play-from-tabiya mode
- Elite analysis pane with multi-PV engine evaluation
- Compare-your-move vs repertoire move vs engine best move
- Explain-last-move for in-context learning
- Prep session generator with exportable one-page cheat sheets
- Personal coach with daily workout and readiness scores
- Tactical motif packs, transition trainer, blindfold recall
- PGN import for comparing your games against the repertoire
- Personal notebook for annotating positions
- Progress tracking with study sessions, streaks, and mastery percentages
- Quick 5 fast review sessions
- Settings: skill level, dark mode, daily goal, notification preferences
- Legal notices and open-source license information
- Full offline operation — no internet required

### Who This Is For
- Ambitious intermediates: your first serious, cohesive repertoire
- Competitive club players: two reliable weapons for tournament play
- Titled grinders: engine-validated lines with clear post-opening plans
- Elite lab users: proof matrix data, specialist tuning, and deep analysis

### Engine Testing Results
- Validated on the published fixed suite against Stockfish 18 at TC 1+0.1, 1 thread, 64 MB hash
- CounterLine Combined: 52.50% (LOS ~93%, +17 Elo est.)
- Black specialist: 53.75% on Caro-Kann exit (+26 Elo est.)
- Results represent a positive trend; not yet statistically proven at 95% confidence

### Technical
- Minimum Android version: 8.0 (API 26)
- Target Android version: 14 (API 34)
- Architecture: Multi-module, Jetpack Compose, Hilt DI, Room database
- License: GPL-3.0 (derived from Stockfish)
