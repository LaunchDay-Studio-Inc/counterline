# Content Pipeline

**Date:** 2026-03-30

## Overview

Content flows from repo-level manifests through an extraction script into
bundled Android asset JSON files, which are loaded at app startup into the
Room database.

```
content/claims_manifest.json     ─┐
content/proof_manifest.json      ─┤  scripts/extract_content.py
content/repertoire_manifest.json ─┘        │
                                           ▼
      app/src/main/assets/content/
        ├── claims.json         (3.4 KB)
        ├── proof.json          (3.5 KB)
        ├── repertoire.json     (6.6 KB)
        ├── plans.json          (1.5 KB)
        ├── themes.json         (1.0 KB)
        ├── deviations.json     (1.2 KB)
        ├── model_games.json    (2.4 KB)
        ├── drills.json         (5.1 KB)
        └── quick_starts.json   (1.9 KB)
                 │
                 ▼
      ContentValidator  →  validates all 9 assets exist and parse
      ContentAssetLoader →  deserializes into domain models
      ContentSeeder      →  bulk-inserts into Room DB on first launch
```

## Regenerating Content

From the repo root:

```bash
cd apps/android && python3 scripts/extract_content.py
```

Or via Gradle:

```bash
cd apps/android && ./gradlew :app:extractContent
```

## Startup Validation

`ContentValidator` runs before `ContentSeeder`. If any asset is:
- **Missing** — logged as error, shown to user on error screen
- **Empty or non-JSON** — logged as corrupt, shown to user

The app will not attempt database seeding with corrupt or missing content.

## Adding New Content

1. Update the relevant manifest in `content/`
2. If adding a new asset type, add its generation to `scripts/extract_content.py`
3. Add the file to `ContentAssetLoader`
4. Add the asset path to `ContentValidator.REQUIRED_ASSETS`
5. Run `python3 scripts/extract_content.py`
6. Verify with `python3 -c "import json; json.load(open('app/src/main/assets/content/NEW_FILE.json'))"`
