# Release Checklist — CounterLine Android

## Pre-Release

- [ ] Version bumped in `app/build.gradle.kts` (`versionCode` and `versionName`)
- [ ] Content assets regenerated: `python3 scripts/extract_content.py`
- [ ] All claims in-app sourced from `content/claims_manifest.json`
- [ ] No forbidden phrases present (grep for items in `forbidden_phrases` list)
- [ ] `content/claims_manifest.json` `last_updated` field current
- [ ] `content/repertoire_manifest.json` matches actual repertoire
- [ ] `content/proof_manifest.json` matches actual proof matrix results

## Build Verification

- [ ] Debug APK builds: `./gradlew assembleDebug`
- [ ] Release APK builds: `./gradlew assembleRelease`
- [ ] Release AAB builds: `./gradlew bundleRelease`
- [ ] Unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Lint passes: `./gradlew lintDebug`

## Functional Testing

- [ ] First launch shows onboarding flow
- [ ] Onboarding completes and navigates to Home
- [ ] Subsequent launches skip onboarding and go to Home
- [ ] Home screen displays correct headline from claims manifest
- [ ] Home screen displays correct badges from claims manifest
- [ ] Evidence summary shows correct proof data
- [ ] Required disclaimers are displayed
- [ ] Repertoire browser shows both lines (White: Vienna, Black: Caro-Kann)
- [ ] Chess board renders correctly for both exit FENs
- [ ] Drill session works (fill-in-blank, choose-move, flashcard)
- [ ] Spaced repetition scheduling works
- [ ] Plans screen shows plans for both sides
- [ ] Deviations screen shows deviations for both sides
- [ ] Model games screen shows annotated games
- [ ] Quick Start cards display memory hooks and key actions
- [ ] Exam mode starts and records results
- [ ] Progress screen shows study statistics
- [ ] Settings changes persist across app restart
- [ ] Skill level filter works for content visibility

## Engine Integration

- [ ] Stockfish engine initializes on Practice screen
- [ ] Engine provides position evaluation
- [ ] Engine session cleans up on screen exit
- [ ] App does not crash if engine fails to load

## Offline Operation

- [ ] App launches without network connectivity
- [ ] All training flows work offline
- [ ] No network error messages appear
- [ ] Content is fully available from bundled assets

## Accessibility

- [ ] TalkBack reads screen content logically
- [ ] All interactive elements have content descriptions
- [ ] Touch targets are at least 48dp
- [ ] Text contrast meets WCAG AA (4.5:1 for body, 3:1 for large)
- [ ] Dark mode renders correctly
- [ ] ChessBoard has content description for current position
- [ ] Navigation labels are descriptive

## Device Compatibility

- [ ] Phone portrait (standard flow)
- [ ] Phone landscape (content scrolls properly)
- [ ] Tablet portrait (layout scales)
- [ ] Tablet landscape (layout scales)
- [ ] Android 8.0 (API 26) minimum
- [ ] Android 14 (API 34) target

## Store Readiness

- [ ] Short description under 80 characters
- [ ] Full description under 4000 characters
- [ ] Privacy policy present at `store/privacy-policy.md`
- [ ] Content rating notes complete at `store/content-rating-notes.md`
- [ ] Data safety notes complete at `store/data-safety-notes.md`
- [ ] Feature graphic spec complete at `store/feature-graphic-spec.md`
- [ ] Screenshot shot list complete at `store/screenshot-shotlist.md`
- [ ] FAQ complete at `store/faq.md`

## Signing

- [ ] Keystore generated and securely stored
- [ ] CI/CD secrets configured in GitHub Actions
- [ ] Release APK is properly signed (verify with `apksigner verify`)
- [ ] AAB is properly signed

## Legal

- [ ] GPL-3.0 license present (`Copying.txt`)
- [ ] Stockfish attribution in app (Settings → Legal)
- [ ] Open-source license list in Settings
- [ ] Required disclaimers displayed in-app
- [ ] Privacy policy does not overclaim data practices

## Post-Release

- [ ] Tag created: `git tag -a v1.0.0 -m "Release v1.0.0"`
- [ ] Tag pushed: `git push origin v1.0.0`
- [ ] GitHub Release created with APK and AAB
- [ ] Play Console listing created (if applicable)
- [ ] Store screenshots uploaded
- [ ] Feature graphic uploaded
- [ ] Content rating questionnaire submitted
- [ ] Data safety form completed
