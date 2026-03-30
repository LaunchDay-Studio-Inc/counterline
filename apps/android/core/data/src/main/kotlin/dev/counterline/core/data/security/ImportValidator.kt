package dev.counterline.core.data.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates imported backup data before it is applied to the database.
 * Prevents corrupt, oversized, or malformed imports from damaging app state.
 */
@Singleton
class ImportValidator @Inject constructor() {

    companion object {
        const val MAX_BACKUP_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB
        const val CURRENT_SCHEMA_VERSION = 2
        private val REQUIRED_KEYS = setOf("schema_version", "export_timestamp", "data")
    }

    /**
     * Validates the raw JSON backup string. Returns a [ValidationResult].
     */
    fun validate(jsonString: String): ValidationResult {
        if (jsonString.length > MAX_BACKUP_SIZE_BYTES) {
            return ValidationResult.Invalid("Backup exceeds maximum size limit")
        }

        val root: JsonObject = try {
            Json.parseToJsonElement(jsonString).jsonObject
        } catch (e: Exception) {
            return ValidationResult.Invalid("Invalid JSON format: ${e.message}")
        }

        // Check required keys
        val missingKeys = REQUIRED_KEYS - root.keys
        if (missingKeys.isNotEmpty()) {
            return ValidationResult.Invalid("Missing required keys: $missingKeys")
        }

        // Validate schema version
        val schemaVersion = try {
            root["schema_version"]?.jsonPrimitive?.content?.toIntOrNull()
        } catch (e: Exception) {
            null
        }

        if (schemaVersion == null) {
            return ValidationResult.Invalid("Missing or invalid schema_version")
        }
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            return ValidationResult.Invalid(
                "Backup schema version $schemaVersion is newer than supported version $CURRENT_SCHEMA_VERSION. Update the app first.",
            )
        }

        // Validate export timestamp
        val timestamp = try {
            root["export_timestamp"]?.jsonPrimitive?.content?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
        if (timestamp == null || timestamp <= 0) {
            return ValidationResult.Invalid("Missing or invalid export_timestamp")
        }

        // Validate data section exists and is an object
        val data = try {
            root["data"]?.jsonObject
        } catch (e: Exception) {
            null
        }
        if (data == null) {
            return ValidationResult.Invalid("Missing or invalid data section")
        }

        return ValidationResult.Valid(schemaVersion)
    }

    sealed class ValidationResult {
        data class Valid(val schemaVersion: Int) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
