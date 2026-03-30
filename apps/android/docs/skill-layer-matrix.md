# Skill-Layer Matrix: CounterLine

This document defines exactly what each skill level controls across every dimension of the app.

---

## 1  Content Visibility

| Dimension | Intermediate (1400–1700) | Advanced Club (1700–2000) | Expert / Master (2000–2300) | Elite Lab (2300+) |
|-----------|:------------------------|:-------------------------|:---------------------------|:------------------|
| **Mainlines** | Core mainline only | Full mainlines | Full mainlines | Full mainlines + novelty candidates |
| **Sidelines** | 2–3 critical sidelines per weapon | All common sidelines | All sidelines incl. rare | All + engine-only branches |
| **Deviations shown** | Top 3 most common deviations | Top 8 deviations | All deviations with >1% occurrence | All deviations including theoretical novelties |
| **Plans** | 1–2 key plans per line | All practical plans | All plans + subtle nuances | All plans + computer-generated ideas |
| **Model games** | 2 flagship games per weapon | 5+ annotated games | Full model game library | Full + raw engine analysis games |
| **Tactical motifs** | Basic motifs (pins, forks) | Intermediate motifs | Advanced motifs (exchange sacs, positional sacrifices) | Full tactical database |
| **Endgame tendencies** | Hidden | Basic endgame types mentioned | Full endgame plan per variation | Endgame tabiya positions with engine analysis |

---

## 2  Drill Configuration

| Dimension | Intermediate | Advanced Club | Expert / Master | Elite Lab |
|-----------|:------------|:-------------|:---------------|:----------|
| **Drill types available** | FILL_IN_BLANK, CHOOSE_MOVE | + FEN_RECOGNITION, PLANS_QUIZ | + DEVIATION_RESPONSE, TRANSITION_QUIZ, STRUCTURE_FLASHCARD | + COMPARE_POSITION, TACTICAL_MOTIF (full set) |
| **Options shown (MCQ)** | 3 choices | 4 choices | 5 choices or free entry | Free entry only |
| **Hints available** | First letter of SAN | Square highlight only | No hints | No hints + no board (Blindfold mode) |
| **Time pressure** | None | Optional gentle timer (30s) | Standard timer (15s) | Speed mode (8s) |
| **Woodpecker cycles** | Not available | 3-cycle | 5-cycle | 7-cycle with time decay |
| **Interleaving** | Mild (same weapon, mixed lines) | Moderate (both weapons) | Full interleaving | Full + deliberate interference patterns |

---

## 3  Engine Analysis

| Dimension | Intermediate | Advanced Club | Expert / Master | Elite Lab |
|-----------|:------------|:-------------|:---------------|:----------|
| **Eval display** | Eval bar (+/=/-) | Numerical eval (centipawns) | Numerical eval + trend arrow | Raw centipawns + WDL probabilities |
| **Best move comparison** | Hidden | Available on request | Always shown | Always shown + continuation lines |
| **MultiPV** | Not available | Not available | 2-line MultiPV | Up to 5-line MultiPV |
| **Depth control** | Fixed (depth 15) | Fixed (depth 20) | Adjustable (15–30) | Adjustable (15–40) + infinite mode |
| **"Why not this move?"** | Not available | Available (simple explanation) | Available (full refutation line) | Available (full tree with engine annotations) |
| **Engine sparring** | ~1200 Elo | ~1200–1600 Elo | ~1200–2000 Elo | ~1200–2400 Elo |

---

## 4  Explanation Depth

| Dimension | Intermediate | Advanced Club | Expert / Master | Elite Lab |
|-----------|:------------|:-------------|:---------------|:----------|
| **Move explanation** | "{move} defends the {piece} and prepares {plan}." | "{move} maintains tension. Plan: {plan}; if {alt}, then {refutation}." | "{move} (={eval}). Key idea: {plan}. Compare {alt} which allows {line}." | "{move} {eval}. {alt}: {continuation} ({eval2})." |
| **Plan description** | 2–3 sentences, natural language | 1 paragraph with chess terminology | Concise, assumes pattern vocabulary | Minimal prose, raw analysis preferred |
| **Deviation response** | "They play {move}. Just play {response} and you're fine." | "After {move}, {response} is best because {reason}." | "{move} ({eval_shift}). {response} maintains the advantage: {continuation}." | "{move}: {response} ({eval}). Alternatives: {alt1} ({eval1}), {alt2} ({eval2})." |
| **Vocabulary level** | No jargon; "develop," "control the center," "protect" | Standard terms: fianchetto, initiative, prophylaxis | Full vocabulary: tabiya, novelty, transposition, Zugzwang | Abbreviations accepted: N/f3, B/d3, 0-0-0 |

---

## 5  Exam Configuration

| Dimension | Intermediate | Advanced Club | Expert / Master | Elite Lab |
|-----------|:------------|:-------------|:---------------|:----------|
| **Questions** | 15 (mainlines only) | 20 (mainlines + common deviations) | 25 (full repertoire) | 30 (full + rare deviations + plans) |
| **Time limit** | 20 min | 20 min | 25 min | 25 min |
| **Question types** | CHOOSE_MOVE only | CHOOSE_MOVE + FILL_IN_BLANK | + PLANS_QUIZ + DEVIATION_RESPONSE | + FEN_RECOGNITION + COMPARE_POSITION |
| **Pass threshold** | 60% | 70% | 75% | 80% |
| **Scoring** | Correct / Incorrect | + Partial credit for related moves | + Speed bonus | + Precision scoring (exact vs. adequate) |
| **Certificate text** | "Beginner Certificate" | "Club Player Certificate" | "Expert Certificate" | "Master Certificate" |

---

## 6  Progression Signals

The app suggests level promotion when ALL of the following are met:

| Condition | Intermediate → Advanced | Advanced → Expert | Expert → Elite |
|-----------|:----------------------|:-----------------|:--------------|
| **Mainline mastery** | ≥ 85% | ≥ 90% | ≥ 95% |
| **Exam pass** | ≥ 1 pass at current level | ≥ 2 passes at current level | ≥ 3 passes at current level |
| **Active streak** | ≥ 7 days | ≥ 14 days | ≥ 21 days |
| **Unresolved mistakes** | ≤ 5 | ≤ 3 | ≤ 2 |
| **Deviation accuracy** | Not required | ≥ 70% | ≥ 80% |

The user can always self-select any level, overriding the progression system.

---

## 7  UI Differences by Level

| Element | Intermediate | Advanced Club | Expert / Master | Elite Lab |
|---------|:------------|:-------------|:---------------|:----------|
| **Board annotations** | Arrows for key moves | Arrows + square highlights | Minimal arrows | Clean board (user configurable) |
| **Coach banner tone** | Encouraging, directive | Informative, suggestive | Concise, data-driven | Minimal, stats-only |
| **Heatmap granularity** | Per-line (coarse) | Per-branch | Per-node | Per-node + historical trend |
| **Default session size** | 10 items | 15 items | 20 items | 25 items |
| **Blindfold mode** | Locked | Locked | Available | Available + recommended |

---

*Document version: 1.0*  
*Last updated: 2026-03-30*
