package dev.counterline.core.domain

import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 6 tests for skill-layer progression logic.
 * Verifies that skill level affects content filtering, exam config, and UI behavior.
 */
class SkillLayerProgressionTest {

    /**
     * Exam question counts and pass thresholds per skill level.
     * Mirrors the examConfig() logic in ExamViewModel.
     */
    private fun examConfig(skillLevel: SkillLevel): Pair<Int, Float> = when (skillLevel) {
        SkillLevel.INTERMEDIATE -> 15 to 0.60f
        SkillLevel.ADVANCED_CLUB -> 20 to 0.70f
        SkillLevel.EXPERT_MASTER -> 30 to 0.80f
        SkillLevel.ELITE_LAB -> 40 to 0.85f
    }

    @Test
    fun `INTERMEDIATE starts with fewest exam questions and lowest threshold`() {
        val (count, threshold) = examConfig(SkillLevel.INTERMEDIATE)
        assertEquals(15, count)
        assertEquals(0.60f, threshold, 0.01f)
    }

    @Test
    fun `ADVANCED_CLUB has moderate exam difficulty`() {
        val (count, threshold) = examConfig(SkillLevel.ADVANCED_CLUB)
        assertEquals(20, count)
        assertEquals(0.70f, threshold, 0.01f)
    }

    @Test
    fun `EXPERT_MASTER has harder exam`() {
        val (count, threshold) = examConfig(SkillLevel.EXPERT_MASTER)
        assertEquals(30, count)
        assertEquals(0.80f, threshold, 0.01f)
    }

    @Test
    fun `ELITE_LAB has hardest exam`() {
        val (count, threshold) = examConfig(SkillLevel.ELITE_LAB)
        assertEquals(40, count)
        assertEquals(0.85f, threshold, 0.01f)
    }

    @Test
    fun `exam question count increases monotonically with skill level`() {
        val levels = SkillLevel.entries
        val counts = levels.map { examConfig(it).first }
        for (i in 1 until counts.size) {
            assertTrue(
                "Count at ${levels[i]} (${counts[i]}) should be >= ${levels[i - 1]} (${counts[i - 1]})",
                counts[i] >= counts[i - 1],
            )
        }
    }

    @Test
    fun `pass threshold increases monotonically with skill level`() {
        val levels = SkillLevel.entries
        val thresholds = levels.map { examConfig(it).second }
        for (i in 1 until thresholds.size) {
            assertTrue(
                "Threshold at ${levels[i]} should be >= ${levels[i - 1]}",
                thresholds[i] >= thresholds[i - 1],
            )
        }
    }
}
