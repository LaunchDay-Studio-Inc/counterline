package dev.counterline.core.data.security

/**
 * Play Integrity evaluation for CounterLine.
 *
 * # Assessment
 *
 * CounterLine is an offline chess training app with no accounts, no payments,
 * and no competitive multiplayer. Play Integrity provides three signal levels:
 *
 * 1. **Device integrity** — is the device genuine and unrooted?
 * 2. **App integrity** — was the APK installed from the Play Store?
 * 3. **Account integrity** — does the user have a licensed Google account?
 *
 * # Where Play Integrity helps
 *
 * - **Release verification**: On first launch, optionally verify the app was
 *   installed from the Play Store. This helps detect repackaged APKs and
 *   alerts users if they are running a potentially tampered copy.
 *
 * - **Protected update channels**: If OTA content updates are added later,
 *   integrity signals could gate who receives updates (only genuine installs).
 *
 * - **Anti-tamper signals**: Basic tamper detection for users who want
 *   confidence that their study data and engine analysis are from the
 *   genuine app.
 *
 * # Where Play Integrity does NOT help
 *
 * - Core study functionality must never depend on Play Integrity. The app
 *   is GPL-3.0 and must work when sideloaded, on devices without Play
 *   Services, or in regions where Play is unavailable.
 *
 * - Engine analysis results are local and have no competitive value that
 *   needs server-side attestation.
 *
 * # Decision
 *
 * Play Integrity integration is **deferred**. The current threat model does
 * not justify the complexity. If repackaging becomes a real problem, the
 * integration point is defined below.
 *
 * # Integration point (for future use)
 *
 * If enabled, Play Integrity would be checked:
 * - Once on first launch (non-blocking, result cached)
 * - Before applying OTA content updates (blocking)
 * - Never for core study, drills, or engine analysis
 */
object PlayIntegrityPolicy {
    /**
     * Whether Play Integrity checks are enabled.
     * Default: false. Enable only if repackaging becomes a real distribution problem.
     */
    const val ENABLED = false

    /**
     * Whether a failed integrity check blocks app usage.
     * Must always be false — core study must work without Play Services.
     */
    const val BLOCKS_CORE_FUNCTIONALITY = false
}
