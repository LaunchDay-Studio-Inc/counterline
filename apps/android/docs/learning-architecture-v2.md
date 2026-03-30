# CounterLine Learning Architecture v2

## Purpose

CounterLine trains deep mastery of exactly two openings—one White weapon and one Black weapon—through evidence-based learning methods.  
This document defines the learning system that powers every study mode, drill, and coaching interaction.

---

## 1  Foundational Learning Principles

### 1.1  Spacing (Distributed Practice)

| Principle | Implementation |
|-----------|---------------|
| Reviews are spread over increasing intervals | SM-2 algorithm in `Scheduler.kt` computes next-review dates from grade + ease factor |
| New items get short intervals (1–3 days) | Initial interval = 1 day for GOOD, same-session for FAIL |
| Mature items can reach 90+ day intervals | Ease factor floor = 1.3; no ceiling on interval growth |
| Users never review what they already know well | Adaptive queue: items answered EASY three consecutive times are deprioritized |

**Why it matters for chess openings:** Move-order memory is highly susceptible to interference.  
Short-burst daily sessions outperform weekend cram sessions by 2–3× in long-term retention (Cepeda et al., 2006).

### 1.2  Retrieval Practice

Users must **produce** the correct move, not simply recognize it.

| Mode | Retrieval type |
|------|---------------|
| Recall | Type or tap the move from memory |
| Deviation Drill | Recognize the surprise, produce the counter |
| Exam | Timed recall under pressure |
| Blindfold Recall | Produce moves without full board visualization |

No mode ever shows the answer first.  The "Learn" mode is the sole exception; it teaches before testing.

### 1.3  Interleaving

Training sessions mix:
- White and Black positions (unless the user chooses single-side focus)
- Different lines within the same weapon
- Mainlines and sidelines
- Tactical and strategic positions

The scheduler interleaves by default.  The user can override to block-practice a single line when cramming for a tournament.

### 1.4  Mistake-Driven Remediation

Every incorrect response is captured as a `MistakeItem` and enters a remediation pipeline:

```
miss → record → tag theme → boost priority → re-drill → verify fix → mark resolved
```

1. **Record**: FEN, user's move, correct move, line context, timestamp.
2. **Tag**: Opening knowledge gap, move-order confusion, tactical miss, strategic misunderstanding.
3. **Boost**: The node's ease factor drops; its next review is accelerated.
4. **Re-drill**: Mistake Review mode surfaces unresolved items in context.
5. **Verify**: The item must be answered correctly twice at increasing intervals before resolution.
6. **Resolve**: After two consecutive correct answers, the mistake is archived (never deleted).

### 1.5  Model-Game Pattern Learning

Master games carrying the repertoire's key themes are annotated at three depths:
- **Headline**: One-sentence takeaway per critical moment.
- **Plan callout**: What the winning side aimed for in the middlegame.
- **Deep note**: Engine-backed positional or tactical explanation (Expert+ only).

Interactive Guess-the-Move scoring rewards:
- Exact match (3 pts)
- Same piece correct destination (2 pts)
- Thematically reasonable (1 pt via plan alignment)
- Miss (0 pts, with explanation of what was missed)

### 1.6  Opening-to-Middlegame Transition

The most common failure mode in opening preparation is knowing the moves but not understanding the resulting middlegame.  CounterLine bridges this gap:

| Component | What it teaches |
|-----------|----------------|
| Transition Trainer | Typical piece placements from the tabiya forward |
| Plan cards | Pawn breaks, piece maneuvers, and strategic goals per variation |
| Endgame tendencies | When the opening's pawn structure leads to a known endgame type |
| Model Games | Full-game illustration from opening through result |

### 1.7  Deliberate Practice Loops

Each practice loop follows a structured cycle:

```
Diagnose → Target → Drill → Evaluate → Adjust
```

1. **Diagnose**: Dashboard identifies weakest branches via mastery heatmap.
2. **Target**: Coach recommends a focused session on the weakest area.
3. **Drill**: 10–20 positions from that branch, with interleaved review items.
4. **Evaluate**: Accuracy, response time, and confidence grades are recorded.
5. **Adjust**: Scheduler updates intervals; coach recalculates priorities.

---

## 2  Skill Layer Architecture

CounterLine serves four distinct user populations from the same repertoire data, controlled by `SkillLevel`:

| Level | Target Elo | Visible lines | Engine depth | Explanation style |
|-------|-----------|---------------|-------------|-------------------|
| **Intermediate** | 1400–1700 | Main moves + 2–3 critical sidelines per weapon | Eval bar only | Natural language, no jargon |
| **Advanced Club** | 1700–2000 | All mainlines + common deviations | Eval + best-move comparison | Terminology introduced, plans emphasized |
| **Expert / Master** | 2000–2300 | Full repertoire tree including rare sidelines | MultiPV + depth control | Concise, assumes pattern vocabulary |
| **Elite Lab** | 2300+ | Everything + novelty candidates + engine-only branches | Full engine access, custom depth | Minimal prose, raw analysis preferred |

### Content gating rules

- Lines tagged above the user's level are hidden from drill queues and learn mode.
- Plans and themes unlock progressively: Intermediate sees "castle queenside and attack," Expert sees "exchange sacrifice on c3 to weaken d4."
- Engine analysis depth scales: Intermediate gets eval-bar summary; Elite gets MultiPV with continuation lines.
- Exam difficulty scales: Intermediate exams cover mainlines only; Elite exams include rare deviations.

### Promotion conditions

Users can self-select any level.  The app suggests promotion when:
- Mainline mastery ≥ 90% at current level
- Exam pass rate ≥ 80% at current level
- Active study streak ≥ 14 days

---

## 3  Study Mode Design Matrix

| Mode | Purpose | Retrieval? | Timed? | Uses SRS? | Skill-filtered? |
|------|---------|-----------|--------|-----------|-----------------|
| Guided Learn | First exposure to a line | No | No | Seeds SRS | Yes |
| Recall | Spaced retrieval of learned moves | Yes | No | Core SRS loop | Yes |
| Deviation Drill | Handle opponent surprises | Yes | No | Own SRS track | Yes |
| Model Game GTM | Pattern recognition via master games | Partial | No | No | Yes |
| Mistake Review | Fix recurring errors | Yes | No | Accelerated SRS | Yes |
| Tactical Motif | Repertoire-specific tactics | Yes | Optional | Woodpecker cycle | Yes |
| Transition Trainer | Opening → middlegame plans | Mixed | No | No | Yes |
| Exam Mode | Certification assessment | Yes | Yes | Feeds results back | Yes |
| Quick 5 | Daily micro-session | Yes | 5 min | Uses SRS queue | Yes |
| Blindfold Recall | Advanced memory training | Yes | No | Uses SRS queue | Expert+ |

---

## 4  Scheduler Design

### 4.1  Core Algorithm (SM-2 Variant)

```
if grade < HARD:
    interval = 1    // reset
    ease = max(ease - 0.20, 1.3)
elif grade == HARD:
    interval = max(interval * 1.2, 1)
    ease = max(ease - 0.15, 1.3)
elif grade == GOOD:
    interval = interval * ease
    ease = ease + 0.0
elif grade == EASY:
    interval = interval * ease * 1.3
    ease = ease + 0.15
```

### 4.2  Priority Queue Ordering

Items are ordered by:
1. **Overdue ratio** = (days since due) / interval — higher = more urgent
2. **Mistake boost** — unresolved mistakes get +2.0 to overdue ratio
3. **Interleaving** — alternates White/Black when both have due items
4. **Diversity** — no more than 3 consecutive items from the same line

### 4.3  Session Sizing

| Session type | Target items | Time budget |
|-------------|-------------|-------------|
| Quick 5 | 5–8 | 5 min |
| Standard drill | 15–25 | 15 min |
| Deep session | 30–50 | 30 min |
| Exam | 20 (fixed) | 20 min |

### 4.4  Lapse Handling

When a user returns after a long break:
- Items overdue by >30 days enter a "relearn" state (interval resets to 1 day, ease preserved).
- A maximum of 20 relapse items per session to prevent overwhelm.
- Coach message: "Welcome back. Let's rebuild from your strongest lines first."

---

## 5  Coaching Intelligence

### 5.1  Daily Workout Generation

```
inputs:
  - user's SRS queue (sorted by priority)
  - mistake inventory (unresolved items)
  - White vs Black mastery imbalance
  - days until next tournament (if set)
  - recent session history (avoid fatigue)

output:
  - ordered list of drill items (15–25)
  - mode recommendation (Recall, Deviation, or Mistake Review)
  - side focus (White, Black, or Mixed)
  - explanation: "You have 4 overdue Black lines and 2 unresolved mistakes in the Petroff.  
    Today's session focuses on Black with mistake remediation."
```

### 5.2  Weakness Detection

The coach monitors:
- **Chronic miss nodes**: Same position failed ≥ 3 times across different sessions.
- **Branch fragility**: Lines where accuracy drops >20% compared to the user's average.
- **Side imbalance**: >15% mastery gap between White and Black weapons.
- **Exam underperformance**: Exam score significantly below drill accuracy (suggests test anxiety or shallow encoding).

### 5.3  Explanation Depth Adaptation

| User level | Explanation template |
|-----------|---------------------|
| Intermediate | "{move} defends the {piece} and prepares {plan}." |
| Advanced Club | "{move} maintains the tension. The plan is {plan}; if {alternative}, then {refutation}." |
| Expert | "{move} (={eval}). Key idea: {plan}. Compare {alternative} which allows {line}." |
| Elite | "{move} {eval}. {alternative}: {continuation} ({eval2})." |

---

## 6  Data Flow

```
Repertoire JSON (assets)
        │
        ▼
  ContentSeeder → Room DB
        │
        ▼
  Repositories (Repertoire, UserProgress, NodeReview, Mistake)
        │
        ▼
  UseCases (GetLines, ReviewNode, GenerateWorkout, TrackSession)
        │
        ▼
  ViewModels (per study mode)
        │
        ▼
  Compose UI (ChessBoard, DrillCard, CoachBanner)
        │
        ▼
  User interaction → grade/move → back to UseCases
```

---

## 7  Retention and Transfer Goals

Every feature in CounterLine must serve at least one of:

| Goal | Definition | Example feature |
|------|-----------|-----------------|
| **Retention** | User remembers the move next week, next month, next year | SRS scheduling, mistake remediation |
| **Transfer** | User applies the idea in a real game against a move they haven't drilled | Plan cards, transition trainer, model games |
| **Practical prep** | User is ready for a specific opponent or tournament | Preparation packs, deviation drill, exam mode |

Features that serve none of these three goals are out of scope.

---

## 8  Anti-Patterns (What CounterLine Does NOT Do)

| Anti-pattern | Why avoided |
|-------------|-------------|
| Gamification XP / levels that reward volume over quality | Inflates engagement metrics but does not improve play |
| Unlimited repertoire breadth | Dilutes mastery of the two chosen weapons |
| AI-generated explanations without engine backing | Can be subtly wrong in critical positions |
| Passive video content | Does not produce retrieval; low transfer |
| Social features / leaderboards | Distraction from personal mastery |
| Subscription paywalls on core training | Learning momentum should not be interrupted by billing |

---

## 9  Measurement

### 9.1  Learning Metrics (Internal)

- **Retention rate**: % of due items answered correctly on first attempt
- **Interval growth**: Average interval across all active items
- **Mistake resolution time**: Days from first miss to resolved status
- **Coverage**: % of repertoire nodes with at least one successful review

### 9.2  User-Facing Metrics

- **Mastery heatmap**: Per-node color from red (unknown) to green (mature)
- **Preparation readiness score**: Weighted coverage of mainlines + deviations
- **Weekly progress arc**: Items learned, reviewed, and resolved this week
- **Streak**: Consecutive days with at least one completed session

---

*Document version: 2.0*  
*Last updated: 2026-03-30*
