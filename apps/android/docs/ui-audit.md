# CounterLine Android — UI/UX Audit

**Date:** 2026-03-30
**Branch:** release/close-critical-blockers
**Auditor:** Design system review (automated)

---

## 1  Executive Summary

CounterLine's Android app is functionally complete and architecturally sound
(multi-module, Hilt DI, Compose, MVVM). However the UI layer sits at
"engineering prototype" fidelity — it works, but it does not yet feel like the
**premium training instrument** the product positioning demands.

The gap is not about missing features; it is about **surface quality**:
typography, spacing rhythm, motion, adaptive layouts, and micro-interactions
that together communicate "this tool is serious and trustworthy."

---

## 2  Design System Assessment

### 2.1  Color

| Aspect | Status | Notes |
|--------|--------|-------|
| Light scheme | ✅ Good | Warm board-brown primary is distinctive |
| Dark scheme | ✅ Good | Proper inverse roles |
| Dynamic color | ✅ Present | Android 12+ path exists |
| Chess-specific tokens | ⚠️ Partial | Board square colors exist; no arrow/annotation/eval-bar tokens |
| Surface tint layers | ❌ Missing | Only 1 surface level; M3 supports 5 elevation tones |
| Colorblind-safe board | ❌ Missing | No alternative board palette |

### 2.2  Typography

| Aspect | Status | Notes |
|--------|--------|-------|
| Type scale | ⚠️ Default M3 | No custom font family — feels generic |
| Move notation style | ✅ Present | Monospace `labelMedium` |
| Display/hero text | ❌ Unused | `displayLarge` defined but never used |
| Font scaling | ❌ Untested | No `maxLines`/`overflow` guards on scaled text |

### 2.3  Spacing & Rhythm

| Aspect | Status | Notes |
|--------|--------|-------|
| Grid system | ❌ Missing | Ad-hoc dp values (4, 8, 12, 16, 24, 32) with no named tokens |
| Consistent card padding | ⚠️ Inconsistent | Most cards use 16.dp but some use 12.dp, 24.dp |
| Section spacing | ⚠️ Ad-hoc | `spacedBy(8.dp)` vs `spacedBy(12.dp)` varies per screen |

### 2.4  Elevation & Shape

| Aspect | Status | Notes |
|--------|--------|-------|
| Card elevation | ⚠️ Inconsistent | 2.dp, 4.dp used arbitrarily |
| Custom shapes | ❌ Missing | Default M3 rounded corners |
| Board corner radius | ❌ Missing | Board Canvas has no clipping |

### 2.5  Motion & Animation

| Aspect | Status | Notes |
|--------|--------|-------|
| Screen transitions | ❌ None | Hard cuts between all routes |
| List item animations | ❌ None | Items appear without entrance |
| Board move animation | ❌ None | FEN swaps instantly |
| Progress fills | ❌ None | `LinearProgressIndicator` has no animateFloatAsState |
| Drill feedback | ❌ None | Correct/incorrect has no motion/haptic |

### 2.6  Iconography

| Aspect | Status | Notes |
|--------|--------|-------|
| Material icons | ✅ Present | Reasonable icon choices |
| Custom chess icons | ❌ Missing | Using Unicode symbols rendered in Canvas |
| Icon consistency | ⚠️ Mixed | Same icon (SportsEsports) used for two different actions on Home |

---

## 3  Screen-by-Screen Audit

### 3.1  Home Screen  — **Weak**

**Issues:**
- Quick Actions are a vertical wall of identical `OutlinedButton` — no visual hierarchy
- 10 buttons of equal weight fights the "two weapons" focus
- Daily progress card could be a radial/ring instead of linear bar
- Evidence Summary uses flat text — could be structured evidence card
- No greeting/time-of-day personalization
- No "continue where you left off" prompt
- Quick Start cards are text-heavy, board is small

**Opportunities:**
- Hero board preview with last-studied position
- Weapon summary cards (White / Black) with mastery rings
- Featured action based on SRS state ("3 drills due")
- Subtle parallax or fade-in on load

### 3.2  Drill Screen  — **Functional but flat**

**Issues:**
- No entrance animation for drill card
- Grading buttons (Again/Hard/Good/Easy) are small, same-height — hard to tap one-handed
- No haptic on correct/incorrect
- Score bar is plain text — no ring or animated counter
- Session complete card has no celebration moment
- Board at 70% width — wastes space on phones

**Opportunities:**
- Card flip animation for reveal
- Haptic pulse patterns (short = correct, double = wrong)
- Confetti or subtle checkmark animation on session complete
- Swipe gesture for next (as alternative to button)

### 3.3  Repertoire / Line Explorer  — **Usable but cramped**

**Issues:**
- No list-detail split on tablets
- Selected line detail is inline AnimatedVisibility — breaks scroll position
- Board at 70% width in detail — should be larger
- Move list is plain text with no interactivity
- No search/filter by line name

**Opportunities:**
- Two-pane layout on expanded screens
- Tap-to-step-through moves on the board
- Move tree visualization
- Breadcrumb trail showing current variation depth

### 3.4  Progress Dashboard  — **Data-heavy, visualization-light**

**Issues:**
- Stat cards are small text boxes — no visual weight
- Mastery/accuracy use `LinearProgressIndicator` — boring
- No charts (accuracy over time, drills per day)
- Weakest Areas is a flat list of node IDs — not human-readable
- No visual streak representation (calendar heat map)
- Badges are plain `StatusBadge` text chips

**Opportunities:**
- Circular mastery rings (White + Black)
- 7-day activity sparkline
- Calendar heat map for streak
- Badge icons with earned/locked states

### 3.5  Settings / About  — **Functional**

**Issues:**
- All sections in one flat scroll — no grouping
- No board theme preview
- About section is minimal
- No accessibility settings section

**Opportunities:**
- Grouped preference sections with dividers
- Board theme picker with live preview
- Font size preview
- Accessibility toggle group

### 3.6  Exam Mode  — **Clean but plain**

**Issues:**
- Progress bar is basic LinearProgressIndicator
- No timer/clock feel for exam pressure
- Results card has no visual celebration for high scores
- No per-question review after completion

**Opportunities:**
- Segmented progress bar (green/red per answered question)
- Score reveal animation
- Trophy/badge unlock on 90%+

### 3.7  Practice vs Engine  — **Good structure, plain surface**

**Issues:**
- Board at 85% width — could be full-width on phones
- Mode/strength chips crowd the selector
- Move history is dense text
- No visual distinction between "in repertoire" and "deviation" states

**Opportunities:**
- Full-width board with notation drawer below
- Glowing border when inside repertoire, dimmed when deviated
- Move-by-move replay after session

### 3.8  Onboarding  — **Best screen currently**

**Issues:**
- Page dots are basic circles — no animation
- Board previews are small (140.dp)
- Skill level descriptions could be more visual

**Opportunities:**
- Animated dot transition with sliding
- Lottie-style welcome animation (or Compose animated vector)
- Larger board preview area

---

## 4  Accessibility Risks

| Risk | Severity | Notes |
|------|----------|-------|
| ChessBoard has no per-square semantics | 🔴 High | TalkBack cannot describe position |
| Touch targets < 48dp on grade buttons | 🔴 High | Grade buttons are weight-distributed, may be < 48dp |
| No `contentDescription` on most icons | 🟡 Medium | Quick action icons use `null` |
| No `LiveRegion` for score updates | 🟡 Medium | Score changes not announced |
| No reduced-motion support | 🟡 Medium | Once animations are added, must respect preference |
| No font-scaling guards | 🟡 Medium | Long text could overflow on scaled fonts |
| Contrast untested on all themes | 🟡 Medium | Dynamic color could produce low-contrast combos |

---

## 5  Tablet / Foldable Gaps

| Gap | Impact |
|-----|--------|
| No `WindowSizeClass` detection | All screens render phone-only layout |
| No list-detail split | Repertoire, Model Games, Deviations are single-column only |
| No dual-pane drill | Board + move tree side by side impossible |
| Navigation bar on tablets | Bottom nav is wrong pattern for large screens — should be NavigationRail |
| Board aspect ratio fixed at 1:1 | Board should expand to fill available height on landscape |
| No foldable hinge awareness | Table-top posture not handled |

---

## 6  Priority Actions

1. **Design system tokens** — spacing, elevation, shape, motion tokens
2. **Custom typography** — chess-specific premium font pairing
3. **Home screen redesign** — hero moment + weapon cards + smart CTA
4. **Adaptive navigation** — NavigationRail for medium+, NavigationDrawer for expanded
5. **Adaptive layouts** — list-detail for repertoire/model games on tablets
6. **Board component upgrade** — shadows, rounded clip, move animation, accessibility
7. **Progress visualization** — circular rings, sparklines, heat map
8. **Micro-interactions** — haptics, animated progress, card transitions
9. **Accessibility pass** — TalkBack labels, touch targets, reduced motion, contrast
10. **Dark/light parity review** — ensure all custom tokens have dark equivalents
