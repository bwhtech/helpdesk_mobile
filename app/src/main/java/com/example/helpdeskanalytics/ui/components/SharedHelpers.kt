package com.example.helpdeskanalytics.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.helpdeskanalytics.ui.theme.FrappeMotion
import com.example.helpdeskanalytics.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Deterministic initials avatar painted with the active color scheme's
 * container palette. Three-way hash bucket so adjacent rows look distinct.
 */
@Composable
fun InitialsAvatar(name: String, size: Dp = 36.dp) {
    val cs = MaterialTheme.colorScheme
    val palette = listOf(
        cs.primaryContainer to cs.onPrimaryContainer,
        cs.secondaryContainer to cs.onSecondaryContainer,
        cs.tertiaryContainer to cs.onTertiaryContainer,
    )
    val hash = name.hashCode().let { if (it < 0) -it else it }
    val (bg, fg) = palette[hash % palette.size]
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsFrom(name),
            color = fg,
            style = if (size >= 44.dp) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

fun initialsFrom(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/**
 * Staggered enter animation. Fades + slides content in with M3E spring physics,
 * delayed by `index * stepMs`. Cap delay with `maxStep`.
 */
@Composable
fun EnterAnimated(
    index: Int,
    modifier: Modifier = Modifier,
    stepMs: Long = 40L,
    maxStep: Int = 8,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(stepMs * index.coerceAtMost(maxStep))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = FrappeMotion.fadeIn()) +
                slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = FrappeMotion.spatialOffset,
                ),
    ) {
        content()
    }
}

/**
 * Animated integer counter that eases from the previous value to the target over 500ms.
 * Returns a Compose State<Int>; read .value inside a Text.
 */
@Composable
fun animatedCount(target: Int): Int {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 500),
        label = "count",
    )
    return animated
}

/** Trigger [onResume] every time the host enters RESUMED, except the first time. */
@Composable
fun OnResume(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        var skipFirst = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skipFirst) skipFirst = false else onResume()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/** Centered empty / error state with optional icon and action button. */
@Composable
fun EmptyBlock(
    title: String,
    description: String,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cs.surface)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant,
                modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(Spacing.md))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        if (description.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(description, style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.base))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private val WEB_LINK_SCHEMES = setOf("http", "https", "mailto", "tel")

/**
 * Opens a link that came from ticket content. Customers author that HTML, so any
 * scheme outside the web set could aim the tap at another app or a local file.
 */
fun UriHandler.openWebLink(url: String) {
    if (url.substringBefore(':', "").lowercase() in WEB_LINK_SCHEMES) {
        runCatching { openUri(url) }
    }
}
