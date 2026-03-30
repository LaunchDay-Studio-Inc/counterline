package dev.counterline.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Primary ── Deep walnut brown: the wood of a tournament board
val PrimaryLight = Color(0xFF5D4037)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEDE0D4)
val OnPrimaryContainerLight = Color(0xFF321911)

val PrimaryDark = Color(0xFFD7CCC8)
val OnPrimaryDark = Color(0xFF321911)
val PrimaryContainerDark = Color(0xFF4E342E)
val OnPrimaryContainerDark = Color(0xFFEFEBE9)

// ── Secondary ── Muted sage green for positive/analysis signals
val SecondaryLight = Color(0xFF4A7C59)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCDE7D3)
val OnSecondaryContainerLight = Color(0xFF0B3D1D)

val SecondaryDark = Color(0xFFA3D9B1)
val OnSecondaryDark = Color(0xFF0B3D1D)
val SecondaryContainerDark = Color(0xFF2E5E3F)
val OnSecondaryContainerDark = Color(0xFFD7F0DD)

// ── Tertiary ── Deep slate blue for strategic depth & plans
val TertiaryLight = Color(0xFF37598A)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD2E4FF)
val OnTertiaryContainerLight = Color(0xFF001D36)

val TertiaryDark = Color(0xFFA8C8F0)
val OnTertiaryDark = Color(0xFF0A3057)
val TertiaryContainerDark = Color(0xFF214270)
val OnTertiaryContainerDark = Color(0xFFD6E9FF)

// ── Error ── Warm red, not aggressive
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ── Neutral surfaces ── Warm-shifted off-white / off-black
val BackgroundLight = Color(0xFFFFFBF8)
val OnBackgroundLight = Color(0xFF1D1B19)
val SurfaceLight = Color(0xFFFFFBF8)
val OnSurfaceLight = Color(0xFF1D1B19)
val SurfaceVariantLight = Color(0xFFF0E6DC)
val OnSurfaceVariantLight = Color(0xFF504539)
val OutlineLight = Color(0xFF847568)
val OutlineVariantLight = Color(0xFFD5C5B6)
val SurfaceDimLight = Color(0xFFE0D8D0)
val SurfaceBrightLight = Color(0xFFFFFBF8)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFFBF3EB)
val SurfaceContainerLight = Color(0xFFF5EDE5)
val SurfaceContainerHighLight = Color(0xFFEFE7DF)
val SurfaceContainerHighestLight = Color(0xFFE9E1D9)

val BackgroundDark = Color(0xFF1D1B19)
val OnBackgroundDark = Color(0xFFE8E1D9)
val SurfaceDark = Color(0xFF1D1B19)
val OnSurfaceDark = Color(0xFFE8E1D9)
val SurfaceVariantDark = Color(0xFF504539)
val OnSurfaceVariantDark = Color(0xFFD5C5B6)
val OutlineDark = Color(0xFF9D8E81)
val OutlineVariantDark = Color(0xFF504539)
val SurfaceDimDark = Color(0xFF1D1B19)
val SurfaceBrightDark = Color(0xFF44403C)
val SurfaceContainerLowestDark = Color(0xFF121110)
val SurfaceContainerLowDark = Color(0xFF262320)
val SurfaceContainerDark = Color(0xFF2A2724)
val SurfaceContainerHighDark = Color(0xFF35312E)
val SurfaceContainerHighestDark = Color(0xFF403C38)

// ── Board colors ──
val BoardLightSquare = Color(0xFFF0D9B5)
val BoardDarkSquare = Color(0xFFB58863)
val BoardHighlight = Color(0x6646B84A)
val BoardLastMove = Color(0x55CDA435)
val BoardCheck = Color(0x66E53935)
val BoardArrow = Color(0xBB3B82F6)
val BoardAnnotation = Color(0xBBF59E0B)

// Colorblind-safe alternate board
val BoardLightSquareCB = Color(0xFFE8E2D0)
val BoardDarkSquareCB = Color(0xFF7B9AAD)

// ── Evaluation bar ──
val EvalWhiteAdvantage = Color(0xFFF5F5F5)
val EvalBlackAdvantage = Color(0xFF2C2C2C)
val EvalEqual = Color(0xFF9E9E9E)

// ── Extended chess tokens ──
@Immutable
data class ChessColors(
    val boardLight: Color,
    val boardDark: Color,
    val highlight: Color,
    val lastMove: Color,
    val check: Color,
    val arrow: Color,
    val annotation: Color,
    val evalWhite: Color,
    val evalBlack: Color,
    val evalEqual: Color,
    val correctMove: Color,
    val incorrectMove: Color,
    val masteryHigh: Color,
    val masteryMedium: Color,
    val masteryLow: Color,
    val streakActive: Color,
    val streakInactive: Color,
    val whiteWeapon: Color,
    val blackWeapon: Color,
)

val LightChessColors = ChessColors(
    boardLight = BoardLightSquare,
    boardDark = BoardDarkSquare,
    highlight = BoardHighlight,
    lastMove = BoardLastMove,
    check = BoardCheck,
    arrow = BoardArrow,
    annotation = BoardAnnotation,
    evalWhite = EvalWhiteAdvantage,
    evalBlack = EvalBlackAdvantage,
    evalEqual = EvalEqual,
    correctMove = Color(0xFF2E7D32),
    incorrectMove = Color(0xFFC62828),
    masteryHigh = Color(0xFF2E7D32),
    masteryMedium = Color(0xFFED6C02),
    masteryLow = Color(0xFFD32F2F),
    streakActive = Color(0xFFFF8F00),
    streakInactive = Color(0xFFE0D8D0),
    whiteWeapon = Color(0xFF5D4037),
    blackWeapon = Color(0xFF37598A),
)

val DarkChessColors = ChessColors(
    boardLight = Color(0xFFD4B88A),
    boardDark = Color(0xFF8B6B47),
    highlight = Color(0x6666BB6A),
    lastMove = Color(0x55FFD54F),
    check = Color(0x66EF5350),
    arrow = Color(0xBB60A5FA),
    annotation = Color(0xBBFBBF24),
    evalWhite = Color(0xFFE0E0E0),
    evalBlack = Color(0xFF424242),
    evalEqual = Color(0xFF757575),
    correctMove = Color(0xFF66BB6A),
    incorrectMove = Color(0xFFEF5350),
    masteryHigh = Color(0xFF66BB6A),
    masteryMedium = Color(0xFFFFB74D),
    masteryLow = Color(0xFFEF5350),
    streakActive = Color(0xFFFFB300),
    streakInactive = Color(0xFF44403C),
    whiteWeapon = Color(0xFFD7CCC8),
    blackWeapon = Color(0xFFA8C8F0),
)

val LocalChessColors = staticCompositionLocalOf { LightChessColors }

// Legacy aliases for backward compatibility
val BoardLight = BoardLightSquare
val BoardDark = BoardDarkSquare
val BoardLastMove = BoardLastMove
