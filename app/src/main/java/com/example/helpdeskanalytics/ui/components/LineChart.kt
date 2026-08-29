package com.example.helpdeskanalytics.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpdeskanalytics.ui.screens.analytics.ChartData

@Composable
fun LineChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val cs = MaterialTheme.colorScheme
    val lineColor = cs.primary
    val fillColor = cs.primary.copy(alpha = 0.10f)
    val gridColor = cs.outlineVariant
    val labelColor = cs.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(
        modifier = modifier.fillMaxWidth().height(200.dp)
    ) {
        val maxValue = data.maxOf { it.value }.coerceAtLeast(1f)
        val paddingLeft = 36.dp.toPx()
        val paddingBottom = 22.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val chartWidth = size.width - paddingLeft
        val chartHeight = size.height - paddingBottom - paddingTop
        val stepX = chartWidth / (data.size - 1)
        val ySteps = 4

        val dotted = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        for (i in 0..ySteps) {
            val y = paddingTop + chartHeight - (chartHeight * i / ySteps)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dotted
            )
            val yVal = (maxValue * i / ySteps)
            val label = yVal.toInt().toString()
            val result = textMeasurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = result,
                topLeft = Offset(
                    paddingLeft - result.size.width - 4.dp.toPx(),
                    y - result.size.height / 2f
                )
            )
        }

        // Line path
        val linePath = Path()
        data.forEachIndexed { index, d ->
            val x = paddingLeft + index * stepX
            val y = paddingTop + chartHeight * (1 - d.value / maxValue)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Soft area fill under the line, adding depth without competing with the stroke
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(paddingLeft + (data.size - 1) * stepX, paddingTop + chartHeight)
            lineTo(paddingLeft, paddingTop + chartHeight)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, fillColor.copy(alpha = 0f)),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        data.forEachIndexed { index, d ->
            val x = paddingLeft + index * stepX
            val y = paddingTop + chartHeight * (1 - d.value / maxValue)
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }

        val labelInterval = (data.size / 6).coerceAtLeast(1)
        val baseY = paddingTop + chartHeight
        data.forEachIndexed { index, d ->
            if (index % labelInterval == 0 || index == data.lastIndex) {
                val x = paddingLeft + index * stepX
                val result = textMeasurer.measure(d.label, labelStyle)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(x - result.size.width / 2f, baseY + 4.dp.toPx())
                )
            }
        }
    }
}
