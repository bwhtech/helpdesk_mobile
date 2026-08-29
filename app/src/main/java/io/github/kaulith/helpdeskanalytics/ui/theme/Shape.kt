package io.github.kaulith.helpdeskanalytics.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// M3 Expressive radius scale, larger and more confident than the original
// Frappe values. Cards default to 16dp, dialogs/sheets to 24-28dp, pills are
// fully rounded. Smaller scales kept for chips and inline tokens.
object FrappeRadius {
    val none = RoundedCornerShape(0.dp)
    val sm   = RoundedCornerShape(8.dp)
    val md   = RoundedCornerShape(12.dp)
    val lg   = RoundedCornerShape(16.dp)   // default card
    val largeIncreased = RoundedCornerShape(20.dp)  // M3E
    val xl   = RoundedCornerShape(24.dp)
    val xl2  = RoundedCornerShape(28.dp)   // sheets / dialogs
    val xl3  = RoundedCornerShape(32.dp)   // M3E extra-large-increased
    val xl4  = RoundedCornerShape(48.dp)   // M3E extra-extra-large
    val full = RoundedCornerShape(9999.dp) // pills, FABs
}

// Material3 Shapes mapping for screens still on MaterialTheme.shapes.
val AppShapes = Shapes(
    extraSmall = FrappeRadius.sm,
    small      = FrappeRadius.md,
    medium     = FrappeRadius.lg,
    large      = FrappeRadius.xl2,
    extraLarge = FrappeRadius.xl3
)
