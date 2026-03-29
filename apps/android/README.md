# CounterLine Android App

Production-grade Android app for the CounterLine opening repertoire trainer.

## Architecture

```
apps/android/
├── app/                    # Application module (entry point, navigation, DI)
├── core/
│   ├── model/             # Shared data classes (@Serializable)
│   ├── database/          # Room database, entities, DAOs
│   ├── data/              # Repository implementations, DataStore
│   ├── domain/            # Use cases (business logic)
│   ├── content/           # Asset loading & database seeding
│   ├── designsystem/      # Material 3 theme, reusable Compose components
│   └── engine/            # Chess position logic (FEN parsing, move validation)
├── feature/
│   ├── home/              # Dashboard with quick starts, progress, proof summary
│   ├── repertoire/        # Line browser with board visualization
│   ├── drill/             # Spaced-repetition drill sessions
│   ├── plans/             # Plans & strategic themes
│   ├── deviations/        # Common opponent deviations & responses
│   ├── modelgames/        # Annotated engine-vs-engine games
│   ├── exam/              # 20-question exam mode
│   ├── progress/          # Training statistics dashboard
│   └── settings/          # App preferences
└── scripts/
    └── extract_content.py # Generates JSON assets from repo manifests
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (bottom bar + nested) |
| DI | Hilt |
| Database | Room (offline-first) |
| Preferences | DataStore |
| Serialization | kotlinx.serialization |
| Build | Gradle Version Catalog |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 |

## Content Pipeline

All app content is sourced from the repo manifests — never hardcoded:

1. `content/claims_manifest.json` → claims, badges, disclaimers
2. `content/repertoire_manifest.json` → opening lines, moves, FENs
3. `content/proof_manifest.json` → match results, statistics

The `scripts/extract_content.py` script reads these manifests and generates
9 JSON files under `app/src/main/assets/content/`. These are loaded into Room
on first launch by `ContentSeeder`.

```bash
python3 apps/android/scripts/extract_content.py
```

## Build

```bash
cd apps/android

# Generate content assets (required before first build)
python3 scripts/extract_content.py

# Debug build
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Release build (requires signing config env vars)
./gradlew assembleRelease
```

### Release Signing

Set these environment variables for release builds:

- `COUNTERLINE_KEYSTORE_FILE` — path to the keystore
- `COUNTERLINE_KEYSTORE_PASSWORD` — keystore password
- `COUNTERLINE_KEY_ALIAS` — key alias
- `COUNTERLINE_KEY_PASSWORD` — key password

## Data Flow

```
Manifests (JSON) → extract_content.py → Asset JSONs → ContentAssetLoader
    → ContentSeeder → Room Database → Repositories → Use Cases → ViewModels → Compose UI
```

User progress (drill results, streaks) is stored locally in Room and DataStore.
All repertoire content is bundled offline — no network required.

## Key Design Decisions

1. **Evidence-gated claims**: All marketing text comes from `claims_manifest.json`.
   Forbidden phrases are enforced. Proof status is always shown with proper caveats.

2. **Offline-first**: No network calls. All content is bundled as assets and
   loaded into Room on first launch.

3. **Multi-module**: Clean separation of concerns. Feature modules depend only
   on core modules, never on each other.

4. **Spaced repetition**: Drill progress uses a simple SRS algorithm that
   doubles the review interval with each correct answer.
