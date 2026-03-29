package dev.counterline.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for engine model types, strength profiles, and move validation.
 */
class EngineModelsTest {

    // --- Strength Profile Tests ---

    @Test
    fun `all strength profiles have valid parameters`() {
        EngineStrengthProfile.entries.forEach { profile ->
            assertTrue("${profile.name} threads must be >= 1", profile.threads >= 1)
            assertTrue("${profile.name} hash must be >= 1", profile.hashMb >= 1)
            assertTrue("${profile.name} depth must be >= 1", profile.depthLimit >= 1)
            assertTrue("${profile.name} movetime must be >= 1", profile.movetimeMs >= 1)
            assertTrue("${profile.name} skill level must be 0-20", profile.skillLevel in 0..20)
        }
    }

    @Test
    fun `training profiles are ordered by strength`() {
        val easy = EngineStrengthProfile.TRAINING_EASY
        val medium = EngineStrengthProfile.TRAINING_MEDIUM
        val hard = EngineStrengthProfile.TRAINING_HARD

        assertTrue(easy.depthLimit < medium.depthLimit)
        assertTrue(medium.depthLimit < hard.depthLimit)
        assertTrue(easy.skillLevel < medium.skillLevel)
        assertTrue(medium.skillLevel < hard.skillLevel)
        assertTrue(easy.movetimeMs < medium.movetimeMs)
        assertTrue(medium.movetimeMs < hard.movetimeMs)
    }

    @Test
    fun `analysis profile is strongest`() {
        val analysis = EngineStrengthProfile.ANALYSIS
        val deepAnalysis = EngineStrengthProfile.DEEP_ANALYSIS

        assertEquals(20, analysis.skillLevel)
        assertEquals(20, deepAnalysis.skillLevel)
        assertTrue(deepAnalysis.depthLimit > analysis.depthLimit)
        assertTrue(deepAnalysis.movetimeMs > analysis.movetimeMs)
    }

    @Test
    fun `all profiles have display names`() {
        EngineStrengthProfile.entries.forEach { profile ->
            assertTrue(
                "${profile.name} must have non-empty display name",
                profile.displayName.isNotEmpty(),
            )
        }
    }

    // --- PositionEvaluation Tests ---

    @Test
    fun `position evaluation is approximate by default`() {
        val eval = PositionEvaluation(
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            scoreCp = 30,
            depth = 20,
            bestMove = "e2e4",
        )
        assertTrue(eval.isApproximate)
    }

    @Test
    fun `position evaluation stores fen correctly`() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val eval = PositionEvaluation(
            fen = fen,
            scoreCp = 25,
            depth = 16,
            bestMove = "c7c6",
        )
        assertEquals(fen, eval.fen)
        assertEquals(25, eval.scoreCp)
        assertEquals(16, eval.depth)
        assertEquals("c7c6", eval.bestMove)
    }

    // --- PvLine Tests ---

    @Test
    fun `pv line stores rank and moves`() {
        val line = PvLine(
            rank = 1,
            move = "e2e4",
            scoreCp = 30,
            mateIn = 0,
            depth = 20,
            pv = listOf("e2e4", "e7e5", "g1f3"),
        )
        assertEquals(1, line.rank)
        assertEquals("e2e4", line.move)
        assertEquals(3, line.pv.size)
    }

    // --- MoveComparison Tests ---

    @Test
    fun `move comparison calculates centipawn loss`() {
        val comparison = MoveComparison(
            userMove = "d2d4",
            repertoireMove = "e2e4",
            engineBestMove = "e2e4",
            userMoveCp = 20,
            repertoireMoveCp = 30,
            engineBestCp = 30,
            centipawnLoss = 10,
        )
        assertEquals(10, comparison.centipawnLoss)
        assertEquals("d2d4", comparison.userMove)
        assertEquals("e2e4", comparison.repertoireMove)
    }

    @Test
    fun `move comparison allows null engine values`() {
        val comparison = MoveComparison(
            userMove = "d2d4",
            repertoireMove = "e2e4",
            engineBestMove = "e2e4",
        )
        assertEquals(null, comparison.userMoveCp)
        assertEquals(null, comparison.centipawnLoss)
    }
}
