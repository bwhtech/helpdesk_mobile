@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.helpdeskanalytics.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.helpdeskanalytics.R

// Inter Variable. Single TTF, weight via variation axis.
private fun interVar(weight: Int) = Font(
    R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val InterFontFamily = FontFamily(
    interVar(400),
    interVar(420),
    interVar(500),
    interVar(600),
    interVar(700),
    interVar(800)
)

// Frappe text tokens. M3-Expressive-leaning: body sizes stay close to Frappe
// for density parity, but display/headline weights are bumped so hero areas
// read as confident rather than understated.
object FrappeTextStyles {
    val xs2 = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(420), fontSize = 11.sp, lineHeight = 12.65.sp, letterSpacing = 0.11.sp)
    val xs  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(420), fontSize = 12.sp, lineHeight = 13.8.sp, letterSpacing = 0.24.sp)
    val sm  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(420), fontSize = 13.sp, lineHeight = 14.95.sp, letterSpacing = 0.26.sp)
    val base = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(420), fontSize = 14.sp, lineHeight = 16.1.sp, letterSpacing = 0.28.sp)
    val lg  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 18.4.sp, letterSpacing = 0.32.sp)
    val xl  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 20.7.sp, letterSpacing = 0.18.sp)
    val xl2 = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 20.sp, lineHeight = 23.sp, letterSpacing = 0.2.sp)
    val xl3 = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 24.sp, lineHeight = 27.6.sp, letterSpacing = 0.12.sp)

    // Paragraph variants (looser line-height for body copy).
    val pXs2 = xs2.copy(lineHeight = 17.6.sp)
    val pXs  = xs.copy(lineHeight = 19.2.sp)
    val pSm  = sm.copy(lineHeight = 19.5.sp)
    val pBase = base.copy(lineHeight = 21.sp)
    val pLg  = lg.copy(lineHeight = 24.sp)
    val pXl  = xl.copy(lineHeight = 25.56.sp)
    val pXl2 = xl2.copy(lineHeight = 27.6.sp)

    // Hero display, reserved for big landing moments (dashboard greeting,
    // login wordmark, leaderboard podium). Tight letter spacing, bold weight,
    // no surrounding decoration needed.
    val displayHero = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight(700),
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    )
}

// Map Frappe → Material3 Typography for screens still using MaterialTheme.typography.
val AppTypography = Typography(
    displayLarge   = FrappeTextStyles.displayHero,
    displayMedium  = FrappeTextStyles.xl3.copy(fontWeight = FontWeight(700), fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.25).sp),
    displaySmall   = FrappeTextStyles.xl3.copy(fontWeight = FontWeight(700)),
    headlineLarge  = FrappeTextStyles.xl2.copy(fontWeight = FontWeight(700)),
    headlineMedium = FrappeTextStyles.xl.copy(fontWeight = FontWeight(600)),
    headlineSmall  = FrappeTextStyles.lg.copy(fontWeight = FontWeight(600)),
    titleLarge     = FrappeTextStyles.lg.copy(fontWeight = FontWeight(600)),
    titleMedium    = FrappeTextStyles.base.copy(fontWeight = FontWeight(600)),
    titleSmall     = FrappeTextStyles.sm.copy(fontWeight = FontWeight(600)),
    bodyLarge      = FrappeTextStyles.pLg,
    bodyMedium     = FrappeTextStyles.pBase,
    bodySmall      = FrappeTextStyles.pSm,
    labelLarge     = FrappeTextStyles.base.copy(fontWeight = FontWeight(500)),
    labelMedium    = FrappeTextStyles.sm.copy(fontWeight = FontWeight(500)),
    labelSmall     = FrappeTextStyles.xs.copy(fontWeight = FontWeight(500))
)
