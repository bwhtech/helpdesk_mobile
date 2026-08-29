package com.example.helpdeskanalytics.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// M3 Expressive motion tokens. Springs for spatial movement (size, position),
// eased tweens for non-spatial properties (opacity, color). Keep call sites
// using these instead of hand-rolled animationSpecs so motion feels coherent.
object FrappeMotion {

    // --- Spatial springs (size, position, offset) -------------------------

    val emphasizedSpatial: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    fun <T> spatialSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f,
        visibilityThreshold = null
    )

    val spatialDp: SpringSpec<Dp> = spring(dampingRatio = 0.8f, stiffness = 380f)
    val spatialOffset: SpringSpec<IntOffset> = spring(dampingRatio = 0.85f, stiffness = 420f)
    val spatialSize: SpringSpec<IntSize> = spring(dampingRatio = 0.85f, stiffness = 420f)

    val standardSpatial: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 420f
    )

    // --- Eased tweens (opacity, color, scale on tap) ----------------------

    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val standardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun fadeIn() = tween<Float>(durationMillis = 300, easing = emphasizedDecelerate)
    fun fadeOut() = tween<Float>(durationMillis = 200, easing = emphasizedAccelerate)

    // Tap feedback: quick settle, no overshoot.
    val pressScale: SpringSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 1600f
    )
}
