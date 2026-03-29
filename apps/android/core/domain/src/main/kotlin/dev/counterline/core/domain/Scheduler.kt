package dev.counterline.core.domain

import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Spaced repetition scheduler for CounterLine.
 *
 * Implements SM-2 variant with per-node ease factor, interval growth,
 * lapse tracking, and overdue handling.
 */
class ReviewScheduler(
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        private const val MIN_EASE = 1.3f
        private const val DEFAULT_EASE = 2.5f
        private const val MS_PER_DAY = 86_400_000L

        // Initial intervals in days
        private const val FAIL_INTERVAL = 0.007f      // ~10 minutes
        private const val FIRST_GOOD_INTERVAL = 0.042f // ~1 hour
        private const val SECOND_GOOD_INTERVAL = 1f    // 1 day
    }

    /**
     * Given the current review state and the user's grade, return the updated state
     * with new interval, ease, and next review time.
     */
    fun schedule(current: NodeReviewState, grade: ReviewGrade): NodeReviewState {
        val now = clock()

        return when (grade) {
            ReviewGrade.FAIL -> scheduleFail(current, now)
            ReviewGrade.HARD -> scheduleHard(current, now)
            ReviewGrade.GOOD -> scheduleGood(current, now)
            ReviewGrade.EASY -> scheduleEasy(current, now)
        }
    }

    private fun scheduleFail(state: NodeReviewState, now: Long): NodeReviewState {
        val newEase = max(MIN_EASE, state.easeFactor - 0.20f)
        val newInterval = FAIL_INTERVAL
        return state.copy(
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = 0,
            lastReviewEpochMs = now,
            nextReviewEpochMs = now + intervalToMs(newInterval),
            lapseCount = state.lapseCount + 1,
            lastGrade = ReviewGrade.FAIL,
        )
    }

    private fun scheduleHard(state: NodeReviewState, now: Long): NodeReviewState {
        val newEase = max(MIN_EASE, state.easeFactor - 0.15f)
        val reps = state.repetitions + 1
        val newInterval = when {
            reps <= 1 -> FIRST_GOOD_INTERVAL
            reps == 2 -> SECOND_GOOD_INTERVAL
            else -> state.intervalDays * 1.2f
        }
        return state.copy(
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = reps,
            lastReviewEpochMs = now,
            nextReviewEpochMs = now + intervalToMs(newInterval),
            lastGrade = ReviewGrade.HARD,
        )
    }

    private fun scheduleGood(state: NodeReviewState, now: Long): NodeReviewState {
        val reps = state.repetitions + 1
        val newInterval = when {
            reps <= 1 -> FIRST_GOOD_INTERVAL
            reps == 2 -> SECOND_GOOD_INTERVAL
            else -> state.intervalDays * state.easeFactor
        }
        return state.copy(
            intervalDays = newInterval,
            repetitions = reps,
            lastReviewEpochMs = now,
            nextReviewEpochMs = now + intervalToMs(newInterval),
            lastGrade = ReviewGrade.GOOD,
        )
    }

    private fun scheduleEasy(state: NodeReviewState, now: Long): NodeReviewState {
        val newEase = state.easeFactor + 0.15f
        val reps = state.repetitions + 1
        val newInterval = when {
            reps <= 1 -> SECOND_GOOD_INTERVAL
            reps == 2 -> SECOND_GOOD_INTERVAL * 4
            else -> state.intervalDays * state.easeFactor * 1.3f
        }
        return state.copy(
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = reps,
            lastReviewEpochMs = now,
            nextReviewEpochMs = now + intervalToMs(newInterval),
            lastGrade = ReviewGrade.EASY,
        )
    }

    private fun intervalToMs(days: Float): Long = (days * MS_PER_DAY).roundToLong()

    /**
     * Calculate mastery score for a list of review states (0.0 to 1.0).
     * Weights: interval length, ease factor, lapse count.
     */
    fun calculateMastery(states: List<NodeReviewState>): Float {
        if (states.isEmpty()) return 0f
        val scores = states.map { state ->
            val intervalScore = (state.intervalDays / 30f).coerceIn(0f, 1f) // 30-day cap
            val easeScore = ((state.easeFactor - MIN_EASE) / (DEFAULT_EASE + 1f - MIN_EASE)).coerceIn(0f, 1f)
            val lapseScore = (1f - state.lapseCount * 0.1f).coerceIn(0f, 1f)
            intervalScore * 0.5f + easeScore * 0.3f + lapseScore * 0.2f
        }
        return scores.average().toFloat()
    }

    /**
     * Build a daily review queue: all nodes where nextReviewEpochMs <= now,
     * sorted by most overdue first, then by lapse count descending.
     */
    fun buildReviewQueue(states: List<NodeReviewState>): List<NodeReviewState> {
        val now = clock()
        return states
            .filter { it.nextReviewEpochMs <= now }
            .sortedWith(
                compareBy<NodeReviewState> { it.nextReviewEpochMs }
                    .thenByDescending { it.lapseCount },
            )
    }

    /**
     * Build an interleaved session mixing White and Black, different lines.
     */
    fun buildInterleavedSession(
        states: List<NodeReviewState>,
        maxItems: Int = 20,
    ): List<NodeReviewState> {
        val queue = buildReviewQueue(states)
        if (queue.size <= maxItems) return queue

        // Interleave white and black
        val white = queue.filter { it.side == Side.WHITE }.toMutableList()
        val black = queue.filter { it.side == Side.BLACK }.toMutableList()
        val result = mutableListOf<NodeReviewState>()

        while (result.size < maxItems && (white.isNotEmpty() || black.isNotEmpty())) {
            if (white.isNotEmpty()) result.add(white.removeFirst())
            if (result.size < maxItems && black.isNotEmpty()) result.add(black.removeFirst())
        }
        return result
    }
}
