# CounterLine Training Model

## Overview

CounterLine teaches two complete opening repertoires — one as White, one as Black — using evidence-based learning techniques adapted for chess study. The system does not promise mastery from app usage alone; it provides a structured, honest tool that supports study from strong club level through elite preparation.

## Learning Stack

### 1. Retrieval Practice

Every study session forces active recall rather than passive review. The user sees a position and must produce the correct move from memory before receiving feedback. No move list is visible during recall drills.

- **Mechanism**: Position is displayed; user taps or enters the repertoire move.
- **Why it works**: Testing memory strengthens it more effectively than re-reading.

### 2. Spaced Repetition

Each repertoire node carries its own review schedule. Correctly recalled moves are shown less frequently; missed moves appear sooner.

- **Grading**: Fail / Hard / Good / Easy after each attempt.
- **Intervals**: Start at minutes, expand through hours → days → weeks.
- **Overdue items**: Prioritized in the next session without penalty-stacking.

### 3. Interleaving

Drills mix White and Black positions, different openings, and different move depths within a single session. The user cannot predict which line comes next.

- **Why it works**: Interleaved practice builds discrimination ability — the skill of recognizing which pattern applies to a given position.

### 4. Immediate Feedback

After every response the app shows:
- Whether the move was correct.
- The correct move if wrong.
- A brief explanation of the move's purpose.
- The strategic idea behind the position.

No delayed or batched feedback. The user learns at the point of error.

### 5. Mistake-Driven Remediation

Every incorrect response automatically creates a review item:
- The missed position is tagged with the correct answer and explanation.
- It enters the spaced-repetition queue at the shortest interval.
- The user can browse all accumulated mistakes in Mistake Review mode.
- Mistakes are never silently discarded.

### 6. Model-Game Learning

Annotated games from the repertoire are presented as interactive "guess the move" exercises:
- User attempts each move in sequence.
- After each guess, the model move and annotation are revealed.
- Strategic commentary explains plans, piece placement, and critical moments.
- Games are filterable by opening, side, and theme.

### 7. Tactical Motif Repetition

Short tactical exercises are drawn from positions that arise within the repertoire:
- Fork, pin, skewer, and deflection motifs from the specific pawn structures in the repertoire.
- Positions are repeated in a Woodpecker-style fast cycle: solve, review, solve faster.
- This bridges the gap between opening knowledge and middlegame execution.

### 8. Periodic Exam Mode

Timed assessments with no hints:
- Covers both White and Black repertoires.
- Scores by accuracy, recall speed, and branch coverage.
- Separate per-side certificates/badges (earned inside the app — not external credentials).
- Exam results do not affect the spaced-repetition queue directly, but missed exam questions feed into the mistake pipeline.

## Skill Layers

Content is organized into four depth tiers:

| Layer | Target Audience | Content Scope |
|---|---|---|
| **Intermediate** | Club players learning the repertoire | Main lines, basic plans, common traps, high guidance |
| **Advanced Club** | Experienced club players | Important branches, middlegame plans, common deviations |
| **Expert / Master** | Titled or near-titled players | Move-order nuance, deep branches, harder quizzes |
| **Elite Lab** | Full-depth study | Complete reference tree, raw engine notes, deep line browser, exact tabiya search |

Users select their layer. Content from higher layers is hidden, not locked — the user can change level at any time. No paywall is implied by layers.

## Study Modes

1. **Learn Mode** — Guided introduction with move-by-move explanation.
2. **Recall Mode** — Position shown, user plays the correct move, confidence grading stored.
3. **Deviation Drill** — Opponent deviations shown, user finds the repertoire response.
4. **Plans & Patterns** — Structure-specific plans, piece placement, pawn breaks, danger signals.
5. **Model Game Replay** — Interactive guess-the-move with annotated feedback.
6. **Mistake Review** — All missed moves collected, explained, and scheduled for re-review.
7. **Exam Mode** — Timed, no-hint assessment with accuracy and speed scoring.
8. **Quick 5 / Daily Session** — Fast 5-minute mobile-first review for daily habit building.

## Scheduling

The scheduler maintains per-node state:

```
NodeReviewState:
  nodeId: String
  side: Side
  easeFactor: Float        (initial 2.5)
  intervalDays: Float      (initial 0)
  repetitions: Int         (initial 0)
  lastReviewEpochMs: Long
  nextReviewEpochMs: Long
  lapseCount: Int
  grade: Grade             (FAIL | HARD | GOOD | EASY)
```

After each review:
- **Fail**: interval resets to a short value, lapse count increments, ease factor decreases.
- **Hard**: interval grows modestly, ease factor decreases slightly.
- **Good**: interval grows by the current ease factor.
- **Easy**: interval grows by more than the ease factor, ease factor increases.

Mastery score per line = weighted average of all node review states in that line.

Daily review queue = all nodes where `nextReviewEpochMs <= now`, sorted by overdue duration (most overdue first), then by lapse count (most lapsed first).

## Honesty Commitments

- The app does not claim that completing drills guarantees any rating or title.
- Certificates and badges are internal milestones, not external credentials.
- The app states clearly that opening study is one component of chess improvement.
- No fake urgency ("your streak will expire!"), no manipulative dark patterns.
- Gamification is motivating, not casino-like. Streaks measure consistency, not worth.
