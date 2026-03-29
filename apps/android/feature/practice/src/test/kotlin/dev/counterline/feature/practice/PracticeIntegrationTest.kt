package dev.counterline.feature.practice

import dev.counterline.core.engine.EngineStrengthProfile
import dev.counterline.core.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Practice mode integration tests — state machine and configuration logic.
 */
class PracticeIntegrationTest {

    @Test
    fun `initial state has line selector visible`() {
        val state = PracticeUiState()
        assertTrue(state.showLineSelector)
        assertFalse(state.sessionStarted)
        assertFalse(state.sessionOver)
    }

    @Test
    fun `default side is WHITE`() {
        val state = PracticeUiState()
        assertEquals(Side.WHITE, state.side)
    }

    @Test
    fun `default mode is LINE_LOCK`() {
        val state = PracticeUiState()
        assertEquals(PracticeMode.LINE_LOCK, state.mode)
    }

    @Test
    fun `default strength is TRAINING_MEDIUM`() {
        val state = PracticeUiState()
        assertEquals(EngineStrengthProfile.TRAINING_MEDIUM, state.strengthProfile)
    }

    @Test
    fun `initial FEN is startpos`() {
        val state = PracticeUiState()
        assertEquals(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            state.currentFen,
        )
    }

    @Test
    fun `move history starts empty`() {
        val state = PracticeUiState()
        assertTrue(state.moveHistory.isEmpty())
        assertTrue(state.moveHistorySan.isEmpty())
    }

    @Test
    fun `starts inside repertoire`() {
        val state = PracticeUiState()
        assertTrue(state.isInsideRepertoire)
    }

    @Test
    fun `practice mode enum has both values`() {
        assertEquals(2, PracticeMode.entries.size)
        assertTrue(PracticeMode.entries.contains(PracticeMode.LINE_LOCK))
        assertTrue(PracticeMode.entries.contains(PracticeMode.DEVIATION))
    }

    @Test
    fun `session counters start at zero`() {
        val state = PracticeUiState()
        assertEquals(0, state.movesPlayed)
        assertEquals(0, state.correctMoves)
        assertEquals(0, state.currentMoveIndex)
    }

    @Test
    fun `all training profiles exclude analysis presets`() {
        val trainingProfiles = EngineStrengthProfile.entries.filter {
            it != EngineStrengthProfile.ANALYSIS && it != EngineStrengthProfile.DEEP_ANALYSIS
        }
        assertEquals(3, trainingProfiles.size)
        assertTrue(trainingProfiles.all { it.skillLevel < 20 })
    }
}
