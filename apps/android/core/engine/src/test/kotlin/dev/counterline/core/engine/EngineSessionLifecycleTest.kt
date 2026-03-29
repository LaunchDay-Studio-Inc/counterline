package dev.counterline.core.engine

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Engine session lifecycle tests.
 * Tests engine state transitions and configuration without the native library.
 */
class EngineSessionLifecycleTest {

    @Test
    fun `engine is not ready before startSession`() {
        val engine = StockfishEngine()
        assertFalse(engine.isReady())
    }

    @Test(expected = IllegalStateException::class)
    fun `evaluateFen throws when session not started`() = runTest {
        val engine = StockfishEngine()
        engine.evaluateFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    @Test(expected = IllegalStateException::class)
    fun `getBestMove throws when session not started`() = runTest {
        val engine = StockfishEngine()
        engine.getBestMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    @Test(expected = IllegalStateException::class)
    fun `getTopMoves throws when session not started`() = runTest {
        val engine = StockfishEngine()
        engine.getTopMoves("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    @Test(expected = IllegalStateException::class)
    fun `analyzeLine throws when session not started`() = runTest {
        val engine = StockfishEngine()
        engine.analyzeLine(
            listOf("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
        )
    }

    @Test
    fun `cancelAnalysis is safe when session not started`() {
        val engine = StockfishEngine()
        // Should not throw
        engine.cancelAnalysis()
    }
}
