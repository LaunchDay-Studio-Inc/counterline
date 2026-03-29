package dev.counterline.core.engine

import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.SkillLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for TrainingAssistant move comparison and feedback generation.
 * These test the logic without requiring a live engine.
 */
class TrainingAssistantTest {

    private val dummyEngine = StockfishEngine()

    private val assistant = TrainingAssistant(dummyEngine)

    private val repertoireMove = RepertoireMove(
        moveNumber = 1,
        san = "e4",
        purpose = "Controls the center with a pawn",
        isWhiteMove = true,
        whyThisMove = "Opens lines for the bishop and queen",
        keyPlanCallout = "Develop pieces rapidly after e4",
    )

    @Test
    fun `correct move returns positive feedback`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "e4",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.INTERMEDIATE,
        )
        assertTrue(feedback.isCorrect)
        assertEquals("e4", feedback.userMove)
        assertEquals("e4", feedback.repertoireMove)
        assertNotNull(feedback.explanation)
    }

    @Test
    fun `incorrect move returns negative feedback with explanation`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "d4",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.INTERMEDIATE,
        )
        assertFalse(feedback.isCorrect)
        assertEquals("d4", feedback.userMove)
        assertEquals("e4", feedback.repertoireMove)
        assertTrue(feedback.explanation.contains("e4"))
    }

    @Test
    fun `intermediate user does not get engine note`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "d4",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.INTERMEDIATE,
        )
        assertNull(feedback.engineNote)
    }

    @Test
    fun `advanced club user does not get engine note`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "d4",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.ADVANCED_CLUB,
        )
        assertNull(feedback.engineNote)
    }

    @Test
    fun `correct move case insensitive`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "E4",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.INTERMEDIATE,
        )
        assertTrue(feedback.isCorrect)
    }

    @Test
    fun `wrong move includes whyThisMove in explanation`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "Nf3",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.INTERMEDIATE,
        )
        assertTrue(feedback.explanation.contains("Opens lines"))
    }

    @Test
    fun `wrong move includes keyPlanCallout in explanation`() = runTest {
        val feedback = assistant.compareMove(
            userMove = "Nf3",
            repertoireMove = repertoireMove,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            skillLevel = SkillLevel.INTERMEDIATE,
        )
        assertTrue(feedback.explanation.contains("Develop pieces"))
    }
}
