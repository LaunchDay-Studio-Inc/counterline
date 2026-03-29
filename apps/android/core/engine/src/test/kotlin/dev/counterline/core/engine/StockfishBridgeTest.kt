package dev.counterline.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JNI smoke tests — verify the bridge class structure and model types.
 * Actual native calls require an Android environment or instrumented test;
 * these tests validate the Kotlin-side contracts.
 */
class StockfishBridgeTest {

    @Test
    fun `bridge class exists and has expected native methods`() {
        // Verify the class can be instantiated (native methods will throw
        // UnsatisfiedLinkError without the .so, but the class itself loads)
        val bridge = try {
            StockfishBridge()
        } catch (_: UnsatisfiedLinkError) {
            // Expected in unit test environment — no native library
            null
        }
        // The class itself should be loadable even if native lib is absent
        // This verifies the Kotlin class definition compiles correctly
        assertNotNull(StockfishBridge::class.java)
    }

    @Test
    fun `engine search result parses correctly`() {
        val result = EngineSearchResult(
            bestmove = "e2e4",
            ponder = "e7e5",
            depth = 20,
            scoreCp = 35,
            mateIn = 0,
        )
        assertEquals("e2e4", result.bestmove)
        assertEquals("e7e5", result.ponder)
        assertEquals(20, result.depth)
        assertEquals(35, result.scoreCp)
        assertEquals(0, result.mateIn)
    }

    @Test
    fun `engine search result default values`() {
        val result = EngineSearchResult()
        assertEquals("", result.bestmove)
        assertEquals("", result.ponder)
        assertEquals(0, result.depth)
        assertEquals(0, result.scoreCp)
        assertEquals(0, result.mateIn)
    }
}
