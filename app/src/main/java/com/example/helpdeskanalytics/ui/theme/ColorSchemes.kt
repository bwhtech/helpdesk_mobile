package com.example.helpdeskanalytics.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// Six accent schemes built on Frappe palette families. Ocean is the default,
// closest to the Frappe brand. Each scheme keeps surfaces neutral and varies only
// the primary palette family, so screens read consistently across schemes.
enum class AppColorScheme(val label: String, val key: String, val seed: Color) {
    OCEAN("Ocean Blue", "ocean", FrappePalette.Light.Blue.s600),
    EMERALD("Emerald", "emerald", FrappePalette.Light.Green.s600),
    SUNSET("Sunset", "sunset", FrappePalette.Light.Orange.s500),
    VIOLET("Violet", "violet", FrappePalette.Light.Violet.s500),
    ROSE("Rose", "rose", FrappePalette.Light.Pink.s500),
    SLATE("Slate", "slate", FrappePalette.Light.Gray.s700);

    companion object {
        fun fromKey(key: String): AppColorScheme =
            values().firstOrNull { it.key == key } ?: OCEAN
    }
}

internal val OceanFamily = PaletteFamily(
    l50 = FrappePalette.Light.Blue.s50,
    l100 = FrappePalette.Light.Blue.s100,
    l200 = FrappePalette.Light.Blue.s200,
    l400 = FrappePalette.Light.Blue.s400,
    l500 = FrappePalette.Light.Blue.s500,
    l600 = FrappePalette.Light.Blue.s600,
    l700 = FrappePalette.Light.Blue.s700,
    l800 = FrappePalette.Light.Blue.s800,
    l900 = FrappePalette.Light.Blue.s900,
    d50 = FrappePalette.Dark.Blue.s50,
    d300 = FrappePalette.Dark.Blue.s300,
    d400 = FrappePalette.Dark.Blue.s400,
    d500 = FrappePalette.Dark.Blue.s500,
    d700 = FrappePalette.Dark.Blue.s700,
    d800 = FrappePalette.Dark.Blue.s800,
    d900 = FrappePalette.Dark.Blue.s900
)

private val EmeraldFamily = PaletteFamily(
    l50 = FrappePalette.Light.Green.s50,
    l100 = FrappePalette.Light.Green.s100,
    l200 = FrappePalette.Light.Green.s200,
    l400 = FrappePalette.Light.Green.s400,
    l500 = FrappePalette.Light.Green.s500,
    l600 = FrappePalette.Light.Green.s600,
    l700 = FrappePalette.Light.Green.s700,
    l800 = FrappePalette.Light.Green.s800,
    l900 = FrappePalette.Light.Green.s900,
    d50 = FrappePalette.Dark.Green.s50,
    d300 = FrappePalette.Dark.Green.s300,
    d400 = FrappePalette.Dark.Green.s400,
    d500 = FrappePalette.Dark.Green.s500,
    d700 = FrappePalette.Dark.Green.s700,
    d800 = FrappePalette.Dark.Green.s800,
    d900 = FrappePalette.Dark.Green.s900
)

private val SunsetFamily = PaletteFamily(
    l50 = FrappePalette.Light.Orange.s50,
    l100 = FrappePalette.Light.Orange.s100,
    l200 = FrappePalette.Light.Orange.s200,
    l400 = FrappePalette.Light.Orange.s400,
    l500 = FrappePalette.Light.Orange.s500,
    l600 = FrappePalette.Light.Orange.s600,
    l700 = FrappePalette.Light.Orange.s700,
    l800 = FrappePalette.Light.Orange.s800,
    l900 = FrappePalette.Light.Orange.s900,
    d50 = FrappePalette.Dark.Orange.s50,
    d300 = FrappePalette.Dark.Orange.s300,
    d400 = FrappePalette.Dark.Orange.s400,
    d500 = FrappePalette.Dark.Orange.s500,
    d700 = FrappePalette.Dark.Orange.s700,
    d800 = FrappePalette.Dark.Orange.s800,
    d900 = FrappePalette.Dark.Orange.s900
)

private val VioletFamily = PaletteFamily(
    l50 = FrappePalette.Light.Violet.s50,
    l100 = FrappePalette.Light.Violet.s100,
    l200 = FrappePalette.Light.Violet.s200,
    l400 = FrappePalette.Light.Violet.s400,
    l500 = FrappePalette.Light.Violet.s500,
    l600 = FrappePalette.Light.Violet.s600,
    l700 = FrappePalette.Light.Violet.s700,
    l800 = FrappePalette.Light.Violet.s800,
    l900 = FrappePalette.Light.Violet.s900,
    d50 = FrappePalette.Dark.Violet.s50,
    d300 = FrappePalette.Dark.Violet.s300,
    d400 = FrappePalette.Dark.Violet.s400,
    d500 = FrappePalette.Dark.Violet.s500,
    d700 = FrappePalette.Dark.Violet.s700,
    d800 = FrappePalette.Dark.Violet.s800,
    d900 = FrappePalette.Dark.Violet.s900
)

private val RoseFamily = PaletteFamily(
    l50 = FrappePalette.Light.Pink.s50,
    l100 = FrappePalette.Light.Pink.s100,
    l200 = FrappePalette.Light.Pink.s200,
    l400 = FrappePalette.Light.Pink.s400,
    l500 = FrappePalette.Light.Pink.s500,
    l600 = FrappePalette.Light.Pink.s600,
    l700 = FrappePalette.Light.Pink.s700,
    l800 = FrappePalette.Light.Pink.s800,
    l900 = FrappePalette.Light.Pink.s900,
    d50 = FrappePalette.Light.Pink.s50,
    d300 = FrappePalette.Light.Pink.s300,
    d400 = FrappePalette.Light.Pink.s400,
    d500 = FrappePalette.Light.Pink.s500,
    d700 = FrappePalette.Light.Pink.s700,
    d800 = FrappePalette.Light.Pink.s800,
    d900 = FrappePalette.Light.Pink.s900
)

private val SlateFamily = PaletteFamily(
    l50 = FrappePalette.Light.Gray.s50,
    l100 = FrappePalette.Light.Gray.s100,
    l200 = FrappePalette.Light.Gray.s200,
    l400 = FrappePalette.Light.Gray.s400,
    l500 = FrappePalette.Light.Gray.s500,
    l600 = FrappePalette.Light.Gray.s600,
    l700 = FrappePalette.Light.Gray.s700,
    l800 = FrappePalette.Light.Gray.s800,
    l900 = FrappePalette.Light.Gray.s900,
    d50 = FrappePalette.Dark.Gray.s50,
    d300 = FrappePalette.Dark.Gray.s300,
    d400 = FrappePalette.Dark.Gray.s400,
    d500 = FrappePalette.Dark.Gray.s500,
    d700 = FrappePalette.Dark.Gray.s700,
    d800 = FrappePalette.Dark.Gray.s800,
    d900 = FrappePalette.Dark.Gray.s900
)

internal fun familyFor(scheme: AppColorScheme): PaletteFamily = when (scheme) {
    AppColorScheme.OCEAN -> OceanFamily
    AppColorScheme.EMERALD -> EmeraldFamily
    AppColorScheme.SUNSET -> SunsetFamily
    AppColorScheme.VIOLET -> VioletFamily
    AppColorScheme.ROSE -> RoseFamily
    AppColorScheme.SLATE -> SlateFamily
}

// Companion accents per scheme. Each scheme picks complementary palettes for
// secondary + tertiary so the whole M3 colorScheme, not just primary, shifts
// when the user changes accent in Settings. Status chips and tonal pills key
// off secondary/tertiary containers, so without this they'd look identical
// across all six schemes.
internal fun companionsFor(scheme: AppColorScheme): Pair<PaletteFamily, PaletteFamily> =
    when (scheme) {
        AppColorScheme.OCEAN -> CyanFamily to TealFamily
        AppColorScheme.EMERALD -> TealFamily to AmberFamily
        AppColorScheme.SUNSET -> TealFamily to AmberFamily
        AppColorScheme.VIOLET -> CyanFamily to OceanFamily
        AppColorScheme.ROSE -> VioletFamily to OrangeFamily
        AppColorScheme.SLATE -> OceanFamily to TealFamily
    }

fun getColorScheme(scheme: AppColorScheme, isDark: Boolean): ColorScheme {
    val primary = familyFor(scheme)
    val (secondary, tertiary) = companionsFor(scheme)
    return if (isDark) buildDarkScheme(primary, secondary, tertiary)
    else buildLightScheme(primary, secondary, tertiary)
}

// --- Companion palette families ------------------------------------------------
// Dark slots fall back to nearest available Frappe Dark palette when the source
// hue has no native dark variant (Cyan/Teal/Pink/Yellow are light-only).

private val CyanFamily = PaletteFamily(
    l50 = FrappePalette.Light.Cyan.s50,
    l100 = FrappePalette.Light.Cyan.s100,
    l200 = FrappePalette.Light.Cyan.s200,
    l400 = FrappePalette.Light.Cyan.s400,
    l500 = FrappePalette.Light.Cyan.s500,
    l600 = FrappePalette.Light.Cyan.s600,
    l700 = FrappePalette.Light.Cyan.s700,
    l800 = FrappePalette.Light.Cyan.s800,
    l900 = FrappePalette.Light.Cyan.s800,
    d50 = FrappePalette.Light.Cyan.s50,
    d300 = FrappePalette.Dark.Blue.s300,
    d400 = FrappePalette.Dark.Blue.s400,
    d500 = FrappePalette.Dark.Blue.s500,
    d700 = FrappePalette.Dark.Blue.s700,
    d800 = FrappePalette.Dark.Blue.s800,
    d900 = FrappePalette.Dark.Blue.s900
)

private val TealFamily = PaletteFamily(
    l50 = FrappePalette.Light.Teal.s50,
    l100 = FrappePalette.Light.Teal.s100,
    l200 = FrappePalette.Light.Teal.s200,
    l400 = FrappePalette.Light.Teal.s400,
    l500 = FrappePalette.Light.Teal.s500,
    l600 = FrappePalette.Light.Teal.s600,
    l700 = FrappePalette.Light.Teal.s700,
    l800 = FrappePalette.Light.Teal.s800,
    l900 = FrappePalette.Light.Teal.s800,
    d50 = FrappePalette.Light.Teal.s50,
    d300 = FrappePalette.Dark.Green.s300,
    d400 = FrappePalette.Dark.Green.s400,
    d500 = FrappePalette.Dark.Green.s500,
    d700 = FrappePalette.Dark.Green.s700,
    d800 = FrappePalette.Dark.Green.s800,
    d900 = FrappePalette.Dark.Green.s900
)

private val AmberFamily = PaletteFamily(
    l50 = FrappePalette.Light.Amber.s50,
    l100 = FrappePalette.Light.Amber.s100,
    l200 = FrappePalette.Light.Amber.s200,
    l400 = FrappePalette.Light.Amber.s400,
    l500 = FrappePalette.Light.Amber.s500,
    l600 = FrappePalette.Light.Amber.s600,
    l700 = FrappePalette.Light.Amber.s700,
    l800 = FrappePalette.Light.Amber.s800,
    l900 = FrappePalette.Light.Amber.s800,
    d50 = FrappePalette.Dark.Amber.s50,
    d300 = FrappePalette.Dark.Amber.s300,
    d400 = FrappePalette.Dark.Amber.s400,
    d500 = FrappePalette.Dark.Amber.s500,
    d700 = FrappePalette.Dark.Amber.s700,
    d800 = FrappePalette.Dark.Amber.s800,
    d900 = FrappePalette.Dark.Amber.s900
)

private val OrangeFamily = PaletteFamily(
    l50 = FrappePalette.Light.Orange.s50,
    l100 = FrappePalette.Light.Orange.s100,
    l200 = FrappePalette.Light.Orange.s200,
    l400 = FrappePalette.Light.Orange.s400,
    l500 = FrappePalette.Light.Orange.s500,
    l600 = FrappePalette.Light.Orange.s600,
    l700 = FrappePalette.Light.Orange.s700,
    l800 = FrappePalette.Light.Orange.s800,
    l900 = FrappePalette.Light.Orange.s900,
    d50 = FrappePalette.Dark.Orange.s50,
    d300 = FrappePalette.Dark.Orange.s300,
    d400 = FrappePalette.Dark.Orange.s400,
    d500 = FrappePalette.Dark.Orange.s500,
    d700 = FrappePalette.Dark.Orange.s700,
    d800 = FrappePalette.Dark.Orange.s800,
    d900 = FrappePalette.Dark.Orange.s900
)
