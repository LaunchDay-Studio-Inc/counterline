# Screenshot Shot List

All screenshots must display content derived from `content/claims_manifest.json`, `content/repertoire_manifest.json`, or `content/proof_manifest.json`. No fabricated claims.

## Required Screenshots (minimum 4, maximum 8)

### 1. Home Dashboard
- **What to show**: Home screen with daily progress card, quick action buttons, and the approved headline/subtitle from claims_manifest.json
- **Device**: Phone (portrait)
- **Key elements visible**: "CounterLine: An Engine-Tested Opening Repertoire" headline, daily drill progress bar, quick action list
- **Dark mode variant**: Yes

### 2. Repertoire Browser — White Line
- **What to show**: Vienna Gambit Accepted (C29) line with chess board showing exit position
- **Device**: Phone (portrait)
- **Key elements**: Chess board with exit FEN, move list with SAN notation, ECO badge "C29", evaluation "+1.7 to +2.0"
- **Content source**: `repertoire_manifest.json` → `current_repertoire.white`

### 3. Repertoire Browser — Black Line
- **What to show**: Caro-Kann Classical 4…Bf5 (B18) line with chess board
- **Device**: Phone (portrait)
- **Key elements**: Chess board with exit FEN, move list, ECO badge "B18", evaluation "+0.3"
- **Content source**: `repertoire_manifest.json` → `current_repertoire.black`

### 4. Drill Session
- **What to show**: Active drill with a fill-in-the-blank or choose-the-move question
- **Device**: Phone (portrait)
- **Key elements**: Question text, answer options, chess board with position, progress indicator

### 5. Plans & Patterns
- **What to show**: Post-opening plans for one side with strategic descriptions
- **Device**: Phone (portrait)
- **Key elements**: Plan cards with priority ordering, strategic descriptions

### 6. Evidence Summary
- **What to show**: Proof summary section from Home screen showing actual test results
- **Device**: Phone (portrait)
- **Key elements**: "52.50% against Stockfish 18", "LOS ~93%", status badge showing "Positive trend"
- **Content source**: `claims_manifest.json` → `proof_status` and `allowed_performance_statements`

### 7. Quick Start Card
- **What to show**: Quick Start card with memory hook, seed line, and exit position
- **Device**: Phone (portrait)
- **Key elements**: Memory hook text, three key actions, chess board with exit position

### 8. Settings Screen
- **What to show**: Settings with skill level selector, dark mode toggle, daily goal slider
- **Device**: Phone (portrait)
- **Key elements**: Skill level dropdown, theme selector, drill goal slider

## Tablet Screenshots (optional, if tablet layout is implemented)
- Same shots 1, 2, 4 in landscape on a 10" tablet frame

## Technical Requirements
- Resolution: 1080 × 1920 px minimum (phone), 1200 × 1920 px (7" tablet), 1600 × 2560 px (10" tablet)
- Format: PNG or JPEG
- No alpha/transparency
- No device frames in the screenshot itself (Play Console adds them)

## Forbidden in Screenshots
- Any phrase from `claims_manifest.json → forbidden_phrases`
- Fabricated win rates, Elo numbers, or user counts
- Claims beyond the scope defined in `scope_statement`
