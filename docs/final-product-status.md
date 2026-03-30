# Final Product Status — CounterLine v1.0.0

**Date:** 2026-03-30
**Branch:** release/close-critical-blockers

## Deliverable Status

| Deliverable | Status | Location |
|---|---|---|
| Android APK (debug) | BUILD READY | `apps/android/` — `./gradlew assembleDebug` |
| Android APK (release) | BUILD READY | `apps/android/` — `./gradlew assembleRelease` (needs signing env vars) |
| Android AAB (Play) | BUILD READY | `apps/android/` — `./gradlew bundleRelease` (needs signing env vars) |
| Companion book (PDF) | DONE | `book/dist/CounterLine--An-Engine-Tested-Opening-Repertoire.pdf` (646 KB) |
| Companion book (EPUB) | DONE | `book/dist/CounterLine--An-Engine-Tested-Opening-Repertoire.epub` (644 KB) |
| Store metadata | DONE | `apps/android/store/` |
| Brand & copy guide | DONE | `apps/android/docs/brand-and-copy.md` |
| Upload guide | DONE | `apps/android/docs/upload-guide.md` |
| Release notes | DONE | `apps/android/docs/release-notes.md` |
| QA report | DONE | `apps/android/docs/qa-report.md` |
| Python wrapper tests | 34/34 PASS | `tests/` |
| Claims manifest | AUDITED | `content/claims_manifest.json` — all copy evidence-gated |

## Architecture Summary

- **7 core modules**: analytics, common, database, engine, model, testing, ui
- **21 feature modules**: blindfold, coach, deviation, drill, exam, explore, home, learn, mistakereview, modelgames, notebook, onboarding, pgnimport, plans, practice, preppack, progress, quick5, quickstart, settings, tacticalmotifs, transitiontrainer
- **App module**: Hilt-based DI, Jetpack Navigation, Material 3 Compose

## Phase 0 — Expert Study Features (COMPLETE)

| Feature | File(s) |
|---|---|
| Play-from-tabiya mode | `feature/practice/…/PracticeViewModel.kt`, `PracticeScreen.kt` |
| Elite analysis pane | `feature/practice/…/PracticeViewModel.kt`, `PracticeScreen.kt` |
| Compare-your-move | `feature/practice/…/PracticeViewModel.kt`, `PracticeScreen.kt` |
| Explain-last-move | `feature/practice/…/PracticeViewModel.kt`, `PracticeScreen.kt` |
| Export prep sheet | `feature/preppack/…/PrepPackViewModel.kt`, `PrepPackScreen.kt` |

## Phase 1 — Product Copy & Manifests (COMPLETE)

- `claims_manifest.json` audited — `last_updated` set to 2026-03-30
- Store `full-description.txt` strengthened with audience tiers and new features
- `brand-and-copy.md` created as single source of truth for all surfaces
- `release-notes.md` updated with all Phase 0 features
- `book/index.qmd` updated with "Who this book is for" audience tiers
- Forbidden phrase scan: CLEAN (no violations)

## Phase 2 — Release Artifacts (COMPLETE)

- 7 missing feature modules registered in `settings.gradle.kts` and `app/build.gradle.kts`
- 7 `build.gradle.kts` files created for: blindfold, coach, notebook, pgnimport, preppack, tacticalmotifs, transitiontrainer
- Book rendered: PDF (646 KB) + EPUB (644 KB)

## Phase 3 — QA Results (COMPLETE)

- Python tests: 34/34 pass (2.11 s)
- Forbidden phrase scan: no violations
- Accessibility audit: 83 annotations across feature modules
- Book render: both formats compile cleanly (cosmetic LaTeX warnings only)
- QA report updated at `apps/android/docs/qa-report.md`

## What Remains Before Play Store Upload

1. **Native Stockfish library** — Build ARM64 + x86_64 `.so` files via NDK cross-compile and place in `app/src/main/jniLibs/`
2. **Screenshots** — Generate 4+ PNGs from a running device per `store/screenshot-shotlist.md`
3. **Feature graphic** — Design 1024×500 graphic per `store/feature-graphic-spec.md`
4. **Signing keystore** — Create release keystore and set `COUNTERLINE_KEYSTORE_*` env vars (see `upload-guide.md`)
5. **CI secrets** — Add keystore secrets to GitHub Actions for automated release builds

## Approved Headline Copy

> **CounterLine: An Engine-Tested Opening Repertoire**
>
> Two precise opening lines — validated against Stockfish 18 on a published fixed suite

Source: `content/claims_manifest.json`

## Required Disclaimers

- "Results represent a positive trend; statistical significance (p < 0.05) has not been reached."
- "Results were obtained under controlled fixed-suite conditions and may not reflect real-game outcomes."
- "All engine evaluations are depth-limited and should not be treated as ground truth."

## Build Commands

```bash
# Content assets
cd apps/android && python3 scripts/extract_content.py

# Debug APK
./gradlew assembleDebug

# Release APK (requires signing env vars)
./gradlew assembleRelease

# Play Store AAB (requires signing env vars)
./gradlew bundleRelease

# Book
cd book && quarto render
```
