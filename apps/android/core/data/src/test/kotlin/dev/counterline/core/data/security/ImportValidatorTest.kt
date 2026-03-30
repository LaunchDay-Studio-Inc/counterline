package dev.counterline.core.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidatorTest {

    private val validator = ImportValidator()

    @Test
    fun `valid backup JSON passes validation`() {
        val json = """
            {
                "schema_version": "2",
                "export_timestamp": "1711800000",
                "data": {
                    "drills": [],
                    "progress": {}
                }
            }
        """.trimIndent()

        val result = validator.validate(json)
        assertTrue("Expected Valid result", result is ImportValidator.ValidationResult.Valid)
        assertEquals(2, (result as ImportValidator.ValidationResult.Valid).schemaVersion)
    }

    @Test
    fun `missing schema_version fails validation`() {
        val json = """
            {
                "export_timestamp": "1711800000",
                "data": {}
            }
        """.trimIndent()

        val result = validator.validate(json)
        assertTrue("Expected Invalid result", result is ImportValidator.ValidationResult.Invalid)
    }

    @Test
    fun `future schema version fails validation`() {
        val json = """
            {
                "schema_version": "99",
                "export_timestamp": "1711800000",
                "data": {}
            }
        """.trimIndent()

        val result = validator.validate(json)
        assertTrue(result is ImportValidator.ValidationResult.Invalid)
        assertTrue(
            (result as ImportValidator.ValidationResult.Invalid).reason.contains("newer"),
        )
    }

    @Test
    fun `invalid JSON fails validation`() {
        val result = validator.validate("{not valid json")
        assertTrue(result is ImportValidator.ValidationResult.Invalid)
    }

    @Test
    fun `missing data section fails validation`() {
        val json = """
            {
                "schema_version": "2",
                "export_timestamp": "1711800000"
            }
        """.trimIndent()

        val result = validator.validate(json)
        assertTrue(result is ImportValidator.ValidationResult.Invalid)
    }

    @Test
    fun `negative timestamp fails validation`() {
        val json = """
            {
                "schema_version": "2",
                "export_timestamp": "-1",
                "data": {}
            }
        """.trimIndent()

        val result = validator.validate(json)
        assertTrue(result is ImportValidator.ValidationResult.Invalid)
    }

    @Test
    fun `schema version 1 passes with lower version`() {
        val json = """
            {
                "schema_version": "1",
                "export_timestamp": "1711800000",
                "data": {}
            }
        """.trimIndent()

        val result = validator.validate(json)
        assertTrue(result is ImportValidator.ValidationResult.Valid)
        assertEquals(1, (result as ImportValidator.ValidationResult.Valid).schemaVersion)
    }
}
