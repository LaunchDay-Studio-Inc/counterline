package dev.counterline.core.content

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates that all required content assets are present and parseable
 * before the app attempts to seed the database.
 */
@Singleton
class ContentValidator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "ContentValidator"
        private val REQUIRED_ASSETS = listOf(
            "content/repertoire.json",
            "content/plans.json",
            "content/themes.json",
            "content/deviations.json",
            "content/model_games.json",
            "content/drills.json",
            "content/claims.json",
            "content/proof.json",
            "content/quick_starts.json",
        )
    }

    data class ValidationResult(
        val valid: Boolean,
        val missingAssets: List<String> = emptyList(),
        val corruptAssets: List<String> = emptyList(),
    ) {
        val errorMessage: String
            get() = buildString {
                if (missingAssets.isNotEmpty()) {
                    append("Missing assets: ${missingAssets.joinToString()}")
                }
                if (corruptAssets.isNotEmpty()) {
                    if (isNotEmpty()) append("; ")
                    append("Corrupt assets: ${corruptAssets.joinToString()}")
                }
            }
    }

    fun validate(): ValidationResult {
        val missing = mutableListOf<String>()
        val corrupt = mutableListOf<String>()

        for (asset in REQUIRED_ASSETS) {
            try {
                val content = context.assets.open(asset).bufferedReader().use { it.readText() }
                if (content.isBlank()) {
                    Log.e(TAG, "Asset is empty: $asset")
                    corrupt.add(asset)
                } else if (!content.trimStart().startsWith("[") && !content.trimStart().startsWith("{")) {
                    Log.e(TAG, "Asset is not valid JSON: $asset")
                    corrupt.add(asset)
                }
            } catch (e: java.io.FileNotFoundException) {
                Log.e(TAG, "Asset missing: $asset")
                missing.add(asset)
            } catch (e: Exception) {
                Log.e(TAG, "Asset read error: $asset", e)
                corrupt.add(asset)
            }
        }

        val valid = missing.isEmpty() && corrupt.isEmpty()
        if (valid) {
            Log.i(TAG, "All ${REQUIRED_ASSETS.size} content assets validated OK")
        }
        return ValidationResult(valid = valid, missingAssets = missing, corruptAssets = corrupt)
    }
}
