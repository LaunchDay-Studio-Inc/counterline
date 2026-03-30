# Threat Model — CounterLine Android

**Version:** 1.0
**Date:** 2026-03-30
**Scope:** CounterLine Android app (offline chess opening repertoire trainer)

---

## 1. Application Profile

| Property | Value |
|---|---|
| App type | Offline educational / chess trainer |
| Network access | None (no INTERNET permission) |
| User accounts | None |
| Payment | None |
| Sensitive data | User study progress, preferences, custom notes |
| Native code | JNI bridge to Stockfish engine (C++) |
| Target API | 35 (Android 15) |
| Min API | 26 (Android 8.0) |

## 2. Trust Boundaries

```
┌──────────────────────────────────────────┐
│  Play Store / Sideload                   │  ← APK integrity boundary
├──────────────────────────────────────────┤
│  Android OS / App Sandbox                │  ← OS-enforced isolation
├──────────────────────────────────────────┤
│  Kotlin/JVM layer                        │
│   ├── Jetpack Compose UI                 │
│   ├── Room database (SQLite)             │
│   ├── DataStore preferences              │
│   └── Content JSON assets                │
├──────────────────────────────────────────┤
│  JNI boundary                            │  ← Memory safety boundary
│   └── Stockfish C++ engine               │
├──────────────────────────────────────────┤
│  File system (import/export)             │  ← Data ingress/egress boundary
└──────────────────────────────────────────┘
```

## 3. Threat Catalog

### T1 — Local Content Tampering

**Description:** An attacker with root access or a malicious file manager modifies the app's bundled content JSON assets (repertoire lines, claims, plans) to inject misleading chess content or alter claims.

**Attack vector:** Root access, ADB, backup/restore manipulation.

**Impact:** Medium — corrupted study material; claims manifest integrity broken.

**Likelihood:** Low — requires device compromise.

**Mitigations:**
- Content JSON is shipped inside the APK as read-only assets.
- SHA-256 checksums of bundled content verified at app startup (Phase 2).
- Room database stores user progress separately from content; content is immutable after install.
- If future OTA content updates are added, bundles must be signature-verified (see T7).

**Residual risk:** Rooted devices can always modify app-private storage.

---

### T2 — APK Repackaging

**Description:** An attacker decompiles the APK, modifies it (inject malware, alter content, remove license compliance), re-signs with a different key, and distributes it.

**Attack vector:** Third-party app stores, direct APK sharing.

**Impact:** High — malware distribution under CounterLine name; GPL violations.

**Likelihood:** Medium — common for popular apps.

**Mitigations:**
- R8 minification and shrinking enabled for release builds.
- Play Integrity API can detect non-genuine installs (Phase 4, optional).
- Play Store App Signing provides an additional layer — Google holds the upload key.
- Signing key rotation documented; original key never committed to repo.
- Build reproducibility: users can verify APKs by building from source.

**Residual risk:** Cannot prevent repackaging on sideloaded installs; R8 is not a security boundary.

---

### T3 — Leaked Signing Material

**Description:** The release keystore file or its passwords are accidentally committed to the repository, exposed in CI logs, or leaked from a developer machine.

**Attack vector:** Git history, CI artifacts, insecure developer workstation.

**Impact:** Critical — attacker can sign and push malicious updates that appear genuine.

**Mitigations:**
- Keystore is never committed; `.gitignore` excludes `*.jks` and `*.keystore`.
- CI uses base64-encoded GitHub Secrets; keystore is decoded to `/tmp` and deleted after use.
- Environment variables used in `build.gradle.kts`, not hardcoded values.
- GitHub secret scanning enabled (Phase 3).
- Signing documentation recommends Play App Signing for key escrow.
- CI workflow verifies no secrets in build output (`secret-scan` job, Phase 3).

**Residual risk:** A compromised CI runner could exfiltrate secrets during the build.

---

### T4 — Debug/Release Configuration Mistakes

**Description:** A release build is shipped with `debuggable=true`, missing minification, or with the debug signing key, making it trivially inspectable and modifiable.

**Attack vector:** Misconfigured `build.gradle.kts`, skipped CI checks.

**Impact:** High — debuggable release APKs allow runtime inspection, patching, and memory dumping.

**Mitigations:**
- `debug` build type has `applicationIdSuffix = ".debug"` — cannot be confused with release.
- `release` build type enforces `isMinifyEnabled = true` and `isShrinkResources = true`.
- CI lint job added to verify release build flags (Phase 6).
- Gradle task `verifyReleaseBuildConfig` fails the build if debuggable or un-minified (Phase 1).
- No `android:debuggable` attribute in manifest (defaults to `false` for release).

**Residual risk:** A developer could locally build a misconfigured release; CI gate prevents merge.

---

### T5 — Exported Component Abuse

**Description:** An exported Activity, Service, or ContentProvider is invoked by a malicious app to trigger unintended behavior (data exfiltration, denial of service, privilege escalation).

**Attack vector:** Malicious app on the same device sends crafted intents.

**Impact:** Medium — depends on what the component exposes.

**Mitigations:**
- Only `MainActivity` is exported (required for launcher intent).
- All other components use `exported=false` explicitly (Phase 1).
- No ContentProviders, BroadcastReceivers, or Services are currently declared.
- If a FileProvider is added for export, it will use restrictive path configurations.
- CI audit script verifies no unintentionally exported components (Phase 6).

**Residual risk:** Future feature additions could introduce exported components; CI gate catches this.

---

### T6 — Unsafe File Import/Export

**Description:** A user imports a maliciously crafted backup file that exploits JSON/SQLite parsing vulnerabilities, causes SQL injection, or overwrites critical app data.

**Attack vector:** User downloads a "shared" backup from an untrusted source.

**Impact:** Medium — could corrupt study data or crash the app.

**Mitigations:**
- Import validates file structure, schema version, and data integrity before applying (Phase 2).
- Encrypted backup format uses authenticated encryption (AES-GCM) — tampering is detected.
- Database imports use parameterized queries (Room enforces this).
- File size limits prevent memory exhaustion attacks.
- Imported content is validated against expected types and ranges.
- No executable content (no WebView, no JavaScript, no dynamic code loading).

**Residual risk:** A valid-looking but semantically malicious import (e.g., 10 million fake drill records) could degrade performance.

---

### T7 — Future Content-Update Risks

**Description:** If OTA content updates are added later, an attacker could intercept or forge update bundles to inject malicious content.

**Attack vector:** Man-in-the-middle (if fetched over network), compromised update server, DNS hijack.

**Impact:** High — could silently replace repertoire content.

**Mitigations (design-time, for future implementation):**
- Content bundles must be signed with a key embedded in the APK at build time.
- Verification uses `Ed25519` or `RSA-PSS` signatures checked before any content is applied.
- Content bundle format includes version number, schema hash, and signed manifest.
- Rollback protection: app refuses bundles older than currently installed version.
- Certificate pinning required if content is fetched over HTTPS.
- Specification documented in `ContentBundleVerifier` interface (Phase 2).

**Current status:** Not implemented — app is fully offline. This spec exists so the security design is ready when needed.

---

### T8 — JNI Misuse / Native Crashes

**Description:** Bugs in the Stockfish JNI bridge could cause memory corruption, buffer overflows, use-after-free, or denial of service via native crashes.

**Attack vector:** Malformed FEN strings, race conditions in engine lifecycle, resource exhaustion.

**Impact:** Medium — app crash (DoS); memory corruption could theoretically lead to code execution, but this requires crafted input through the JNI boundary.

**Mitigations:**
- JNI bridge uses `std::mutex` for thread safety on all engine operations.
- Engine lifecycle is guarded by `g_initialized` atomic flag.
- FEN validation occurs in Stockfish's `set_position` (returns error on invalid input).
- JNI string conversions use `GetStringUTFChars`/`ReleaseStringUTFChars` correctly.
- Native code compiled with `-O3 -DNDEBUG` for release; no debug symbols in release `.so`.
- Search depth and move time are bounded by caller (no unbounded computation).
- `nativeDestroy` waits for search completion before releasing the engine.
- C++ exceptions are caught at the JNI boundary to prevent unwinding into JVM.

**Residual risk:** Stockfish itself is battle-tested but C++ memory bugs are always possible. AddressSanitizer builds are recommended for development.

---

## 4. Risk Summary Matrix

| ID | Threat | Severity | Likelihood | Risk | Mitigation Phase |
|----|--------|----------|------------|------|-------------------|
| T1 | Content tampering | Medium | Low | Low | Phase 2 |
| T2 | APK repackaging | High | Medium | Medium | Phase 1, 4 |
| T3 | Leaked signing material | Critical | Low | Medium | Phase 3 |
| T4 | Debug/release misconfiguration | High | Low | Medium | Phase 1, 6 |
| T5 | Exported component abuse | Medium | Low | Low | Phase 1, 6 |
| T6 | Unsafe file import/export | Medium | Medium | Medium | Phase 2 |
| T7 | Content-update attacks | High | N/A (future) | N/A | Phase 2 (design) |
| T8 | JNI misuse / native crash | Medium | Low | Low | Phase 1 |

## 5. Out of Scope

| Item | Reason |
|---|---|
| Network-based attacks (MITM, API abuse) | App has no network access |
| Authentication bypass | App has no accounts or auth |
| Server-side vulnerabilities | No server component |
| DDoS | No network endpoints |
| Payment fraud | No payments or in-app purchases |
| Clipboard/screenshot data leaks | No sensitive PII handled; chess positions are not confidential |
| Side-channel attacks on engine analysis | Engine is for education, not competitive secret analysis |

## 6. OWASP MASVS Mapping

| MASVS Category | Relevance | Status |
|---|---|---|
| MASVS-STORAGE | High — user progress is valuable | Phase 2 |
| MASVS-CRYPTO | Medium — encrypted export | Phase 2 |
| MASVS-AUTH | N/A — no authentication | — |
| MASVS-NETWORK | N/A — fully offline | — |
| MASVS-PLATFORM | High — component export, intent handling | Phase 1 |
| MASVS-CODE | High — R8, build config, native code | Phase 1, 3 |
| MASVS-RESILIENCE | Medium — anti-tamper for distribution | Phase 4 |
| MASVS-PRIVACY | High — no data collection by design | Phase 5 |
