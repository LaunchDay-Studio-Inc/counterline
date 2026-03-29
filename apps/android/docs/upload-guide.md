# Upload Guide — CounterLine Android

## Prerequisites

1. Android SDK with build tools installed (or use the dev container)
2. JDK 17
3. Python 3 (for content asset generation)
4. Keystore file for release signing (see `docs/signing.md`)

## Step 1: Generate Content Assets

```bash
cd apps/android
python3 scripts/extract_content.py
```

This reads from:
- `content/claims_manifest.json`
- `content/proof_manifest.json`
- `content/repertoire_manifest.json`

And writes to: `app/src/main/assets/content/`

## Step 2: Build Artifacts

### Release APK (for direct install / sideloading)

```bash
export COUNTERLINE_KEYSTORE_FILE=/path/to/counterline-release.jks
export COUNTERLINE_KEYSTORE_PASSWORD=your_keystore_password
export COUNTERLINE_KEY_ALIAS=counterline
export COUNTERLINE_KEY_PASSWORD=your_key_password

./gradlew assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release.apk`

### Release AAB (for Google Play)

```bash
# Same environment variables as above
./gradlew bundleRelease
```

**Output:** `app/build/outputs/bundle/release/app-release.aab`

## Step 3: Verify the Build

```bash
# Run tests
./gradlew testReleaseUnitTest

# Verify APK signature (requires Android build-tools on PATH)
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

## Step 4a: Direct Install (Sideload)

1. Transfer `app-release.apk` to the Android device
2. On the device, open the APK file
3. If prompted, enable "Install from unknown sources" for the file manager
4. Install and launch

**No Google Play account required.**

## Step 4b: Google Play Upload

### First-time setup

1. Go to [Google Play Console](https://play.google.com/console)
2. Create a new app:
   - App name: **CounterLine**
   - Default language: English (US)
   - App type: App
   - Free / Paid: Free
   - Category: Education
   - Tags: Chess, Board Games, Learning

### Store listing

Upload the following from `apps/android/store/`:

| Field | Source file |
|---|---|
| Short description | `short-description.txt` |
| Full description | `full-description.txt` |
| Screenshots | Generate per `screenshot-shotlist.md` |
| Feature graphic | Generate per `feature-graphic-spec.md` |
| Privacy policy URL | Host `privacy-policy.md` and link to it |

### Content rating

Complete the IARC questionnaire using answers from `store/content-rating-notes.md`.

### Data safety

Complete the data safety form using answers from `store/data-safety-notes.md`.

### Release

1. Go to **Production** → **Create new release**
2. Upload `app-release.aab`
3. Add release notes:
   ```
   CounterLine v1.0.0
   - Two engine-tested opening lines (Vienna Gambit + Caro-Kann Classical)
   - Spaced-repetition drills
   - Annotated model games
   - Plans and deviation handling
   - On-device Stockfish engine analysis
   - Full offline operation
   ```
4. Review and roll out

## Step 5: Tag the Release

```bash
git tag -a v1.0.0 -m "CounterLine Android v1.0.0"
git push origin v1.0.0
```

This triggers the `android-release.yml` GitHub Actions workflow, which:
- Builds release APK and AAB
- Runs tests
- Creates a GitHub Release with artifacts
- Attaches store metadata

## Artifact Locations

| Artifact | Local path | CI artifact name |
|---|---|---|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | `counterline-debug` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` | `counterline-release-apk` |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | `counterline-release-aab` |
| Store metadata | `apps/android/store/` | `counterline-store-metadata` |

## Required Listing Assets

| Asset | Status | Notes |
|---|---|---|
| Short description | Ready | `store/short-description.txt` |
| Full description | Ready | `store/full-description.txt` |
| Feature graphic (1024×500) | Spec ready | Generate from `store/feature-graphic-spec.md` |
| Screenshots (min 4) | Spec ready | Generate from `store/screenshot-shotlist.md` |
| Privacy policy | Ready | `store/privacy-policy.md` — host publicly |
| Content rating | Ready | `store/content-rating-notes.md` |
| Data safety | Ready | `store/data-safety-notes.md` |

## Currently Approved Claim Text

**Headline:** CounterLine: An Engine-Tested Opening Repertoire

**Subtitle:** Two precise opening lines — validated against Stockfish 18 on a published fixed suite

**Source:** `content/claims_manifest.json` → `approved_headline` / `approved_subtitle`

All in-app and store text must conform to `claims_manifest.json`. No hand-written marketing copy may override the claim boundaries. See `forbidden_phrases` in the manifest for prohibited language.
