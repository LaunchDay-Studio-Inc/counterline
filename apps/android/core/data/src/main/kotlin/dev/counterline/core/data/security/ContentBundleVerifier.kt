package dev.counterline.core.data.security

/**
 * Specification for signed content-bundle verification.
 *
 * Not yet implemented — CounterLine is currently fully offline with all content
 * bundled in the APK. This interface defines the contract for when OTA content
 * updates are added in the future.
 *
 * Security requirements:
 * 1. Bundles must be signed with Ed25519 or RSA-PSS (min 2048-bit).
 * 2. The public verification key is embedded in the APK at build time.
 * 3. Bundle format includes: version, schema hash, signed manifest, payload.
 * 4. Rollback protection: refuse bundles older than the currently installed version.
 * 5. Signature verification must complete before any content is read or applied.
 * 6. If fetched over network, certificate pinning is mandatory.
 */
interface ContentBundleVerifier {

    /**
     * Verifies the integrity and authenticity of a content bundle.
     *
     * @param bundleBytes The raw bundle file contents.
     * @return [BundleVerificationResult.Valid] with metadata if verification passes,
     *         or [BundleVerificationResult.Invalid] with the reason for failure.
     */
    fun verify(bundleBytes: ByteArray): BundleVerificationResult

    sealed class BundleVerificationResult {
        data class Valid(
            val version: Int,
            val schemaHash: String,
            val contentCount: Int,
        ) : BundleVerificationResult()

        data class Invalid(val reason: String) : BundleVerificationResult()
    }
}
