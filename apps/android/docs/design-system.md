# CounterLine Design System

Internal style guide for the CounterLine Android app.

---

## Design Principles

1. **Instrument, not toy** — Premium feel for serious study. No gamification noise.
2. **Focus over features** — Every screen should have one clear purpose.
3. **Warmth & clarity** — Warm walnut and sage tones, generous spacing, readable type.
4. **Motion with purpose** — Subtle animations reinforce actions (celebrations, haptics) without distraction.
5. **Accessible by default** — 48dp touch targets, TalkBack support, reduced-motion respect.

---

## Color Tokens

### Material 3 Roles
| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| Primary | `#5D4037` (walnut) | `#D7CCC8` | CTAs, active states, links |
| Secondary | `#4A7C59` (sage) | `#A5D6A7` | Success, correct, mastered |
| Tertiary | `#37598A` (slate blue) | `#90CAF9` | Informational, deviation |
| Error | `#B71C1C` | `#EF9A9A` | Incorrect, mistakes |

### Chess-Specific (`CounterLineTheme.chessColors`)
| Token | Purpose |
|-------|---------|
| `boardLight` / `boardDark` | Chessboard square colors |
| `whiteWeapon` / `blackWeapon` | Weapon accent colors (Vienna = warm amber, Caro-Kann = cool steel) |
| `correctMove` / `incorrectMove` | Drill feedback |
| `lastMoveHighlight` | Board last-move overlay |
| `masteryNew` / `masteryLearning` / `masteryMastered` | 3-level mastery gradient |
| `streakActive` / `streakInactive` | Streak dot colors |
| `evalAdvantage` / `evalDisadvantage` | Eval bar fill |

---

## Typography

### Font Families
- **Display/Headlines**: `FontFamily.Serif` — warm, authoritative
- **Body/UI**: `FontFamily.Default` (sans-serif) — clean, readable
- **Notation**: `FontFamily.Monospace` — aligned move columns

### Extended Tokens (`CounterLineTheme.chessTypography`)
| Token | Size | Weight | Family | Usage |
|-------|------|--------|--------|-------|
| `statHero` | 40sp | Bold | Serif | Big dashboard numbers |
| `statCompact` | 20sp | SemiBold | Default | Inline stat values |
| `moveNotationLarge` | 18sp | Medium | Monospace | Move display cards |
| `moveInline` | 14sp | Normal | Monospace | Inline move sequences |
| `evaluation` | 16sp | SemiBold | Monospace | Engine eval display |

---

## Spacing (`Spacing`)

| Token | Value | Usage |
|-------|-------|-------|
| `xxs` | 4dp | Tight gaps (badge margins, dot spacing) |
| `xs` | 8dp | Standard list spacing, chip gaps |
| `sm` | 12dp | Card internal padding (compact) |
| `md` | 16dp | Primary page margins, card padding |
| `lg` | 24dp | Section separators, generous margins |
| `xl` | 32dp | Hero spacing |
| `xxl` | 40dp | Large separators |
| `xxxl` | 48dp | Onboarding hero spacing |

---

## Shapes

### Material Shapes (`MaterialTheme.shapes`)
- `extraSmall`: 4dp — tags, badges
- `small`: 8dp — inline cards, option chips
- `medium`: 12dp — standard cards
- `large`: 16dp — prominent cards, dialogs
- `extraLarge`: 24dp — bottom sheets

### Chess Shapes (`ChessShapes`)
| Token | Value | Usage |
|-------|-------|-------|
| `board` | 8dp | Chessboard corners |
| `weaponCard` | 20dp | Hero weapon cards |
| `statCard` | 16dp | Stat hero cards |
| `drillOption` | 12dp | Answer option cards |
| `bottomSheet` | 24dp top only | Bottom sheets |
| `badge` | 50% | Circular badges |

---

## Elevation (`Elevation`)

| Token | Value | Usage |
|-------|-------|-------|
| `none` | 0dp | Flat surfaces |
| `low` | 1dp | Subtle cards |
| `medium` | 3dp | Standard cards |
| `high` | 6dp | Floating elements |
| `highest` | 8dp | Dialogs, overlays |

---

## Motion (`Motion`)

### Durations
| Token | Value | Usage |
|-------|-------|-------|
| `MICRO` | 100ms | Haptic feedback timing |
| `SHORT` | 200ms | Chip select, toggle |
| `MEDIUM` | 350ms | Card transitions, FadeSlideUp |
| `LONG` | 500ms | Page transitions |
| `EXTENDED` | 800ms | Celebration pop |

### Spring Specs
- `springBouncy` — celebration animations
- `springGentle` — layout transitions
- `springSnappy` — UI feedback

---

## Components

### ProgressRing
Animated circular progress indicator with center label/sublabel.
- `ProgressRing(progress, size, strokeWidth, label, sublabel, progressColor)` — large, for dashboards
- `MiniProgressRing(progress, size)` — compact inline use (40dp default)

### WeaponCard
Opening repertoire summary card with accent color, mastery ring, and due count.

### StatHeroCard
Dashboard stat with large number, label, optional icon, and accent color.

### StreakIndicator
7-day dot row showing study streak with animated active/inactive colors.

### MasteryBar
Horizontal bar with animated fill and color-coded mastery levels.

### EvalBar
Vertical advantage indicator with animated white/black ratio.

---

## Adaptive Layout

### Window Size Classes
- **Compact** (<600dp): Single column, bottom navigation bar
- **Medium** (600–839dp): Side NavigationRail, optional list-detail
- **Expanded** (≥840dp): Full NavigationRail, side-by-side board+controls

### Key Composables
- `AdaptiveNavigationLayout` — auto-switches between bottom bar and rail
- `ListDetailLayout` — single vs split pane based on width
- `BoardControlLayout` — stacked (portrait) vs side-by-side (landscape)

---

## Accessibility Checklist

- [ ] All interactive elements ≥ 48×48dp
- [ ] Icons with semantic meaning have `contentDescription`
- [ ] Score updates use `liveRegion = LiveRegionMode.Polite`
- [ ] Section headings use `Modifier.semantics { heading() }`
- [ ] Chessboard has TalkBack description via `describeFenForAccessibility()`
- [ ] Animations respect `rememberReducedMotion()`
- [ ] Haptic feedback uses `rememberHapticFeedback()` (graceful fallback)
