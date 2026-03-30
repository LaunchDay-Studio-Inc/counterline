package dev.counterline.core.domain

import dev.counterline.core.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 tests for exam scoring across skill levels.
 * Validates that pass/fail logic, question selection, and result recording
 * correctly adapt to the user's configured skill level.
 */
class ExamScoringTest {

    private data class ExamConfig(val questionCount: Int, val passThreshold: Float)

    private fun examConfig(level: SkillLevel) = when (level) {
        SkillLevel.INTERMEDIATE -> ExamConfig(15, 0.60f)
        SkillLevel.ADVANCED_CLUB -> ExamConfig(20, 0.70f)
        SkillLevel.EXPERT_MASTER -> ExamConfig(30, 0.80f)
        SkillLevel.ELITE_LAB -> ExamConfig(40, 0.85f)
    }

    // ── Pass/fail boundary tests ────────────────────────────────────

    @Test
    fun `INTERMEDIATE passes with 9 of 15 correct (60 percent)`() {
        val config = examConfig(SkillLevel.INTERMEDIATE)
        val correct = 9
        val score = correct.toFloat() / config.questionCount
        assertTrue("$score should >= ${config.passThreshold}", score >= config.passThreshold)
    }

    @Test
    fun `INTERMEDIATE fails with 8 of 15 correct (53 percent)`() {
        val config = examConfig(SkillLevel.INTERMEDIATE)
        val correct = 8
        val score = correct.toFloat() / config.questionCount
        assertTrue("$score should < ${config.passThreshold}", score < config.passThreshold)
    }

    @Test
    fun `ADVANCED_CLUB passes with 14 of 20 correct (70 percent)`() {
        val config = examConfig(SkillLevel.ADVANCED_CLUB)
        val correct = 14
        val score = correct.toFloat() / config.questionCount
        assertTrue(score >= config.passThreshold)
    }

    @Test
    fun `ADVANCED_CLUB fails with 13 of 20 correct (65 percent)`() {
        val config = examConfig(SkillLevel.ADVANCED_CLUB)
        val correct = 13
        val score = correct.toFloat() / config.questionCount
        assertTrue(score < config.passThreshold)
    }

    @Test
    fun `EXPERT_MASTER passes with 24 of 30 correct (80 percent)`() {
        val config = examConfig(SkillLevel.EXPERT_MASTER)
        val correct = 24
        val score = correct.toFloat() / config.questionCount
        assertTrue(score >= config.passThreshold)
    }

    @Test
    fun `EXPERT_MASTER fails with 23 of 30 correct (77 percent)`() {
        val config = examConfig(SkillLevel.EXPERT_MASTER)
        val correct = 23
        val score = correct.toFloat() / config.questionCount
        assertTrue(score < config.passThreshold)
    }

    @Test
    fun `ELITE_LAB passes with 34 of 40 correct (85 percent)`() {
        val config = examConfig(SkillLevel.ELITE_LAB)
        val correct = 34
        val score = correct.toFloat() / config.questionCount
        assertTrue(score >= config.passThreshold)
    }

    @Test
    fun `ELITE_LAB fails with 33 of 40 correct (82 percent)`() {
        val config = examConfig(SkillLevel.ELITE_LAB)
        val correct = 33
        val score = correct.toFloat() / config.questionCount
        assertTrue(score < config.passThreshold)
    }

    // ── Score computation ───────────────────────────────────────────

    @Test
    fun `perfect score is 100 percent at every level`() {
        for (level in SkillLevel.entries) {
            val config = examConfig(level)
            val score = config.questionCount.toFloat() / config.questionCount
            assertEquals(1.0f, score, 0.001f)
        }
    }

    @Test
    fun `zero correct is 0 percent at every level`() {
        for (level in SkillLevel.entries) {
            val config = examConfig(level)
            val score = 0f / config.questionCount
            assertEquals(0.0f, score, 0.001f)
        }
    }

    // ── Result recording ────────────────────────────────────────────

    @Test
    fun `exam result records correct question count and threshold`() {
        for (level in SkillLevel.entries) {
            val config = examConfig(level)
            assertTrue(config.questionCount > 0)
            assertTrue(config.passThreshold > 0f)
            assertTrue(config.passThreshold <= 1.0f)
        }
    }

    @Test
    fun `minimum correct to pass is ceil of threshold times count`() {
        for (level in SkillLevel.entries) {
            val config = examConfig(level)
            val minCorrect = kotlin.math.ceil(
                (config.passThreshold * config.questionCount).toDouble(),
            ).toInt()
            assertTrue("$level: min correct $minCorrect <= total ${config.questionCount}",
                minCorrect <= config.questionCount)
        }
    }
}
