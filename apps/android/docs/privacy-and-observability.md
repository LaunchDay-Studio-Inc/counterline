# Privacy and Observability — CounterLine Android

**Version:** 1.0
**Date:** 2026-03-30

---

## Core Principle

CounterLine is privacy-first by design. The app collects **zero data by default**.
All telemetry, analytics, and crash reporting are **off** unless the user explicitly
opts in through in-app settings.

## Data Flow Summary

```
┌─────────────────────────────────────────────┐
│  User's Device                              │
│                                             │
│  ┌─────────────┐   ┌────────────────────┐   │
│  │ Study Data  │   │ User Preferences   │   │
│  │ (Room/SQLite│   │ (DataStore)        │   │
│  │ counterline │   │ settings.prefs     │   │
│  │ .db)        │   │                    │   │
│  └─────────────┘   └────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │ Stockfish Engine (JNI, in-process)  │    │
│  └─────────────────────────────────────┘    │
│                                             │
│                   ╱╲                        │
│                  NO NETWORK ACCESS           │
│                   ╲╱                        │
└─────────────────────────────────────────────┘
```

**No data leaves the device** in the default configuration.

## What Is Stored Locally

| Data | Storage | Sensitive? | User-controlled? |
|------|---------|-----------|------------------|
| Study progress (drill scores, streaks) | Room database | No | Yes — Clear Data |
| Spaced-repetition schedules | Room database | No | Yes — Clear Data |
| Exam results | Room database | No | Yes — Clear Data |
| Mistake history (positions) | Room database | No | Yes — Clear Data |
| User preferences (dark mode, skill level) | DataStore | No | Yes — Clear Data |
| Crash reporting consent | DataStore | No | Yes — Settings toggle |
| Repertoire content | APK assets (read-only) | No | N/A — bundled |

## Analytics

**CounterLine has no analytics.** There are no:
- Event tracking SDKs
- Usage analytics
- A/B testing frameworks
- Advertising identifiers
- Device fingerprinting

This is a deliberate design choice, not an oversight.

## Crash Reporting

### Default: OFF

Crash reporting is **disabled by default**. No crash data is collected or transmitted
until the user explicitly enables it.

### Opt-in Flow

1. User navigates to Settings → Privacy & Data.
2. User sees a clear explanation: "Help improve CounterLine by sharing anonymous crash
   reports. No chess content, study progress, or personal information is included."
3. User toggles "Share crash reports" ON.
4. Consent is stored locally in DataStore (`crash_reporting_opted_in`).

### Privacy Protections

When crash reporting is enabled:

| Protection | Implementation |
|-----------|----------------|
| Chess content stripping | FEN strings, UCI moves, and PGN notation are redacted from crash reports before transmission |
| No user identifiers | No device ID, advertising ID, or account information is attached |
| No study data | Crash reports never include drill scores, progress, or mistake history |
| No stack trace enrichment | Only standard Android crash data (exception type, stack trace, OS version, device model) |
| Local consent only | Consent state is stored on-device, never communicated to any server |

### Sanitization Rules

The `CrashReportingManager.sanitize()` function removes:
- FEN position strings (e.g., `rnbqkbnr/pppppppp/...`)
- UCI move sequences (e.g., `e2e4 e7e5 g1f3`)
- PGN move text (e.g., `1. e4 e5 2. Nf3`)

These are replaced with `[chess-content-redacted]` before any transmission.

### Crash Reporter Selection

No crash reporting SDK is currently bundled. When a provider is chosen:

**Requirements:**
- Must support opt-in only mode (no auto-collection)
- Must not collect device identifiers by default
- Must allow custom data scrubbing
- Must be compatible with GPL-3.0 (check licensing)
- Recommended: self-hosted (e.g., Sentry self-hosted) or privacy-respecting SaaS

**Explicitly banned:**
- Firebase Crashlytics (collects device identifiers, requires Google Play Services)
- Any SDK that auto-initializes via ContentProvider
- Any SDK with mandatory analytics bundled

## Encrypted Backup/Export

Users can export their study progress as an encrypted backup file.

| Property | Value |
|----------|-------|
| Algorithm | AES-256-GCM |
| Key storage | Android Keystore |
| Key scope | Per-device (not transferable) |
| File format | Custom binary (version-tagged) |
| Max import size | 50 MB |
| Tamper detection | GCM authentication tag |

The backup contains study progress and preferences only. It does **not** contain
repertoire content (which is bundled in the APK).

## Backup & Restore (OS-level)

Android Auto Backup is enabled for:
- `counterline.db` (study progress)
- `settings.preferences_pb` (user preferences)

This is standard OS behavior. Users can disable it in Android Settings.

Android 12+ uses `dataExtractionRules` for finer-grained control over cloud vs.
device-to-device transfers. WAL/journal files are excluded.

## Claims Manifest Compliance

All public-facing claims in the app are governed by `/content/claims_manifest.json`.
The privacy policy and data safety declarations are consistent with this manifest.

**Key claim:** "CounterLine does not collect, transmit, or share any personal data."

This claim is verifiable by:
1. Examining the source code (GPL-3.0)
2. Checking that no `INTERNET` permission is declared
3. Confirming no analytics SDKs in the dependency tree
4. Running the CI security audit (`.github/workflows/android-security.yml`)

## Regulatory Compliance Notes

| Regulation | Status |
|-----------|--------|
| GDPR | Compliant — no personal data processing |
| CCPA | Compliant — no data collection or sale |
| COPPA | Compliant — no data collection from anyone |
| Google Play Data Safety | Declared "no data collected" |
| Apple App Store (future) | N/A — Android only currently |
