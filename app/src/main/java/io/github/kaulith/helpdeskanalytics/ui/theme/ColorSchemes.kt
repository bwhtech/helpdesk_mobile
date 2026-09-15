package io.github.kaulith.helpdeskanalytics.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// Four accent schemes built on Frappe palette families. Ocean is the default,
// closest to the Frappe brand. Each scheme keeps surfaces neutral and varies only
// the primary palette family, so screens read consistently across schemes.
enum class AppColorScheme(val label: String, val key: String, val seed: Color) {
    OCEAN("Ocean Blue", "ocean", FrappePalette.Light.Blue.s600),
    EMERALD("Emerald", "emerald", FrappePalette.Light.Green.s600),
    VIOLET("Violet", "violet", FrappePalette.Light.Violet.s500),
    SLATE("Slate", "slate", FrappePalette.Light.Gray.s700);

    companion object {
        fun fromKey(key: String): AppColorScheme =
            values().firstOrNull { it.key == key } ?: OCEAN
    }
}

internal val OceanFamily = PaletteFamily(
    l100 = FrappePalette.Light.Blue.s100,
    l200 = FrappePalette.Light.Blue.s200,
    l600 = FrappePalette.Light.Blue.s600,
    l900 = FrappePalette.Light.Blue.s900,
    d50 = FrappePalette.Dark.Blue.s50,
    d400 = FrappePalette.Dark.Blue.s400,
    d700 = FrappePalette.Dark.Blue.s700,
    d800 = FrappePalette.Dark.Blue.s800
)

private val EmeraldFamily = PaletteFamily(
    l100 = FrappePalette.Light.Green.s100,
    l200 = FrappePalette.Light.Green.s200,
    l600 = FrappePalette.Light.Green.s600,
    l900 = FrappePalette.Light.Green.s900,
    d50 = FrappePalette.Dark.Green.s50,
    d400 = FrappePalette.Dark.Green.s400,
    d700 = FrappePalette.Dark.Green.s700,
    d800 = FrappePalette.Dark.Green.s800
)

private val VioletFamily = PaletteFamily(
    l100 = FrappePalette.Light.Violet.s100,
    l200 = FrappePalette.Light.Violet.s200,
    l600 = FrappePalette.Light.Violet.s600,
    l900 = FrappePalette.Light.Violet.s900,
    d50 = FrappePalette.Dark.Violet.s50,
    d400 = FrappePalette.Dark.Violet.s400,
    d700 = FrappePalette.Dark.Violet.s700,
    d800 = FrappePalette.Dark.Violet.s800
)

private val SlateFamily = PaletteFamily(
    l100 = FrappePalette.Light.Gray.s100,
    l200 = FrappePalette.Light.Gray.s200,
    l600 = FrappePalette.Light.Gray.s600,
    l900 = FrappePalette.Light.Gray.s900,
    d50 = FrappePalette.Dark.Gray.s50,
    d400 = FrappePalette.Dark.Gray.s400,
    d700 = FrappePalette.Dark.Gray.s700,
    d800 = FrappePalette.Dark.Gray.s800
)

internal fun familyFor(scheme: AppColorScheme): PaletteFamily = when (scheme) {
    AppColorScheme.OCEAN -> OceanFamily
    AppColorScheme.EMERALD -> EmeraldFamily
    AppColorScheme.VIOLET -> VioletFamily
    AppColorScheme.SLATE -> SlateFamily
}

// Companion accents per scheme. Each scheme picks complementary palettes for
// secondary + tertiary so the whole M3 colorScheme, not just primary, shifts
// when the user changes accent in Settings. Status chips and tonal pills key
// off secondary/tertiary containers, so without this they'd look identical
// across all schemes.
internal fun companionsFor(scheme: AppColorScheme): Pair<PaletteFamily, PaletteFamily> =
    when (scheme) {
        AppColorScheme.OCEAN -> CyanFamily to TealFamily
        AppColorScheme.EMERALD -> TealFamily to AmberFamily
        AppColorScheme.VIOLET -> CyanFamily to OceanFamily
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
    l100 = FrappePalette.Light.Cyan.s100,
    l200 = FrappePalette.Light.Cyan.s200,
    l600 = FrappePalette.Light.Cyan.s600,
    l900 = FrappePalette.Light.Cyan.s800,
    d50 = FrappePalette.Light.Cyan.s50,
    d400 = FrappePalette.Dark.Blue.s400,
    d700 = FrappePalette.Dark.Blue.s700,
    d800 = FrappePalette.Dark.Blue.s800
)

private val TealFamily = PaletteFamily(
    l100 = FrappePalette.Light.Teal.s100,
    l200 = FrappePalette.Light.Teal.s200,
    l600 = FrappePalette.Light.Teal.s600,
    l900 = FrappePalette.Light.Teal.s800,
    d50 = FrappePalette.Light.Teal.s50,
    d400 = FrappePalette.Dark.Green.s400,
    d700 = FrappePalette.Dark.Green.s700,
    d800 = FrappePalette.Dark.Green.s800
)

private val AmberFamily = PaletteFamily(
    l100 = FrappePalette.Light.Amber.s100,
    l200 = FrappePalette.Light.Amber.s200,
    l600 = FrappePalette.Light.Amber.s600,
    l900 = FrappePalette.Light.Amber.s800,
    d50 = FrappePalette.Dark.Amber.s50,
    d400 = FrappePalette.Dark.Amber.s400,
    d700 = FrappePalette.Dark.Amber.s700,
    d800 = FrappePalette.Dark.Amber.s800
)
