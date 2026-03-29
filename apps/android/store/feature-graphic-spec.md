# Feature Graphic Specification

## Dimensions
- 1024 × 500 px (required by Google Play)

## Layout
- **Background**: Dark gradient (use `BoardDark` #6B4226 to `Surface` #1C1B1F)
- **Left side**: Chess board showing the Vienna Gambit exit position (FEN: `rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10`)
- **Right side**: Text overlay

## Text (from claims_manifest.json)
- **Headline**: "CounterLine" (large, white)
- **Subtitle**: "An Engine-Tested Opening Repertoire" (from `approved_headline`)
- **Tagline**: "Two precise opening lines — validated against Stockfish 18" (from `approved_subtitle`)

## Badges to overlay (from `approved_badges`)
- "Engine-Tested Repertoire"
- "Open Source (GPL-3.0)"

## Colors
- Primary text: White (#FFFFFF)
- Badge background: CounterLine Green (#2E7D32)
- Board squares: BoardLight (#F0D9B5) / BoardDark (#B58863)

## Typography
- Headline: Sans-serif bold, 48pt
- Subtitle: Sans-serif regular, 24pt
- Tagline: Sans-serif light, 18pt

## Do NOT include
- Any claim not in `claims_manifest.json`
- Phrases from the `forbidden_phrases` list
- Elo numbers or win percentages (save for the detailed description)

## Production note
Generate using Figma, Canva, or programmatically from the spec above.
Ensure text passes WCAG contrast ratio on the background.
