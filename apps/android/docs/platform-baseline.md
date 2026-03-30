# Platform Baseline — Android SDK & Dependency Decisions

**Date:** 2026-03-30
**Branch:** `release/close-critical-blockers`

## SDK Levels

| Property    | Old | New | Rationale |
|-------------|-----|-----|-----------|
| compileSdk  | 34  | 35  | Required for Play Store new-app submissions from Aug 2025 |
| targetSdk   | 35  | 35  | Play compliance — same as compileSdk |
| minSdk      | 26  | 26  | Unchanged — Android 8.0, covers 97%+ of active devices |

## Dependency Versions (changed)

| Dependency         | Old        | New        | Rationale |
|--------------------|-----------|------------|-----------|
| AGP                | 8.5.2     | 8.7.3     | Full SDK 35 support, latest stable AGP |
| Compose BOM        | 2024.09.03| 2024.12.01| Latest stable BOM with SDK 35 compat |
| core-ktx           | 1.13.1    | 1.15.0    | SDK 35 API surface |
| lifecycle          | 2.8.6     | 2.8.7     | Minor bugfixes |
| activity-compose   | 1.9.2     | 1.9.3     | SDK 35 edge-to-edge defaults |
| navigation-compose | 2.8.2     | 2.8.5     | Minor bugfixes |

## Dependency Versions (unchanged)

| Dependency          | Version    | Status |
|---------------------|-----------|--------|
| Kotlin              | 2.0.20   | Compatible with AGP 8.7.x |
| KSP                 | 2.0.20-1.0.25 | Matched to Kotlin |
| Room                | 2.6.1    | Stable, SDK 35 compatible |
| Hilt                | 2.52     | Stable |
| DataStore           | 1.1.1    | Stable |
| WorkManager         | 2.9.1    | Stable |
| Coroutines          | 1.9.0    | Stable |
| Serialization       | 1.7.3    | Stable |
| Gradle wrapper      | 8.9      | Compatible with AGP 8.7.x (requires 8.7+) |

## Compose Compiler Compatibility

Kotlin 2.0.20 includes the Compose compiler plugin (`kotlin.plugin.compose`).
No separate compose-compiler artifact is needed with Kotlin 2.0+.

## JDK Target

All modules use `jvmTarget = "17"` and `JavaVersion.VERSION_17`.
