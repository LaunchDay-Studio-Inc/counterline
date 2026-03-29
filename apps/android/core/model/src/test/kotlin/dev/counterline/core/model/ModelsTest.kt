package dev.counterline.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillLevelTest {

    @Test
    fun `skill levels are ordered INTERMEDIATE to ELITE_LAB`() {
        val levels = SkillLevel.entries
        assertEquals(4, levels.size)
        assertEquals(SkillLevel.INTERMEDIATE, levels[0])
        assertEquals(SkillLevel.ADVANCED_CLUB, levels[1])
        assertEquals(SkillLevel.EXPERT_MASTER, levels[2])
        assertEquals(SkillLevel.ELITE_LAB, levels[3])
    }

    @Test
    fun `INTERMEDIATE is less than ADVANCED_CLUB`() {
        assertTrue(SkillLevel.INTERMEDIATE < SkillLevel.ADVANCED_CLUB)
    }

    @Test
    fun `ADVANCED_CLUB is less than EXPERT_MASTER`() {
        assertTrue(SkillLevel.ADVANCED_CLUB < SkillLevel.EXPERT_MASTER)
    }

    @Test
    fun `EXPERT_MASTER is less than ELITE_LAB`() {
        assertTrue(SkillLevel.EXPERT_MASTER < SkillLevel.ELITE_LAB)
    }

    @Test
    fun `filtering by INTERMEDIATE shows only INTERMEDIATE content`() {
        val lines = listOf(
            makeLine("a", SkillLevel.INTERMEDIATE),
            makeLine("b", SkillLevel.ADVANCED_CLUB),
            makeLine("c", SkillLevel.EXPERT_MASTER),
            makeLine("d", SkillLevel.ELITE_LAB),
        )
        val filtered = lines.filter { it.skillLevel <= SkillLevel.INTERMEDIATE }
        assertEquals(1, filtered.size)
        assertEquals("a", filtered[0].id)
    }

    @Test
    fun `filtering by ADVANCED_CLUB shows INTERMEDIATE and ADVANCED_CLUB`() {
        val lines = listOf(
            makeLine("a", SkillLevel.INTERMEDIATE),
            makeLine("b", SkillLevel.ADVANCED_CLUB),
            makeLine("c", SkillLevel.EXPERT_MASTER),
        )
        val filtered = lines.filter { it.skillLevel <= SkillLevel.ADVANCED_CLUB }
        assertEquals(2, filtered.size)
    }

    @Test
    fun `filtering by ELITE_LAB shows all content`() {
        val lines = listOf(
            makeLine("a", SkillLevel.INTERMEDIATE),
            makeLine("b", SkillLevel.ADVANCED_CLUB),
            makeLine("c", SkillLevel.EXPERT_MASTER),
            makeLine("d", SkillLevel.ELITE_LAB),
        )
        val filtered = lines.filter { it.skillLevel <= SkillLevel.ELITE_LAB }
        assertEquals(4, filtered.size)
    }

    private fun makeLine(id: String, level: SkillLevel) = RepertoireLine(
        id = id, name = id, family = "", eco = "", side = Side.WHITE,
        seedLine = "", exitFen = "", exitEpd = "", exitMoveNumber = 0,
        specialistType = "", specialistSize = "", screeningRank = 0,
        screeningScorePct = 0.0, evaluationAtExit = "", moves = emptyList(),
        memoryHook = "", memoryHookBreakdown = emptyList(), skillLevel = level,
    )
}

class ReviewGradeTest {

    @Test
    fun `ReviewGrade ordinals are FAIL lt HARD lt GOOD lt EASY`() {
        assertTrue(ReviewGrade.FAIL.ordinal < ReviewGrade.HARD.ordinal)
        assertTrue(ReviewGrade.HARD.ordinal < ReviewGrade.GOOD.ordinal)
        assertTrue(ReviewGrade.GOOD.ordinal < ReviewGrade.EASY.ordinal)
    }

    @Test
    fun `ReviewGrade comparison GOOD gte GOOD is true`() {
        assertTrue(ReviewGrade.GOOD >= ReviewGrade.GOOD)
    }

    @Test
    fun `ReviewGrade comparison FAIL lt GOOD is true`() {
        assertTrue(ReviewGrade.FAIL < ReviewGrade.GOOD)
    }
}

class StudyModeTest {

    @Test
    fun `all 8 study modes exist`() {
        assertEquals(8, StudyMode.entries.size)
    }

    @Test
    fun `study modes include all expected values`() {
        val names = StudyMode.entries.map { it.name }.toSet()
        assertTrue(names.contains("LEARN"))
        assertTrue(names.contains("RECALL"))
        assertTrue(names.contains("DEVIATION_DRILL"))
        assertTrue(names.contains("PLANS_PATTERNS"))
        assertTrue(names.contains("MODEL_GAME_REPLAY"))
        assertTrue(names.contains("MISTAKE_REVIEW"))
        assertTrue(names.contains("EXAM"))
        assertTrue(names.contains("QUICK_5"))
    }
}

class DrillTypeTest {

    @Test
    fun `all 10 drill types exist`() {
        assertEquals(10, DrillType.entries.size)
    }

    @Test
    fun `drill types include tactical motif and deviation response`() {
        val names = DrillType.entries.map { it.name }.toSet()
        assertTrue(names.contains("TACTICAL_MOTIF"))
        assertTrue(names.contains("STRUCTURE_FLASHCARD"))
        assertTrue(names.contains("TRANSITION_QUIZ"))
        assertTrue(names.contains("COMPARE_POSITION"))
        assertTrue(names.contains("DEVIATION_RESPONSE"))
    }
}
