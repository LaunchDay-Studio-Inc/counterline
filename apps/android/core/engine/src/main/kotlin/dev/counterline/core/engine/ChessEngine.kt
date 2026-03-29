package dev.counterline.core.engine

import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.Side

/**
 * Lightweight chess position logic — no engine evaluation,
 * just FEN manipulation, move application, and board state queries.
 */
object ChessEngine {

    /** Starting position FEN */
    const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    /**
     * Apply a sequence of SAN moves to the starting position
     * and return the resulting FEN. This is a simplified implementation
     * that applies moves from a known seed line.
     */
    fun applyMoves(seedLine: String): String {
        // For display purposes, we store the exit FEN directly in repertoire data.
        // This function serves as a stub for future UCI integration.
        return START_FEN
    }

    /** Extract just the piece placement from a FEN */
    fun extractPlacement(fen: String): String =
        fen.split(" ").firstOrNull() ?: fen

    /** Determine whose turn it is from a FEN */
    fun sideToMove(fen: String): Side {
        val parts = fen.split(" ")
        return if (parts.size >= 2 && parts[1] == "b") Side.BLACK else Side.WHITE
    }

    /** Get the full-move number from a FEN */
    fun fullMoveNumber(fen: String): Int {
        val parts = fen.split(" ")
        return if (parts.size >= 6) parts[5].toIntOrNull() ?: 1 else 1
    }

    /** Convert a seed line string (e.g. "1. e4 e5 2. Nc3") to a list of SAN moves */
    fun parseSeedLine(seedLine: String): List<String> =
        seedLine.split("\\s+".toRegex())
            .filter { !it.contains('.') && it.isNotBlank() }

    /** Build a list of RepertoireMove objects from a seed line */
    fun seedLineToMoves(seedLine: String, side: Side): List<RepertoireMove> {
        val sans = parseSeedLine(seedLine)
        val moves = mutableListOf<RepertoireMove>()
        var moveNum = 1
        var isWhite = true

        for (san in sans) {
            moves.add(
                RepertoireMove(
                    moveNumber = moveNum,
                    san = san,
                    purpose = "",
                    isWhiteMove = isWhite,
                ),
            )
            if (!isWhite) moveNum++
            isWhite = !isWhite
        }
        return moves
    }

    /** Format a FEN snippet for display (just pieces, not full FEN) */
    fun formatPosition(fen: String): String {
        val parts = fen.split(" ")
        val turn = if (parts.size >= 2) {
            if (parts[1] == "w") "White to move" else "Black to move"
        } else "Unknown"
        val moveNum = fullMoveNumber(fen)
        return "Move $moveNum — $turn"
    }
}
