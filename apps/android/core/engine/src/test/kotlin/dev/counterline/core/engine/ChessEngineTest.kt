package dev.counterline.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the existing ChessEngine utility functions —
 * legal move helpers, FEN parsing, and seed line processing.
 */
class ChessEngineTest {

    @Test
    fun `START_FEN is valid`() {
        val fen = ChessEngine.START_FEN
        val parts = fen.split(" ")
        assertEquals(6, parts.size)
        assertEquals("w", parts[1])
        assertEquals("KQkq", parts[2])
        assertEquals("0", parts[4])
        assertEquals("1", parts[5])
    }

    @Test
    fun `sideToMove returns WHITE for starting position`() {
        val side = ChessEngine.sideToMove(ChessEngine.START_FEN)
        assertEquals(dev.counterline.core.model.Side.WHITE, side)
    }

    @Test
    fun `sideToMove returns BLACK after 1 e4`() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val side = ChessEngine.sideToMove(fen)
        assertEquals(dev.counterline.core.model.Side.BLACK, side)
    }

    @Test
    fun `fullMoveNumber returns 1 for starting position`() {
        assertEquals(1, ChessEngine.fullMoveNumber(ChessEngine.START_FEN))
    }

    @Test
    fun `fullMoveNumber returns correct value for later positions`() {
        val fen = "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
        assertEquals(2, ChessEngine.fullMoveNumber(fen))
    }

    @Test
    fun `parseSeedLine extracts SAN moves correctly`() {
        val moves = ChessEngine.parseSeedLine("1. e4 e5 2. Nc3 Nf6")
        assertEquals(listOf("e4", "e5", "Nc3", "Nf6"), moves)
    }

    @Test
    fun `parseSeedLine handles empty string`() {
        val moves = ChessEngine.parseSeedLine("")
        assertTrue(moves.isEmpty() || moves.all { it.isBlank() })
    }

    @Test
    fun `extractPlacement returns just piece placement`() {
        val placement = ChessEngine.extractPlacement(ChessEngine.START_FEN)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", placement)
    }

    @Test
    fun `seedLineToMoves creates correct move list`() {
        val moves = ChessEngine.seedLineToMoves(
            "1. e4 e5 2. Nc3",
            dev.counterline.core.model.Side.WHITE,
        )
        assertEquals(3, moves.size)
        assertEquals("e4", moves[0].san)
        assertTrue(moves[0].isWhiteMove)
        assertEquals("e5", moves[1].san)
        assertFalse(moves[1].isWhiteMove)
        assertEquals("Nc3", moves[2].san)
        assertTrue(moves[2].isWhiteMove)
    }

    @Test
    fun `formatPosition shows move number and side`() {
        val formatted = ChessEngine.formatPosition(ChessEngine.START_FEN)
        assertTrue(formatted.contains("Move 1"))
        assertTrue(formatted.contains("White to move"))
    }

    @Test
    fun `formatPosition shows Black to move`() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val formatted = ChessEngine.formatPosition(fen)
        assertTrue(formatted.contains("Black to move"))
    }
}
