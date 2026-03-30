package dev.counterline.core.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingManagerTest {

    @Test
    fun `sanitize removes FEN strings`() {
        val input = "Error at position rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1 in drill"
        val result = CrashReportingManager.sanitize(input)
        assertFalse("FEN should be removed", result.contains("rnbqkbnr"))
        assertTrue("Replacement marker present", result.contains("[chess-content-redacted]"))
    }

    @Test
    fun `sanitize removes UCI move sequences`() {
        val input = "Failed during analysis: e2e4 e7e5 g1f3 b8c6 f1b5"
        val result = CrashReportingManager.sanitize(input)
        assertFalse("UCI moves should be removed", result.contains("e2e4"))
        assertTrue(result.contains("[chess-content-redacted]"))
    }

    @Test
    fun `sanitize preserves non-chess content`() {
        val input = "NullPointerException in MainViewModel.loadProgress()"
        val result = CrashReportingManager.sanitize(input)
        assertEquals(input, result)
    }

    @Test
    fun `sanitize handles empty string`() {
        assertEquals("", CrashReportingManager.sanitize(""))
    }

    @Test
    fun `sanitize handles multiple chess patterns`() {
        val input = "Error: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 then e2e4 e7e5 g1f3"
        val result = CrashReportingManager.sanitize(input)
        assertFalse(result.contains("rnbqkbnr"))
        assertFalse(result.contains("e2e4"))
    }
}
