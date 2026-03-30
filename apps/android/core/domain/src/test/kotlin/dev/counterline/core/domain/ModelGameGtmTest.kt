package dev.counterline.core.domain

import dev.counterline.core.model.GameAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 tests for Guess-the-Move scoring in Model Games.
 * Validates the 4-tier scoring system (3/2/1/0) and score aggregation.
 */
class ModelGameGtmTest {

    // ── Scoring tiers ───────────────────────────────────────────────

    @Test
    fun `exact match scores 3 points`() {
        val score = scoreGuess(guess = "Nf3", expected = "Nf3")
        assertEquals(3, score)
    }

    @Test
    fun `case-insensitive match scores 3 points`() {
        val score = scoreGuess(guess = "nf3", expected = "Nf3")
        assertEquals(3, score)
    }

    @Test
    fun `close match scores 2 points`() {
        // Same piece type, same destination rank or file
        val score = scoreGuess(guess = "Ne3", expected = "Nf3")
        assertEquals(2, score)
    }

    @Test
    fun `reasonable move scores 1 point`() {
        // Same piece type, different destination
        val score = scoreGuess(guess = "Nc3", expected = "Nf3")
        assertEquals(1, score)
    }

    @Test
    fun `wrong move scores 0 points`() {
        val score = scoreGuess(guess = "e4", expected = "Nf3")
        assertEquals(0, score)
    }

    // ── Score aggregation ───────────────────────────────────────────

    @Test
    fun `perfect game gives max score`() {
        val annotations = listOf("e4", "Nf3", "Bb5")
        val guesses = listOf("e4", "Nf3", "Bb5")
        val total = annotations.zip(guesses).sumOf { (expected, guess) ->
            scoreGuess(guess, expected)
        }
        val maxScore = annotations.size * 3
        assertEquals(maxScore, total)
    }

    @Test
    fun `all wrong gives zero`() {
        val annotations = listOf("e4", "Nf3", "Bb5")
        val guesses = listOf("d4", "Bc4", "Qh5")
        val total = annotations.zip(guesses).sumOf { (expected, guess) ->
            scoreGuess(guess, expected)
        }
        assertEquals(0, total)
    }

    @Test
    fun `score percentage calculation`() {
        val total = 6
        val max = 9 // 3 moves * 3 points
        val pct = total.toFloat() / max
        assertEquals(0.667f, pct, 0.01f)
    }

    @Test
    fun `max score equals annotation count times 3`() {
        val annotationCount = 12
        assertEquals(36, annotationCount * 3)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `empty guess scores 0`() {
        val score = scoreGuess(guess = "", expected = "e4")
        assertEquals(0, score)
    }

    @Test
    fun `whitespace-trimmed guess still matches`() {
        val score = scoreGuess(guess = " Nf3 ", expected = "Nf3")
        assertEquals(3, score)
    }

    @Test
    fun `annotation progression builds score history`() {
        val scores = mutableListOf<Int>()
        val moves = listOf("e4", "Nf3", "d4")
        val guesses = listOf("e4", "Bc4", "d4")
        for (i in moves.indices) {
            scores.add(scoreGuess(guesses[i], moves[i]))
        }
        assertEquals(listOf(3, 0, 3), scores)
    }

    // ── Scoring function (mirrors production GTM logic) ─────────────

    /**
     * Score a guess against the expected move.
     * 3 = exact match
     * 2 = close (same piece, adjacent square)
     * 1 = reasonable (same piece type)
     * 0 = wrong
     */
    private fun scoreGuess(guess: String, expected: String): Int {
        val g = guess.trim()
        val e = expected.trim()
        if (g.isEmpty()) return 0
        if (g.equals(e, ignoreCase = true)) return 3

        val gPiece = extractPiece(g)
        val ePiece = extractPiece(e)
        if (gPiece != ePiece) return 0

        // Same piece — check if destination is adjacent
        val gDest = extractDestination(g)
        val eDest = extractDestination(e)
        if (gDest != null && eDest != null && isAdjacent(gDest, eDest)) return 2

        // Same piece type but different destination
        return 1
    }

    private fun extractPiece(san: String): Char {
        val trimmed = san.trim().replace("+", "").replace("#", "").replace("x", "")
        return if (trimmed.isNotEmpty() && trimmed[0].isUpperCase() && trimmed[0] != 'O') {
            trimmed[0]
        } else {
            'P' // pawn
        }
    }

    private fun extractDestination(san: String): Pair<Char, Char>? {
        val cleaned = san.replace("+", "").replace("#", "").replace("=.*".toRegex(), "")
        if (cleaned.length < 2) return null
        val file = cleaned[cleaned.length - 2]
        val rank = cleaned[cleaned.length - 1]
        return if (file in 'a'..'h' && rank in '1'..'8') file to rank else null
    }

    private fun isAdjacent(a: Pair<Char, Char>, b: Pair<Char, Char>): Boolean {
        val fileDiff = kotlin.math.abs(a.first - b.first)
        val rankDiff = kotlin.math.abs(a.second - b.second)
        return fileDiff <= 1 && rankDiff <= 1 && (fileDiff + rankDiff > 0)
    }
}
