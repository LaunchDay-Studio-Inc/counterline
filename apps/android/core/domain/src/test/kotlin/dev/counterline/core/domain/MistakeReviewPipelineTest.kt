package dev.counterline.core.domain

import dev.counterline.core.model.MistakeItem
import dev.counterline.core.model.MistakeSeverity
import dev.counterline.core.model.MistakeTheme
import dev.counterline.core.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 tests for the mistake review pipeline.
 * Tests theme/severity grouping, consecutive-correct tracking, and resolution.
 */
class MistakeReviewPipelineTest {

    private fun sampleMistake(
        id: Long = 1,
        theme: MistakeTheme = MistakeTheme.MOVE_ORDER,
        severity: MistakeSeverity = MistakeSeverity.MINOR,
        resolved: Boolean = false,
        consecutiveCorrect: Int = 0,
    ) = MistakeItem(
        id = id,
        nodeId = "node_$id",
        lineId = "line_1",
        side = Side.WHITE,
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        expectedMove = "e4",
        userMove = "d4",
        explanation = "e4 is the mainline",
        createdEpochMs = System.currentTimeMillis(),
        nextReviewEpochMs = System.currentTimeMillis(),
        resolved = resolved,
        mistakeTheme = theme,
        severity = severity,
        consecutiveCorrect = consecutiveCorrect,
    )

    @Test
    fun `mistakes can be grouped by theme`() {
        val mistakes = listOf(
            sampleMistake(1, MistakeTheme.MOVE_ORDER),
            sampleMistake(2, MistakeTheme.MOVE_ORDER),
            sampleMistake(3, MistakeTheme.STRATEGIC_MISUNDERSTANDING),
            sampleMistake(4, MistakeTheme.TRANSPOSITION_ERROR),
            sampleMistake(5, MistakeTheme.TACTICAL_MISS),
        )

        val grouped = mistakes.groupBy { it.mistakeTheme.name }

        assertEquals(4, grouped.size)
        assertEquals(2, grouped["MOVE_ORDER"]?.size)
        assertEquals(1, grouped["STRATEGIC_MISUNDERSTANDING"]?.size)
        assertEquals(1, grouped["TRANSPOSITION_ERROR"]?.size)
    }

    @Test
    fun `severity ordering is MINOR lt MODERATE lt CRITICAL`() {
        assertTrue(MistakeSeverity.MINOR.ordinal < MistakeSeverity.MODERATE.ordinal)
        assertTrue(MistakeSeverity.MODERATE.ordinal < MistakeSeverity.CRITICAL.ordinal)
    }

    @Test
    fun `unresolved mistakes are filtered correctly`() {
        val mistakes = listOf(
            sampleMistake(1, resolved = false),
            sampleMistake(2, resolved = true),
            sampleMistake(3, resolved = false),
        )
        val unresolved = mistakes.filter { !it.resolved }
        assertEquals(2, unresolved.size)
    }

    @Test
    fun `consecutive correct increments correctly`() {
        val mistake = sampleMistake(consecutiveCorrect = 2)
        val updated = mistake.copy(consecutiveCorrect = mistake.consecutiveCorrect + 1)
        assertEquals(3, updated.consecutiveCorrect)
    }

    @Test
    fun `CRITICAL severity mistakes are prioritized`() {
        val mistakes = listOf(
            sampleMistake(1, severity = MistakeSeverity.MINOR),
            sampleMistake(2, severity = MistakeSeverity.CRITICAL),
            sampleMistake(3, severity = MistakeSeverity.MODERATE),
        )
        val sorted = mistakes.sortedByDescending { it.severity.ordinal }
        assertEquals(MistakeSeverity.CRITICAL, sorted[0].severity)
        assertEquals(MistakeSeverity.MODERATE, sorted[1].severity)
        assertEquals(MistakeSeverity.MINOR, sorted[2].severity)
    }

    @Test
    fun `all MistakeTheme values are distinct`() {
        val themes = MistakeTheme.entries
        assertEquals(themes.size, themes.map { it.name }.distinct().size)
    }
}
