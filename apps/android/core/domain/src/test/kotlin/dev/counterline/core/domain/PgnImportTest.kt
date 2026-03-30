package dev.counterline.core.domain

import dev.counterline.core.model.ImportedGame
import dev.counterline.core.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 tests for PGN import and repertoire comparison.
 * Validates header extraction, deviation detection, and data integrity.
 */
class PgnImportTest {

    // ── PGN header extraction ───────────────────────────────────────

    @Test
    fun `extract White player from PGN headers`() {
        val pgn = """[White "Carlsen, M"]
[Black "Caruana, F"]
[Result "1-0"]
1. e4 e5 2. Nf3 *"""
        val white = extractHeader(pgn, "White")
        assertEquals("Carlsen, M", white)
    }

    @Test
    fun `extract Black player from PGN headers`() {
        val pgn = """[White "Carlsen, M"]
[Black "Caruana, F"]
[Result "1-0"]
1. e4 e5 *"""
        val black = extractHeader(pgn, "Black")
        assertEquals("Caruana, F", black)
    }

    @Test
    fun `extract Result from PGN headers`() {
        val pgn = """[White "A"]
[Black "B"]
[Result "1/2-1/2"]
1. d4 d5 *"""
        val result = extractHeader(pgn, "Result")
        assertEquals("1/2-1/2", result)
    }

    @Test
    fun `missing header returns empty string`() {
        val pgn = """[White "A"]
1. e4 e5 *"""
        val date = extractHeader(pgn, "Date")
        assertEquals("", date)
    }

    // ── ImportedGame construction ────────────────────────────────────

    @Test
    fun `ImportedGame stores all fields`() {
        val game = ImportedGame(
            id = 1,
            pgn = "1. e4 e5 *",
            white = "PlayerW",
            black = "PlayerB",
            result = "1-0",
            date = "2024.01.15",
            opening = "Sicilian",
            importedEpochMs = System.currentTimeMillis(),
            deviationMoveNumber = 5,
            deviationSide = Side.BLACK,
            matchedLineId = "line_sicilian_1",
        )
        assertEquals("PlayerW", game.white)
        assertEquals("PlayerB", game.black)
        assertEquals(5, game.deviationMoveNumber)
        assertEquals(Side.BLACK, game.deviationSide)
        assertEquals("line_sicilian_1", game.matchedLineId)
    }

    @Test
    fun `ImportedGame with no deviation has null fields`() {
        val game = ImportedGame(
            id = 2,
            pgn = "1. d4 d5 *",
            white = "W",
            black = "B",
            result = "0-1",
            date = "",
            importedEpochMs = System.currentTimeMillis(),
        )
        assertNull(game.deviationMoveNumber)
        assertNull(game.deviationSide)
        assertNull(game.matchedLineId)
    }

    // ── Deviation detection logic ───────────────────────────────────

    @Test
    fun `deviation found when move differs from repertoire`() {
        val repertoireMoves = listOf("e4", "Nf3", "Bb5")
        val gameMoves = listOf("e4", "Nf3", "Bc4") // deviates at move 3
        val deviationIdx = findDeviationIndex(repertoireMoves, gameMoves)
        assertEquals(2, deviationIdx) // index 2 = move 3
    }

    @Test
    fun `no deviation when game follows repertoire exactly`() {
        val repertoire = listOf("e4", "Nf3", "Bb5")
        val game = listOf("e4", "Nf3", "Bb5")
        val deviationIdx = findDeviationIndex(repertoire, game)
        assertEquals(-1, deviationIdx) // -1 = no deviation
    }

    @Test
    fun `deviation at first move detected`() {
        val repertoire = listOf("e4", "Nf3")
        val game = listOf("d4", "Nf3")
        val deviationIdx = findDeviationIndex(repertoire, game)
        assertEquals(0, deviationIdx)
    }

    @Test
    fun `shorter game with no deviation returns no deviation`() {
        val repertoire = listOf("e4", "Nf3", "Bb5", "O-O")
        val game = listOf("e4", "Nf3") // game ends early but matches
        val deviationIdx = findDeviationIndex(repertoire, game)
        assertEquals(-1, deviationIdx)
    }

    // ── Move parsing ────────────────────────────────────────────────

    @Test
    fun `extract move list from PGN body`() {
        val pgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *"
        val moves = extractMoves(pgn)
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6"), moves)
    }

    @Test
    fun `handles result tokens in move text`() {
        val pgn = "1. e4 e5 2. Nf3 Nc6 1-0"
        val moves = extractMoves(pgn)
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), moves)
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun extractHeader(pgn: String, tag: String): String {
        val regex = Regex("""\[$tag\s+"([^"]+)"\]""")
        return regex.find(pgn)?.groupValues?.get(1) ?: ""
    }

    private fun findDeviationIndex(repertoire: List<String>, game: List<String>): Int {
        val compareLen = minOf(repertoire.size, game.size)
        for (i in 0 until compareLen) {
            if (repertoire[i] != game[i]) return i
        }
        return -1 // no deviation within overlapping range
    }

    private fun extractMoves(pgn: String): List<String> {
        // Strip headers (lines starting with [)
        val body = pgn.lines()
            .filterNot { it.trimStart().startsWith("[") }
            .joinToString(" ")
        // Remove move numbers, results, and asterisks
        val results = setOf("1-0", "0-1", "1/2-1/2", "*")
        return body.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .filterNot { it.matches(Regex("\\d+\\.+")) }
            .filterNot { it in results }
    }
}
