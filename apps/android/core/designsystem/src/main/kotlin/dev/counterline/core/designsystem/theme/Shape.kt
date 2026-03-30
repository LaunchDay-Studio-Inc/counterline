package dev.counterline.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens — a 4dp base grid with named semantic sizes.
 * Use these instead of raw .dp values for consistent rhythm.
 */
@Immutable
object Spacing {
    /** 4dp — hairline gaps, icon-to-text */
    val xxs = 4.dp
    /** 8dp — tight internal padding */
    val xs = 8.dp
    /** 12dp — default gap between related elements */
    val sm = 12.dp
    /** 16dp — standard card padding, section gaps */
    val md = 16.dp
    /** 20dp — between card groups */
    val lg = 20.dp
    /** 24dp — screen edge padding, section dividers */
    val xl = 24.dp
    /** 32dp — major section breaks */
    val xxl = 32.dp
    /** 48dp — hero spacing, breathing room */
    val xxxl = 48.dp
}

/**
 * Custom shape tokens beyond the M3 defaults.
 * Board corners, cards, and chips have intentionally distinct radii
 * to create visual hierarchy.
 */
val CounterLineShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, small badges
    small = RoundedCornerShape(8.dp),        // option cards, inline elements
    medium = RoundedCornerShape(12.dp),      // standard cards
    large = RoundedCornerShape(16.dp),       // prominent cards, dialogs
    extraLarge = RoundedCornerShape(24.dp),  // hero cards, bottom sheets
)

/** Card-specific shapes */
@Immutable
object ChessShapes {
    /** Board clip shape — slightly rounded for premium feel */
    val board = RoundedCornerShape(8.dp)
    /** Weapon card — distinctive large radius */
    val weaponCard = RoundedCornerShape(20.dp)
    /** Stat card — pill-adjacent */
    val statCard = RoundedCornerShape(16.dp)
    /** Badge — fully rounded */
    val badge = RoundedCornerShape(50)
    /** Drill answer option */
    val drillOption = RoundedCornerShape(12.dp)
    /** Progress ring container */
    val progressContainer = RoundedCornerShape(16.dp)
    /** Bottom sheet / modal */
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

/**
 * Elevation tokens for consistent shadow hierarchy.
 */
@Immutable
object Elevation {
    /** Flat — used for inline content */
    val none = 0.dp
    /** Resting cards — subtle lift */
    val low = 1.dp
    /** Interactive cards — perceptible shadow */
    val medium = 2.dp
    /** Prominent/focused cards — clear separation */
    val high = 4.dp
    /** Floating elements — FABs, dialogs */
    val highest = 8.dp
}

val LocalSpacing = staticCompositionLocalOf { Spacing }
