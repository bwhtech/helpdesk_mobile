package com.example.helpdeskanalytics.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Material3 color schemes are built parametrically per AppColorScheme so a single
// theme switch in Settings recolors the whole app. The shape of each scheme is
// identical; only the primary palette family changes. Surfaces stay
// scheme-neutral (Frappe grays) so accent colors read cleanly against them.

internal data class PaletteFamily(
    val l50: Color, val l100: Color, val l200: Color, val l400: Color,
    val l500: Color, val l600: Color, val l700: Color, val l800: Color, val l900: Color,
    val d50: Color, val d300: Color, val d400: Color, val d500: Color,
    val d700: Color, val d800: Color, val d900: Color
)

internal fun buildLightScheme(
    family: PaletteFamily,
    secondaryFamily: PaletteFamily = family,
    tertiaryFamily: PaletteFamily = family
): ColorScheme {
    // Surface tiers tinted by the active accent at increasing alpha. Light scheme
    // surfaces lose the flat-grey feel and pick up a subtle brand undertone, the
    // way M3 baseline does for purple, except scheme-aware.
    val accent = family.l600
    val white = FrappePalette.White
    val tier1 = lerp(white, accent, 0.025f)
    val tier2 = lerp(white, accent, 0.05f)
    val tier3 = lerp(white, accent, 0.08f)
    val tier4 = lerp(white, accent, 0.12f)

    return lightColorScheme(
    primary = family.l600,
    onPrimary = FrappePalette.White,
    primaryContainer = family.l100,
    onPrimaryContainer = family.l900,

    secondary = secondaryFamily.l600,
    onSecondary = FrappePalette.White,
    secondaryContainer = secondaryFamily.l100,
    onSecondaryContainer = secondaryFamily.l900,

    tertiary = tertiaryFamily.l600,
    onTertiary = FrappePalette.White,
    tertiaryContainer = tertiaryFamily.l100,
    onTertiaryContainer = tertiaryFamily.l900,

    error = FrappePalette.Light.Red.s600,
    onError = FrappePalette.White,
    errorContainer = FrappePalette.Light.Red.s100,
    onErrorContainer = FrappePalette.Light.Red.s900,

    background = FrappePalette.White,
    onBackground = FrappePalette.Light.Gray.s900,

    surface = FrappePalette.White,
    onSurface = FrappePalette.Light.Gray.s900,
    surfaceVariant = FrappePalette.Light.Gray.s100,
    onSurfaceVariant = FrappePalette.Light.Gray.s700,

    surfaceContainerLowest = white,
    surfaceContainerLow = tier1,
    surfaceContainer = tier2,
    surfaceContainerHigh = tier3,
    surfaceContainerHighest = tier4,
    surfaceBright = white,
    surfaceDim = tier3,
    surfaceTint = accent,
    inverseSurface = FrappePalette.Light.Gray.s800,
    inverseOnSurface = FrappePalette.Light.Gray.s50,
    inversePrimary = family.l200,

    outline = FrappePalette.Light.Gray.s400,
    outlineVariant = FrappePalette.Light.Gray.s200
)
}

internal fun buildDarkScheme(
    family: PaletteFamily,
    secondaryFamily: PaletteFamily = family,
    tertiaryFamily: PaletteFamily = family
): ColorScheme = darkColorScheme(
    primary = family.d400,
    onPrimary = FrappePalette.Dark.Gray.s900,
    primaryContainer = family.d800,
    onPrimaryContainer = family.d50,

    secondary = secondaryFamily.d400,
    onSecondary = FrappePalette.Dark.Gray.s900,
    secondaryContainer = secondaryFamily.d800,
    onSecondaryContainer = secondaryFamily.d50,

    tertiary = tertiaryFamily.d400,
    onTertiary = FrappePalette.Dark.Gray.s900,
    tertiaryContainer = tertiaryFamily.d800,
    onTertiaryContainer = tertiaryFamily.d50,

    error = FrappePalette.Dark.Red.s400,
    onError = FrappePalette.White,
    errorContainer = FrappePalette.Dark.Red.s800,
    onErrorContainer = FrappePalette.Dark.Red.s50,

    background = FrappePalette.Dark.Gray.s900,
    onBackground = FrappePalette.Dark.Gray.s50,

    surface = FrappePalette.Dark.Gray.s800,
    onSurface = FrappePalette.Dark.Gray.s50,
    surfaceVariant = FrappePalette.Dark.Gray.s700,
    onSurfaceVariant = FrappePalette.Dark.Gray.s200,

    surfaceContainerLowest = FrappePalette.Dark.Gray.s900,
    surfaceContainerLow = FrappePalette.Dark.Gray.s800,
    surfaceContainer = FrappePalette.Dark.Gray.s800,
    surfaceContainerHigh = FrappePalette.Dark.Gray.s700,
    surfaceContainerHighest = FrappePalette.Dark.Gray.s700,
    surfaceBright = FrappePalette.Dark.Gray.s700,
    surfaceDim = FrappePalette.Dark.Gray.s900,
    surfaceTint = family.d400,
    inverseSurface = FrappePalette.Dark.Gray.s50,
    inverseOnSurface = FrappePalette.Dark.Gray.s800,
    inversePrimary = family.d700,

    outline = FrappePalette.Dark.Gray.s500,
    outlineVariant = FrappePalette.Dark.Gray.s700
)

// Backwards-compat: old call sites (Theme.kt fallback) reference these directly.
val LightColorScheme: ColorScheme = buildLightScheme(OceanFamily)
val DarkColorScheme: ColorScheme = buildDarkScheme(OceanFamily)
