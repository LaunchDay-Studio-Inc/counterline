# Signing Documentation — CounterLine Android

## Versioning Strategy

CounterLine uses semantic versioning: `MAJOR.MINOR.PATCH`

- **versionName**: Human-readable version string (e.g., `1.0.0`)
- **versionCode**: Integer for Play Store ordering (e.g., `1`)

### Version bump rules
| Change type | versionName | versionCode |
|---|---|---|
| Bug fix | 1.0.1 | 2 |
| New feature / content update | 1.1.0 | 3 |
| Breaking change / major redesign | 2.0.0 | 4+ |

Version is set in `apps/android/app/build.gradle.kts` → `defaultConfig`.

## Signing Configuration

### Environment Variables (required for release builds)

| Variable | Description |
|---|---|
| `COUNTERLINE_KEYSTORE_FILE` | Absolute path to the `.jks` keystore file |
| `COUNTERLINE_KEYSTORE_PASSWORD` | Password for the keystore |
| `COUNTERLINE_KEY_ALIAS` | Key alias within the keystore |
| `COUNTERLINE_KEY_PASSWORD` | Password for the key alias |

### CI/CD Secrets (GitHub Actions)

| Secret | Description |
|---|---|
| `COUNTERLINE_KEYSTORE_BASE64` | Base64-encoded `.jks` file |
| `COUNTERLINE_KEYSTORE_PASSWORD` | Keystore password |
| `COUNTERLINE_KEY_ALIAS` | Key alias |
| `COUNTERLINE_KEY_PASSWORD` | Key password |

### Generating a new keystore

```bash
keytool -genkeypair \
  -alias counterline \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore counterline-release.jks \
  -dname "CN=CounterLine,O=CounterLine,L=Open Source,C=XX"
```

### Encoding for CI

```bash
base64 -w0 counterline-release.jks | pbcopy  # macOS
base64 -w0 counterline-release.jks > keystore.b64  # Linux
```

Then set as `COUNTERLINE_KEYSTORE_BASE64` in GitHub Actions secrets.

### Debug builds

Debug builds use Android's default debug keystore. No signing configuration needed.

### Release builds without signing

If `COUNTERLINE_KEYSTORE_FILE` is not set, the release build falls back to the debug signing config. This produces a working APK for testing but is not suitable for Play Store upload.

## Artifact Paths

| Artifact | Path |
|---|---|
| Debug APK | `apps/android/app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `apps/android/app/build/outputs/apk/release/app-release.apk` |
| Release AAB | `apps/android/app/build/outputs/bundle/release/app-release.aab` |

## Build Commands

```bash
# Generate content assets (required before any build)
cd apps/android
python3 scripts/extract_content.py

# Debug APK
./gradlew assembleDebug

# Release APK (requires signing env vars)
./gradlew assembleRelease

# Release AAB for Play Store (requires signing env vars)
./gradlew bundleRelease

# Run tests
./gradlew testDebugUnitTest
```

## Security Notes

- **Never commit** the keystore file or passwords to source control
- The `.gitignore` should exclude `*.jks` and `*.keystore`
- CI/CD secrets are encrypted at rest in GitHub Actions
- The release workflow cleans up the decoded keystore after use
- Play Store upload keys should use Google Play App Signing for additional protection
