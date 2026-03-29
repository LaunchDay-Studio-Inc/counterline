# CounterLine Training System — Implementation Summary

## Overview

The CounterLine Android app has been transformed from a static repertoire browser
into a **serious chess-learning product** that teaches, tests, repeats, and adapts.
Every feature is backed by spaced-repetition scheduling, per-node mastery tracking,
a mistake pipeline, and skill-layer content gating.

---

## Phase 0 — Training Model Documentation

**File:** `docs/training-model.md`

Documents the 8-element learning stack: retrieval practice, spaced repetition,
interleaving, immediate feedback, mistake-driven remediation, model-game learning,
tactical-motif repetition, and periodic exam mode. Includes honesty commitments
(no "become GM" promises).

---

## Phase 1 — Skill Layers

Four skill levels control content depth:

| Level | Enum | Content |
|-------|------|---------|
| Club player | `INTERMEDIATE` | Core lines + basic plans |
| Advanced club | `ADVANCED_CLUB` | Side-lines, deeper deviations |
| Expert/master | `EXPERT_MASTER` | Rare sub-variations, advanced motifs |
| Engine lab | `ELITE_LAB` | All engine-approved novelties |

### Changed files
- `core/model/Models.kt` — `SkillLevel`, `StudyMode`, `ReviewGrade` enums; `skillLevel` field on `RepertoireLine`, `Plan`, `Theme`, `Deviation`, `Drill`; `whyThisMove`/`keyPlanCallout` on `RepertoireMove`; new `DrillType` values
- `core/model/UserModels.kt` — `NodeReviewState`, `MistakeItem`, `ExamResult`, `StudySession`, `OpeningMastery`, `Badge`, enhanced `ProgressStats`, `UserSettings.skillLevel`
- `core/database/entity/Entities.kt` — All entity classes updated; 5 new entities
- `core/database/dao/Daos.kt` — 5 new DAOs
- `core/database/CounterLineDatabase.kt` — v2, 5 new entity classes
- `core/database/di/DatabaseModule.kt` — 5 new DAO providers
- `core/data/repository/Repositories.kt` — Updated mappers, 5 new repositories
- `core/data/repository/SettingsRepository.kt` — `skillLevel` key
- `core/domain/UseCases.kt` — Skill-level filtering in `GetRepertoireLinesUseCase` and `GetDrillsUseCase`
- `core/content/di/ContentModule.kt` — `ReviewScheduler` provider
- `feature/settings/SettingsViewModel.kt` — `setSkillLevel()` method
- `feature/settings/SettingsScreen.kt` — Skill level dropdown selector

---

## Phase 2 — Study Modes (8 total)

### Implemented study modes

| # | Mode | Where | Description |
|---|------|-------|-------------|
| 1 | **Learn** | `feature/learn/` (NEW) | Move-by-move guided introduction; shows `whyThisMove` and `keyPlanCallout` per move; marks each node as reviewed |
| 2 | **Recall / Drill** | `feature/drill/` (ENHANCED) | Position shown, user picks correct move; confidence grading (Again/Hard/Good/Easy) feeds into SRS |
| 3 | **Deviation Drill** | `feature/deviations/` (existing) | Browse deviations with response cards; `strategicIdea` field added |
| 4 | **Plans & Patterns** | `feature/plans/` (existing) | Side-filtered plans + strategic themes |
| 5 | **Model Game Replay** | `feature/modelgames/` (existing) | Expandable annotated games with eval progression |
| 6 | **Mistake Review** | `feature/mistakereview/` (NEW) | Shows past mistakes as flashcards; reveal answer + confidence grade; resolves on Good/Easy |
| 7 | **Exam** | `feature/exam/` (ENHANCED) | Timed 20-question exam; per-question response time; auto-saves ExamResult; awards pass/fail badges |
| 8 | **Quick 5** | `feature/quick5/` (NEW) | Fast 5-question daily session; auto-grades via SRS; tracks elapsed time |

### New feature modules created
- `feature/learn/` — `LearnViewModel.kt`, `LearnScreen.kt`, `build.gradle.kts`
- `feature/mistakereview/` — `MistakeReviewViewModel.kt`, `MistakeReviewScreen.kt`, `build.gradle.kts`
- `feature/quick5/` — `Quick5ViewModel.kt`, `Quick5Screen.kt`, `build.gradle.kts`

### Navigation
- `CounterLineNavHost.kt` — 3 new `NestedRoutes` (LEARN, MISTAKE_REVIEW, QUICK_5) + 3 new `composable()` routes
- `HomeScreen.kt` — 3 new quick action buttons (Learn Mode, Mistake Review, Quick 5) with callback parameters
- `settings.gradle.kts` — 3 new module includes
- `app/build.gradle.kts` — 3 new implementation dependencies

---

## Phase 3 — Scheduling & Spaced Repetition

**File:** `core/domain/Scheduler.kt`

### Algorithm: SM-2 variant
| Grade | Ease change | Interval | Reps | Lapses |
|-------|-------------|----------|------|--------|
| FAIL | −0.20 (min 1.3) | ~10 min | reset to 0 | +1 |
| HARD | −0.15 | ×1.2 | +1 | unchanged |
| GOOD | unchanged | ×ease | +1 | unchanged |
| EASY | +0.15 | ×ease×1.3 | +1 | unchanged |

First review: ~1 hour → second: 1 day → subsequent: interval × ease factor.

### Key methods
- `schedule(current, grade)` → updated `NodeReviewState`
- `calculateMastery(states)` → weighted 0.0–1.0 score (50% interval + 30% ease + 20% lapse)
- `buildReviewQueue(states)` → sorted by most overdue, then lapse count
- `buildInterleavedSession(states, max)` → alternates White/Black

### Use cases powered by scheduler
- `ReviewNodeUseCase` — schedule + record mistakes on FAIL
- `GetReviewQueueUseCase` — due now, due count, interleaved session
- `GetMasteryUseCase` — per-line, per-side mastery + weakest nodes

---

## Phase 4 — Tactical & Strategic Reinforcement

### Woodpecker mode (in DrillViewModel)
When enabled, missed items (graded FAIL or HARD) are automatically re-queued
into subsequent rounds until all items are answered correctly. The UI shows
the current Woodpecker round number and count of remaining items.

### New drill types
- `TACTICAL_MOTIF` — find the tactical pattern
- `STRUCTURE_FLASHCARD` — pawn structure recognition
- `TRANSITION_QUIZ` — middle-game transition plans
- `COMPARE_POSITION` — compare two positions
- `DEVIATION_RESPONSE` — find the correct response to a deviation

---

## Phase 5 — Progress, Motivation & Streaks

### Streak calculation
`StudySessionRepository.calculateStreak()` queries distinct study days and counts
consecutive days ending today or yesterday.

### Badge system
12 default badges seeded on first launch covering both sides:
- Repertoire Start (White/Black)
- Recall 50% (White/Black)
- Mastery (White/Black)
- Perfect Exam (White/Black)
- Exam Certificate (auto-awarded on exam pass)
- 7-Day Streak, 30-Day Streak

### Progress dashboard (`ProgressScreen`)
- **Streak counter** — real consecutive-day count from study sessions
- **Total study time** — aggregated from all sessions (hours + minutes)
- **Mastery bars** — White/Black mastery scores with color thresholds
- **Accuracy bars** — per-side accuracy percentages
- **Earned badges** — FlowRow with all earned badges
- **Exam certificates** — best exam result per side
- **Weakest areas** — top 5 nodes with most lapses
- **Unresolved mistakes** — count with prompt to use Mistake Review

---

## Phase 6 — Tests

### `core/model/src/test/.../ModelsTest.kt`
- `SkillLevelTest` — ordering, comparison, content filtering logic
- `ReviewGradeTest` — ordinal ordering, comparison operators
- `StudyModeTest` — all 8 modes exist
- `DrillTypeTest` — all 10 types exist including new tactical types

### `core/model/src/test/.../UserModelsTest.kt`
- `NodeReviewStateTest` — default initial values
- `MistakeItemTest` — default unresolved state
- `ExamResultTest` — pass/fail threshold at 70%
- `BadgeTest` — earned vs unearned state
- `ProgressStatsTest` — default zero values, empty collections
- `UserSettingsTest` — default INTERMEDIATE skill level

### `core/domain/src/test/.../ReviewSchedulerTest.kt` (25 tests)
- FAIL: resets reps, increases lapses, decreases ease, ~10min interval, ease floor at 1.3
- GOOD: first rep ~1hr, second rep ~1day, third+ uses ease multiplication, ease unchanged
- HARD: decreases ease by 0.15, uses 1.2× interval growth
- EASY: increases ease by 0.15, first rep jumps to 1 day, uses ease×1.3
- All grades set lastReviewEpochMs and lastGrade
- `calculateMastery`: empty=0, well-known>0.7, fresh<0.5, lapse penalty
- `buildReviewQueue`: filters future items, sorts by overdue, tiebreaks by lapses
- `buildInterleavedSession`: interleaves sides, respects maxItems, returns all if under limit

---

## Phase 7 — Data Model Summary

### New entities (5)
| Entity | Primary Key | Purpose |
|--------|-------------|---------|
| `NodeReviewStateEntity` | nodeId | Per-node SRS state (ease, interval, reps, lapses) |
| `MistakeItemEntity` | auto-id | Missed moves queued for review |
| `ExamResultEntity` | auto-id | Exam attempt records |
| `StudySessionEntity` | auto-id | Time-tracked study sessions |
| `BadgeEntity` | id | Achievements / certificates |

### New DAOs (5)
`NodeReviewStateDao`, `MistakeItemDao`, `ExamResultDao`, `StudySessionDao`, `BadgeDao`

### New repositories (5)
`NodeReviewStateRepository`, `MistakeRepository`, `ExamResultRepository`, `StudySessionRepository`, `BadgeRepository`

### New use cases (12)
`ReviewNodeUseCase`, `GetReviewQueueUseCase`, `GetMistakesUseCase`, `ResolveMistakeUseCase`,
`RecordExamResultUseCase`, `GetExamResultsUseCase`, `TrackStudySessionUseCase`,
`GetStudySessionsUseCase`, `GetMasteryUseCase`, `GetBadgesUseCase`,
`GetSettingsUseCase`, `UpdateSettingsUseCase` (enhanced)

### New feature modules (3)
`feature/learn`, `feature/mistakereview`, `feature/quick5`

---

## Truthfulness commitments

The app makes no unsupported claims. All UI text follows the claims manifest:
- No "become GM" or "guaranteed wins" language
- Proof results are shown with statistical status and confidence levels
- Disclaimers are always visible on the home screen
- Mastery percentages reflect actual SRS data, not invented scores

---

## Architecture

```
app (navigation shell)
├── core/model         — data classes, enums
├── core/database      — Room entities, DAOs, DB
├── core/data          — repositories (entity ↔ model mapping)
├── core/domain        — use cases, ReviewScheduler
├── core/content       — JSON content loading
├── core/designsystem  — Compose components, theme
├── core/engine        — UCI engine integration
└── feature/
    ├── home           — dashboard + quick actions
    ├── repertoire     — browse lines
    ├── drill          — recall drills + Woodpecker mode
    ├── learn          — move-by-move guided study (NEW)
    ├── plans          — plans & strategic themes
    ├── deviations     — opponent deviation cards
    ├── modelgames     — annotated model games
    ├── exam           — timed repertoire exam
    ├── mistakereview  — mistake flashcard review (NEW)
    ├── quick5         — 5-minute daily session (NEW)
    ├── progress       — mastery, streaks, badges, exams
    └── settings       — skill level, dark mode, etc.
```
