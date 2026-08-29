package com.example.helpdeskanalytics.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helpdeskanalytics.ui.theme.FrappeRadius
import com.example.helpdeskanalytics.ui.theme.Spacing

@Composable
fun CircularProgressCard(
    title: String,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    // Pick semantic color for the indicator based on the SLA threshold so
    // a glance at the dial communicates "good" vs "warning" without text.
    val progressColor = when {
        percentage >= 90f -> cs.primary
        percentage >= 75f -> cs.tertiary
        else -> cs.error
    }
    val trackColor = cs.surfaceContainerHigh

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (percentage / 100f).coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "%.0f%%".format(percentage),
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
