package dev.counterline.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.counterline.core.designsystem.accessibility.describeFenForAccessibility
import dev.counterline.core.designsystem.theme.BoardDark
import dev.counterline.core.designsystem.theme.BoardHighlight
import dev.counterline.core.designsystem.theme.BoardLastMove
import dev.counterline.core.designsystem.theme.BoardLight
import dev.counterline.core.designsystem.theme.ChessShapes
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Elevation

private val PIECE_SYMBOLS = mapOf(
    'K' to "\u2654", 'Q' to "\u2655", 'R' to "\u2656",
    'B' to "\u2657", 'N' to "\u2658", 'P' to "\u2659",
    'k' to "\u265A", 'q' to "\u265B", 'r' to "\u265C",
    'b' to "\u265D", 'n' to "\u265E", 'p' to "\u265F",
)

/**
 * Renders a chess board from a FEN string using Compose Canvas.
 * Uses Unicode chess symbols for pieces.
 *
 * Features:
 * - Rounded corners with subtle shadow for premium feel
 * - Last-move highlighting
 * - Accessible FEN description for TalkBack
 * - Respects chess color tokens from theme
 */
@Composable
fun ChessBoard(
    fen: String,
    modifier: Modifier = Modifier,
    flipped: Boolean = false,
    highlightSquares: Set<Int> = emptySet(),
    lastMoveSquares: Set<Int> = emptySet(),
) {
    val textMeasurer = rememberTextMeasurer()
    val board = parseFen(fen)
    val chessColors = CounterLineTheme.chessColors
    val accessibilityDescription = describeFenForAccessibility(fen)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(Elevation.medium, ChessShapes.board)
            .clip(ChessShapes.board)
            .semantics { contentDescription = accessibilityDescription },
    ) {
        val squareSize = size.width / 8f

        for (rank in 0 until 8) {
            for (file in 0 until 8) {
                val displayRank = if (flipped) rank else 7 - rank
                val displayFile = if (flipped) 7 - file else file
                val index = displayRank * 8 + displayFile

                val x = file * squareSize
                val y = rank * squareSize

                // Draw square
                val isLight = (rank + file) % 2 == 0
                drawRect(
                    color = if (isLight) chessColors.boardLight else chessColors.boardDark,
                    topLeft = Offset(x, y),
                    size = Size(squareSize, squareSize),
                )

                // Draw last-move indicator (subtle yellow tint)
                if (index in lastMoveSquares) {
                    drawRect(
                        color = chessColors.lastMove,
                        topLeft = Offset(x, y),
                        size = Size(squareSize, squareSize),
                    )
                }

                // Draw highlight (green for legal moves / selected)
                if (index in highlightSquares) {
                    drawRect(
                        color = chessColors.highlight,
                        topLeft = Offset(x, y),
                        size = Size(squareSize, squareSize),
                    )
                }

                // Draw piece
                val piece = board[index]
                if (piece != null) {
                    val symbol = PIECE_SYMBOLS[piece] ?: ""
                    drawPieceText(
                        textMeasurer = textMeasurer,
                        text = symbol,
                        x = x,
                        y = y,
                        squareSize = squareSize,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPieceText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    squareSize: Float,
) {
    val style = TextStyle(
        fontSize = (squareSize * 0.7f).sp,
        fontWeight = FontWeight.Normal,
        color = Color.Black,
    )
    // Approximate centering: measure the text and offset
    val measured = textMeasurer.measure(text, style)
    val offsetX = x + (squareSize - measured.size.width) / 2f
    val offsetY = y + (squareSize - measured.size.height) / 2f
    drawText(
        textMeasurer = textMeasurer,
        text = text,
        style = style,
        topLeft = Offset(offsetX, offsetY),
    )
}

/**
 * Parse a FEN string into a 64-element array.
 * Index 0 = a8, index 63 = h1 (rank 8 first, left to right).
 */
internal fun parseFen(fen: String): Array<Char?> {
    val board = arrayOfNulls<Char>(64)
    val placement = fen.split(" ").firstOrNull() ?: return board
    val ranks = placement.split("/")

    for ((rankIdx, rank) in ranks.withIndex()) {
        var fileIdx = 0
        for (ch in rank) {
            if (ch.isDigit()) {
                fileIdx += ch.digitToInt()
            } else {
                val index = rankIdx * 8 + fileIdx
                if (index in 0 until 64) {
                    board[index] = ch
                }
                fileIdx++
            }
        }
    }
    return board
}
