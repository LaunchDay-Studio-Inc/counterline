package dev.counterline.core.data.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.crashReportingStore by preferencesDataStore(name = "crash_reporting")

/**
 * Manages opt-in crash reporting preferences.
 *
 * Privacy guarantees:
 * - Crash reporting is OFF by default.
 * - User must explicitly opt in via Settings.
 * - No crash data is sent until consent is granted.
 * - Chess-specific content (FEN strings, move sequences, repertoire data)
 *   is stripped from crash reports before transmission.
 * - No user identifiers are attached to crash reports.
 */
@Singleton
class CrashReportingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val OPTED_IN = booleanPreferencesKey("crash_reporting_opted_in")
    }

    val isOptedIn: Flow<Boolean> = context.crashReportingStore.data.map { prefs ->
        prefs[Keys.OPTED_IN] ?: false
    }

    suspend fun setOptedIn(optedIn: Boolean) {
        context.crashReportingStore.edit { it[Keys.OPTED_IN] = optedIn }
    }

    companion object {
        /**
         * Patterns that identify chess-specific content to strip from crash logs.
         * These are removed before any crash report is transmitted.
         */
        private val SENSITIVE_PATTERNS = listOf(
            // FEN strings (e.g., "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1")
            Regex("""[rnbqkpRNBQKP1-8/]{15,}\s+[wb]\s+[KQkq-]{1,4}\s+[a-h1-8-]{1,2}\s+\d+\s+\d+"""),
            // UCI move sequences (e.g., "e2e4 e7e5 g1f3")
            Regex("""(?:[a-h][1-8]){2}(?:\s+(?:[a-h][1-8]){2}){2,}"""),
            // PGN move text
            Regex("""\d+\.\s*[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8][+#]?"""),
        )

        /**
         * Sanitizes a crash message by removing chess-specific content.
         */
        fun sanitize(message: String): String {
            var result = message
            SENSITIVE_PATTERNS.forEach { pattern ->
                result = pattern.replace(result, "[chess-content-redacted]")
            }
            return result
        }
    }
}
