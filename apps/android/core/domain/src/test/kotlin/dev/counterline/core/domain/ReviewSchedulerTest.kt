package dev.counterline.core.domain

import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewSchedulerTest {

    private val fixedTime = 1_700_000_000_000L // fixed epoch for deterministic tests
    private lateinit var scheduler: ReviewScheduler

    @Before
    fun setUp() {
        scheduler = ReviewScheduler(clock = { fixedTime })
    }

    private fun freshNode(nodeId: String = "n1") = NodeReviewState(
        nodeId = nodeId,
        side = Side.WHITE,
        lineId = "line1",
    )

    // --- schedule() tests ---

    @Test
    fun `FAIL resets repetitions to zero and increases lapseCount`() {
        val node = freshNode().copy(repetitions = 3, lapseCount = 0)
        val result = scheduler.schedule(node, ReviewGrade.FAIL)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.lapseCount)
    }

    @Test
    fun `FAIL decreases ease factor by 0_20`() {
        val node = freshNode().copy(easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.FAIL)
        assertEquals(2.3f, result.easeFactor, 0.01f)
    }

    @Test
    fun `FAIL ease factor does not go below 1_3`() {
        val node = freshNode().copy(easeFactor = 1.4f)
        val result = scheduler.schedule(node, ReviewGrade.FAIL)
        assertEquals(1.3f, result.easeFactor, 0.01f)
    }

    @Test
    fun `FAIL schedules review in approx 10 minutes`() {
        val node = freshNode()
        val result = scheduler.schedule(node, ReviewGrade.FAIL)
        val intervalMs = result.nextReviewEpochMs - fixedTime
        assertTrue("Interval should be ~10 min (600s), got ${intervalMs}ms", intervalMs in 500_000..700_000)
    }

    @Test
    fun `GOOD on first rep sets interval to approx 1 hour`() {
        val node = freshNode()
        val result = scheduler.schedule(node, ReviewGrade.GOOD)
        assertEquals(1, result.repetitions)
        val intervalMs = result.nextReviewEpochMs - fixedTime
        // 0.042 days ~= 3629 seconds ~= 3.6M ms
        assertTrue("Interval should be ~1 hr, got ${intervalMs}ms", intervalMs in 3_000_000..4_500_000)
    }

    @Test
    fun `GOOD on second rep sets interval to 1 day`() {
        val node = freshNode().copy(repetitions = 1, intervalDays = 0.042f)
        val result = scheduler.schedule(node, ReviewGrade.GOOD)
        assertEquals(2, result.repetitions)
        val intervalMs = result.nextReviewEpochMs - fixedTime
        val oneDayMs = 86_400_000L
        assertTrue("Interval should be ~1 day, got ${intervalMs}ms", intervalMs in (oneDayMs - 1_000_000)..(oneDayMs + 1_000_000))
    }

    @Test
    fun `GOOD on third rep uses ease factor multiplication`() {
        val node = freshNode().copy(repetitions = 2, intervalDays = 1f, easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.GOOD)
        assertEquals(3, result.repetitions)
        assertEquals(2.5f, result.intervalDays, 0.01f)
    }

    @Test
    fun `GOOD does not change ease factor`() {
        val node = freshNode().copy(easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.GOOD)
        assertEquals(2.5f, result.easeFactor, 0.01f)
    }

    @Test
    fun `HARD decreases ease factor by 0_15`() {
        val node = freshNode().copy(easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.HARD)
        assertEquals(2.35f, result.easeFactor, 0.01f)
    }

    @Test
    fun `HARD on mature card uses 1_2x interval growth`() {
        val node = freshNode().copy(repetitions = 3, intervalDays = 4f, easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.HARD)
        assertEquals(4.8f, result.intervalDays, 0.01f) // 4 * 1.2
    }

    @Test
    fun `EASY increases ease factor by 0_15`() {
        val node = freshNode().copy(easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.EASY)
        assertEquals(2.65f, result.easeFactor, 0.01f)
    }

    @Test
    fun `EASY on first rep jumps to 1 day`() {
        val node = freshNode()
        val result = scheduler.schedule(node, ReviewGrade.EASY)
        assertEquals(1f, result.intervalDays, 0.01f)
    }

    @Test
    fun `EASY on mature card uses ease x 1_3`() {
        val node = freshNode().copy(repetitions = 3, intervalDays = 4f, easeFactor = 2.5f)
        val result = scheduler.schedule(node, ReviewGrade.EASY)
        val expected = 4f * 2.5f * 1.3f // 13.0
        assertEquals(expected, result.intervalDays, 0.01f)
    }

    @Test
    fun `schedule sets lastReviewEpochMs to current time`() {
        val node = freshNode()
        val result = scheduler.schedule(node, ReviewGrade.GOOD)
        assertEquals(fixedTime, result.lastReviewEpochMs)
    }

    @Test
    fun `schedule sets lastGrade`() {
        val node = freshNode()
        assertEquals(ReviewGrade.GOOD, scheduler.schedule(node, ReviewGrade.GOOD).lastGrade)
        assertEquals(ReviewGrade.FAIL, scheduler.schedule(node, ReviewGrade.FAIL).lastGrade)
        assertEquals(ReviewGrade.HARD, scheduler.schedule(node, ReviewGrade.HARD).lastGrade)
        assertEquals(ReviewGrade.EASY, scheduler.schedule(node, ReviewGrade.EASY).lastGrade)
    }

    // --- calculateMastery() tests ---

    @Test
    fun `mastery of empty list is 0`() {
        assertEquals(0f, scheduler.calculateMastery(emptyList()), 0.01f)
    }

    @Test
    fun `mastery of well-known nodes is high`() {
        val nodes = listOf(
            freshNode("a").copy(intervalDays = 30f, easeFactor = 2.5f, lapseCount = 0),
            freshNode("b").copy(intervalDays = 25f, easeFactor = 2.3f, lapseCount = 0),
        )
        val mastery = scheduler.calculateMastery(nodes)
        assertTrue("Mastery should be > 0.7 for well-known nodes, got $mastery", mastery > 0.7f)
    }

    @Test
    fun `mastery of fresh nodes is low`() {
        val nodes = listOf(
            freshNode("a"), // interval=0, ease=2.5, lapse=0
            freshNode("b"),
        )
        val mastery = scheduler.calculateMastery(nodes)
        assertTrue("Mastery should be < 0.5 for fresh nodes, got $mastery", mastery < 0.5f)
    }

    @Test
    fun `mastery penalizes high lapse count`() {
        val good = freshNode("a").copy(intervalDays = 20f, easeFactor = 2.5f, lapseCount = 0)
        val lapsed = freshNode("b").copy(intervalDays = 20f, easeFactor = 2.5f, lapseCount = 5)
        val masteryGood = scheduler.calculateMastery(listOf(good))
        val masteryLapsed = scheduler.calculateMastery(listOf(lapsed))
        assertTrue("Lapsed node should have lower mastery", masteryLapsed < masteryGood)
    }

    // --- buildReviewQueue() tests ---

    @Test
    fun `buildReviewQueue filters out future items`() {
        val due = freshNode("a").copy(nextReviewEpochMs = fixedTime - 1000)
        val future = freshNode("b").copy(nextReviewEpochMs = fixedTime + 100_000)
        val queue = scheduler.buildReviewQueue(listOf(due, future))
        assertEquals(1, queue.size)
        assertEquals("a", queue[0].nodeId)
    }

    @Test
    fun `buildReviewQueue sorts by most overdue first`() {
        val veryOverdue = freshNode("a").copy(nextReviewEpochMs = fixedTime - 10_000_000)
        val slightlyOverdue = freshNode("b").copy(nextReviewEpochMs = fixedTime - 1000)
        val queue = scheduler.buildReviewQueue(listOf(slightlyOverdue, veryOverdue))
        assertEquals("a", queue[0].nodeId)
        assertEquals("b", queue[1].nodeId)
    }

    @Test
    fun `buildReviewQueue sorts equal overdue by lapseCount desc`() {
        val moreLapses = freshNode("a").copy(nextReviewEpochMs = fixedTime - 1000, lapseCount = 5)
        val fewerLapses = freshNode("b").copy(nextReviewEpochMs = fixedTime - 1000, lapseCount = 1)
        val queue = scheduler.buildReviewQueue(listOf(fewerLapses, moreLapses))
        assertEquals("a", queue[0].nodeId)
    }

    // --- buildInterleavedSession() tests ---

    @Test
    fun `buildInterleavedSession interleaves white and black`() {
        val nodes = listOf(
            NodeReviewState("w1", Side.WHITE, "l1", nextReviewEpochMs = fixedTime - 1000),
            NodeReviewState("w2", Side.WHITE, "l1", nextReviewEpochMs = fixedTime - 2000),
            NodeReviewState("b1", Side.BLACK, "l2", nextReviewEpochMs = fixedTime - 1000),
            NodeReviewState("b2", Side.BLACK, "l2", nextReviewEpochMs = fixedTime - 2000),
        )
        val session = scheduler.buildInterleavedSession(nodes, 4)
        // Should alternate sides
        val sides = session.map { it.side }
        assertTrue("Should contain both sides", sides.contains(Side.WHITE) && sides.contains(Side.BLACK))
    }

    @Test
    fun `buildInterleavedSession respects maxItems`() {
        val nodes = (1..30).map { i ->
            freshNode("n$i").copy(nextReviewEpochMs = fixedTime - i * 1000L)
        }
        val session = scheduler.buildInterleavedSession(nodes, 10)
        assertTrue("Session should have at most 10 items, got ${session.size}", session.size <= 10)
    }

    @Test
    fun `buildInterleavedSession returns all if under maxItems`() {
        val nodes = listOf(
            freshNode("a").copy(nextReviewEpochMs = fixedTime - 1000),
            freshNode("b").copy(nextReviewEpochMs = fixedTime - 2000),
        )
        val session = scheduler.buildInterleavedSession(nodes, 20)
        assertEquals(2, session.size)
    }
}
