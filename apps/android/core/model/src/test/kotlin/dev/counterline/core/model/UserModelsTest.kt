package dev.counterline.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeReviewStateTest {

    @Test
    fun `default NodeReviewState has correct initial values`() {
        val state = NodeReviewState(
            nodeId = "n1",
            side = Side.WHITE,
            lineId = "line1",
        )
        assertEquals(2.5f, state.easeFactor, 0.01f)
        assertEquals(0f, state.intervalDays, 0.01f)
        assertEquals(0, state.repetitions)
        assertEquals(0L, state.lastReviewEpochMs)
        assertEquals(0L, state.nextReviewEpochMs)
        assertEquals(0, state.lapseCount)
        assertEquals(ReviewGrade.GOOD, state.lastGrade)
    }
}

class MistakeItemTest {

    @Test
    fun `default MistakeItem is unresolved`() {
        val mistake = MistakeItem(
            nodeId = "n1",
            lineId = "l1",
            side = Side.BLACK,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            expectedMove = "e4",
            userMove = "d4",
            explanation = "e4 opens the center",
            createdEpochMs = 1000L,
            nextReviewEpochMs = 2000L,
        )
        assertFalse(mistake.resolved)
        assertEquals(0, mistake.reviewCount)
    }
}

class ExamResultTest {

    @Test
    fun `exam with 70pct accuracy passes`() {
        val exam = ExamResult(
            side = Side.WHITE,
            startedEpochMs = 0,
            finishedEpochMs = 1000,
            totalQuestions = 10,
            correctAnswers = 7,
            accuracy = 0.7f,
            avgResponseTimeMs = 500,
            branchCoverage = 0.7f,
            passed = true,
        )
        assertTrue(exam.passed)
        assertEquals(0.7f, exam.accuracy, 0.01f)
    }

    @Test
    fun `exam with below 70pct does not pass`() {
        val exam = ExamResult(
            side = Side.BLACK,
            startedEpochMs = 0,
            finishedEpochMs = 1000,
            totalQuestions = 10,
            correctAnswers = 5,
            accuracy = 0.5f,
            avgResponseTimeMs = 800,
            branchCoverage = 0.5f,
            passed = false,
        )
        assertFalse(exam.passed)
    }
}

class BadgeTest {

    @Test
    fun `unearnedBadge has null earnedEpochMs`() {
        val badge = Badge(
            id = "test",
            title = "Test Badge",
            description = "A test badge",
        )
        assertEquals(null, badge.earnedEpochMs)
        assertEquals(null, badge.side)
        assertEquals("star", badge.iconName)
    }

    @Test
    fun `earned badge has non-null earnedEpochMs`() {
        val badge = Badge(
            id = "test",
            title = "Test Badge",
            description = "A test badge",
            earnedEpochMs = 1234567890L,
            side = Side.WHITE,
        )
        assertEquals(1234567890L, badge.earnedEpochMs)
        assertEquals(Side.WHITE, badge.side)
    }
}

class ProgressStatsTest {

    @Test
    fun `default ProgressStats has zeros`() {
        val stats = ProgressStats(
            totalDrillsCompleted = 0,
            whiteLineAccuracy = 0f,
            blackLineAccuracy = 0f,
            currentStreak = 0,
            longestStreak = 0,
            totalStudyTimeMinutes = 0,
            drillsCompletedToday = 0,
            dueForReview = 0,
        )
        assertEquals(0f, stats.whiteMastery, 0.01f)
        assertEquals(0f, stats.blackMastery, 0.01f)
        assertTrue(stats.weakestNodes.isEmpty())
        assertTrue(stats.accuracyTrend.isEmpty())
        assertTrue(stats.timeByMode.isEmpty())
    }
}

class UserSettingsTest {

    @Test
    fun `default UserSettings has INTERMEDIATE skill level`() {
        val settings = UserSettings()
        assertEquals(SkillLevel.INTERMEDIATE, settings.skillLevel)
        assertEquals(DarkMode.SYSTEM, settings.darkMode)
        assertEquals(10, settings.dailyDrillGoal)
    }
}
