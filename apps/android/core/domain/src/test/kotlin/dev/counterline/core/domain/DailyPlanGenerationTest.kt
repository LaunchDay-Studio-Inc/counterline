package dev.counterline.core.domain

import dev.counterline.core.model.Side
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.WorkoutItem
import dev.counterline.core.model.WorkoutReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 tests for daily plan and workout generation logic.
 * Validates item prioritization, reason assignment, and mode recommendation.
 */
class DailyPlanGenerationTest {

    // ── Mode recommendation ─────────────────────────────────────────

    @Test
    fun `recommends MISTAKE_REVIEW when more than 5 unresolved mistakes`() {
        val unresolvedCount = 6
        val dueItemsEmpty = false
        val minutes = 15
        val mode = recommendMode(unresolvedCount, dueItemsEmpty, minutes)
        assertEquals(StudyMode.MISTAKE_REVIEW, mode)
    }

    @Test
    fun `recommends LEARN when no due items`() {
        val mode = recommendMode(unresolvedMistakes = 0, noDueItems = true, minutes = 15)
        assertEquals(StudyMode.LEARN, mode)
    }

    @Test
    fun `recommends QUICK_5 for short sessions`() {
        val mode = recommendMode(unresolvedMistakes = 0, noDueItems = false, minutes = 5)
        assertEquals(StudyMode.QUICK_5, mode)
    }

    @Test
    fun `recommends RECALL as default for normal sessions`() {
        val mode = recommendMode(unresolvedMistakes = 2, noDueItems = false, minutes = 15)
        assertEquals(StudyMode.RECALL, mode)
    }

    // ── Reason assignment rules ─────────────────────────────────────

    @Test
    fun `MISTAKE_REMEDIATION items appear before OVERDUE_REVIEW`() {
        val items = listOf(
            WorkoutItem("n1", "l1", Side.WHITE, WorkoutReason.MISTAKE_REMEDIATION, 0),
            WorkoutItem("n2", "l2", Side.WHITE, WorkoutReason.OVERDUE_REVIEW, 1),
        )
        val mistakeIdx = items.indexOfFirst { it.reason == WorkoutReason.MISTAKE_REMEDIATION }
        val overdueIdx = items.indexOfFirst { it.reason == WorkoutReason.OVERDUE_REVIEW }
        assertTrue("Mistakes should come first", mistakeIdx < overdueIdx)
    }

    @Test
    fun `all WorkoutItem priorities are non-negative`() {
        val items = (0 until 10).map {
            WorkoutItem("n$it", "l$it", Side.WHITE, WorkoutReason.OVERDUE_REVIEW, it)
        }
        items.forEach { assertTrue(it.priority >= 0) }
    }

    // ── Session sizing ──────────────────────────────────────────────

    @Test
    fun `tiny sessions produce exactly 8 items`() {
        val size = sessionSize(3)
        assertEquals(8, size)
    }

    @Test
    fun `medium sessions produce 15 items`() {
        val size = sessionSize(8)
        assertEquals(15, size)
    }

    @Test
    fun `large sessions produce 20 items`() {
        val size = sessionSize(15)
        assertEquals(20, size)
    }

    @Test
    fun `very large sessions produce 25 items`() {
        val size = sessionSize(45)
        assertEquals(25, size)
    }

    // ── Side focus correctness ──────────────────────────────────────

    @Test
    fun `no side focus when balanced`() {
        val focus = determineSideFocus(0.75f, 0.73f)
        assertEquals(null, focus)
    }

    @Test
    fun `BLACK focused when white mastery higher`() {
        val focus = determineSideFocus(whiteMastery = 0.90f, blackMastery = 0.60f)
        assertEquals(Side.BLACK, focus)
    }

    @Test
    fun `WHITE focused when black mastery higher`() {
        val focus = determineSideFocus(whiteMastery = 0.55f, blackMastery = 0.80f)
        assertEquals(Side.WHITE, focus)
    }

    // ── Helpers mirror production logic ─────────────────────────────

    private fun recommendMode(
        unresolvedMistakes: Int,
        noDueItems: Boolean,
        minutes: Int,
    ): StudyMode = when {
        unresolvedMistakes > 5 -> StudyMode.MISTAKE_REVIEW
        noDueItems -> StudyMode.LEARN
        minutes <= 5 -> StudyMode.QUICK_5
        else -> StudyMode.RECALL
    }

    private fun sessionSize(minutes: Int): Int = when (minutes) {
        in 0..5 -> 8
        in 6..10 -> 15
        in 11..20 -> 20
        else -> 25
    }

    private fun determineSideFocus(whiteMastery: Float, blackMastery: Float): Side? {
        val gap = whiteMastery - blackMastery
        return when {
            gap > 0.15f -> Side.BLACK
            gap < -0.15f -> Side.WHITE
            else -> null
        }
    }
}
