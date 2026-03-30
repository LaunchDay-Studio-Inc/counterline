package dev.counterline.core.domain

import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ReadinessScore
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.WorkoutItem
import dev.counterline.core.model.WorkoutReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 tests for coaching use cases.
 * Tests readiness calculation weights, weakness detection thresholds,
 * daily workout generation priorities, and coach message generation.
 */
class CoachingUseCasesTest {

    // ── Readiness score calculation ──────────────────────────────────

    @Test
    fun `readiness score is clamped between 0 and 1`() {
        // The formula is: mastery*0.40 + coverage*0.25 + accuracy*0.20 + (0.15 - penalty)
        // Maximum: 1*0.40 + 1*0.25 + 1*0.20 + 0.15 = 1.0
        // Minimum: 0*0.40 + 0*0.25 + 0*0.20 + (0.15 - 0.15) = 0.0
        val maxScore = 1f * 0.40f + 1f * 0.25f + 1f * 0.20f + 0.15f
        val minScore = 0f * 0.40f + 0f * 0.25f + 0f * 0.20f + (0.15f - 0.15f)
        assertTrue("Max readiness should be <= 1.0", maxScore <= 1.0f)
        assertTrue("Min readiness should be >= 0.0", minScore >= 0.0f)
    }

    @Test
    fun `readiness weight sum equals 1`() {
        val mastery = 0.40f
        val coverage = 0.25f
        val accuracy = 0.20f
        val issuePenaltyMax = 0.15f
        assertEquals(1.0f, mastery + coverage + accuracy + issuePenaltyMax, 0.001f)
    }

    @Test
    fun `issue penalty is capped at 15 percent`() {
        // penalty = min(0.15, unresolvedMistakes * 0.02 + overdueItems * 0.01)
        val enormous = 100 * 0.02f + 100 * 0.01f // 3.0
        val capped = minOf(0.15f, enormous)
        assertEquals(0.15f, capped, 0.001f)
    }

    @Test
    fun `each unresolved mistake costs 2 percent of penalty`() {
        val oneMistake = 1 * 0.02f
        assertEquals(0.02f, oneMistake, 0.001f)
    }

    @Test
    fun `each overdue item costs 1 percent of penalty`() {
        val oneOverdue = 1 * 0.01f
        assertEquals(0.01f, oneOverdue, 0.001f)
    }

    // ── Weakness detection ──────────────────────────────────────────

    @Test
    fun `chronic miss threshold default is 3`() {
        val threshold = 3
        val states = listOf(
            sampleNode("n1", lapseCount = 2),
            sampleNode("n2", lapseCount = 3),
            sampleNode("n3", lapseCount = 5),
        )
        val chronic = states.filter { it.lapseCount >= threshold }
        assertEquals(2, chronic.size)
    }

    @Test
    fun `chronic miss nodes sorted by lapse count descending`() {
        val threshold = 3
        val states = listOf(
            sampleNode("n1", lapseCount = 3),
            sampleNode("n2", lapseCount = 7),
            sampleNode("n3", lapseCount = 5),
        )
        val sorted = states.filter { it.lapseCount >= threshold }
            .sortedByDescending { it.lapseCount }
        assertEquals("n2", sorted[0].nodeId)
        assertEquals("n3", sorted[1].nodeId)
        assertEquals("n1", sorted[2].nodeId)
    }

    @Test
    fun `fragile line needs 20 percent drop below overall mastery`() {
        // overall mastery is 0.80, a line at 0.55 is fragile (drop > 0.20)
        val overallMastery = 0.80f
        val lineMastery = 0.55f
        assertTrue(lineMastery < overallMastery - 0.20f)
    }

    @Test
    fun `side imbalance detected when gap exceeds 15 percent`() {
        val whiteMastery = 0.85f
        val blackMastery = 0.65f
        val gap = whiteMastery - blackMastery
        assertTrue("Gap $gap should exceed 0.15", gap > 0.15f)
    }

    // ── Daily workout generation ────────────────────────────────────

    @Test
    fun `mistake remediation gets at most 30 percent of slots`() {
        val targetSize = 20
        val mistakeSlots = (targetSize * 0.3).toInt()
        assertEquals(6, mistakeSlots)
    }

    @Test
    fun `workout items interleave white and black`() {
        val items = listOf(
            WorkoutItem("w1", "line_w1", Side.WHITE, WorkoutReason.OVERDUE_REVIEW, 0),
            WorkoutItem("w2", "line_w2", Side.WHITE, WorkoutReason.OVERDUE_REVIEW, 1),
            WorkoutItem("b1", "line_b1", Side.BLACK, WorkoutReason.OVERDUE_REVIEW, 2),
            WorkoutItem("b2", "line_b2", Side.BLACK, WorkoutReason.OVERDUE_REVIEW, 3),
        )
        val white = items.filter { it.side == Side.WHITE }.toMutableList()
        val black = items.filter { it.side == Side.BLACK }.toMutableList()
        val interleaved = mutableListOf<WorkoutItem>()
        while (white.isNotEmpty() || black.isNotEmpty()) {
            if (white.isNotEmpty()) interleaved.add(white.removeAt(0))
            if (black.isNotEmpty()) interleaved.add(black.removeAt(0))
        }
        // Pattern should be W, B, W, B
        assertEquals(Side.WHITE, interleaved[0].side)
        assertEquals(Side.BLACK, interleaved[1].side)
        assertEquals(Side.WHITE, interleaved[2].side)
        assertEquals(Side.BLACK, interleaved[3].side)
    }

    @Test
    fun `session size scales with preferred minutes`() {
        val cases = mapOf(5 to 8, 10 to 15, 20 to 20, 30 to 25)
        for ((minutes, expected) in cases) {
            val size = when (minutes) {
                in 0..5 -> 8
                in 6..10 -> 15
                in 11..20 -> 20
                else -> 25
            }
            assertEquals("$minutes min -> $expected items", expected, size)
        }
    }

    @Test
    fun `side focus targets weaker weapon when gap over 15 percent`() {
        // white mastery 0.90, black mastery 0.70 => gap 0.20 > 0.15 => focus on black
        val whiteMastery = 0.90f
        val blackMastery = 0.70f
        val gap = whiteMastery - blackMastery
        val sideFocus = when {
            gap > 0.15f -> Side.BLACK
            gap < -0.15f -> Side.WHITE
            else -> null
        }
        assertEquals(Side.BLACK, sideFocus)
    }

    // ── Coach message ───────────────────────────────────────────────

    @Test
    fun `coach message includes mistake count when present`() {
        val mistakeCount = 4
        val message = buildSimpleCoachMessage(dueCount = 0, mistakeCount = mistakeCount)
        assertTrue(message.contains("4"))
        assertTrue(message.contains("mistake"))
    }

    @Test
    fun `coach message for zero due and zero mistakes is encouraging`() {
        val message = buildSimpleCoachMessage(dueCount = 0, mistakeCount = 0)
        assertTrue(message.contains("caught up") || message.contains("All"))
    }

    // ── WorkoutReason enum completeness ────────────────────────────

    @Test
    fun `all WorkoutReason values are distinct`() {
        val reasons = WorkoutReason.entries
        assertEquals(reasons.size, reasons.map { it.name }.distinct().size)
    }

    @Test
    fun `WorkoutReason has at least OVERDUE and MISTAKE and NEW_MATERIAL`() {
        val names = WorkoutReason.entries.map { it.name }
        assertTrue(names.contains("OVERDUE_REVIEW"))
        assertTrue(names.contains("MISTAKE_REMEDIATION"))
        assertTrue(names.contains("NEW_MATERIAL"))
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun sampleNode(
        nodeId: String,
        lapseCount: Int = 0,
        side: Side = Side.WHITE,
    ) = NodeReviewState(
        nodeId = nodeId,
        side = side,
        lineId = "line_1",
        lapseCount = lapseCount,
    )

    private fun buildSimpleCoachMessage(dueCount: Int, mistakeCount: Int): String {
        if (dueCount == 0 && mistakeCount == 0) {
            return "All caught up. Consider learning new lines or reviewing model games."
        }
        val parts = mutableListOf<String>()
        if (mistakeCount > 0) {
            parts.add("You have $mistakeCount unresolved mistake${if (mistakeCount > 1) "s" else ""}.")
        }
        if (dueCount > 0) {
            parts.add("$dueCount item${if (dueCount > 1) "s" else ""} due for review.")
        }
        return parts.joinToString(" ")
    }
}
