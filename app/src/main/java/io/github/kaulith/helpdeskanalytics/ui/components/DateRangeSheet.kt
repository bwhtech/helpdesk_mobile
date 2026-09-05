package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate as KotlinLocalDate

private const val MONTHS_BACK = 59L

/**
 * Range picker over past dates. Tap once to set the start, again to set the end;
 * a tap before the start moves the start instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSheet(
    initialStart: KotlinLocalDate? = null,
    initialEnd: KotlinLocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (KotlinLocalDate, KotlinLocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.from(today) }
    val weekDays = remember { daysOfWeek() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var start by remember { mutableStateOf(initialStart?.toJavaLocalDate()) }
    var end by remember { mutableStateOf(initialEnd?.toJavaLocalDate()) }

    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(MONTHS_BACK),
        endMonth = currentMonth,
        firstVisibleMonth = YearMonth.from(start ?: today),
        firstDayOfWeek = weekDays.first()
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = Spacing.base)) {
            Text(
                text = rangeLabel(start, end),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val visibleMonth = calendarState.firstVisibleMonth.yearMonth
                IconButton(
                    onClick = { scope.launch { calendarState.animateScrollToMonth(visibleMonth.minusMonths(1)) } },
                    enabled = visibleMonth > calendarState.startMonth
                ) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month") }

                Text(
                    text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, locale)} " +
                            "${visibleMonth.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface
                )

                IconButton(
                    onClick = { scope.launch { calendarState.animateScrollToMonth(visibleMonth.plusMonths(1)) } },
                    enabled = visibleMonth < calendarState.endMonth
                ) { Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month") }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalCalendar(
                state = calendarState,
                userScrollEnabled = false,
                dayContent = { day ->
                    DayCell(
                        day = day,
                        today = today,
                        start = start,
                        end = end,
                        onClick = { date ->
                            val from = start
                            when {
                                from == null || end != null -> {
                                    start = date
                                    end = null
                                }
                                date < from -> start = date
                                else -> end = date
                            }
                        }
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val from = start ?: return@TextButton
                        onConfirm(from.toKotlinLocalDate(), (end ?: from).toKotlinLocalDate())
                    },
                    enabled = start != null
                ) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    today: LocalDate,
    start: LocalDate?,
    end: LocalDate?,
    onClick: (LocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val inMonth = day.position == DayPosition.MonthDate
    val isFuture = day.date > today
    val isEndpoint = day.date == start || day.date == end
    val isBetween = start != null && end != null && day.date > start && day.date < end

    // Endpoints round the outer edge so the span reads as one continuous pill.
    val spanShape = when {
        start != null && end != null && day.date == start -> RoundedCornerShape(
            topStartPercent = 50, bottomStartPercent = 50
        )
        start != null && end != null && day.date == end -> RoundedCornerShape(
            topEndPercent = 50, bottomEndPercent = 50
        )
        else -> RoundedCornerShape(0)
    }
    val spanColor = if (isBetween || (isEndpoint && end != null)) cs.secondaryContainer else Color.Transparent

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(spanColor, spanShape),
        contentAlignment = Alignment.Center
    ) {
        if (!inMonth) return@Box

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isEndpoint) cs.primary else Color.Transparent)
                .clickable(enabled = !isFuture) { onClick(day.date) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isEndpoint || day.date == today) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isEndpoint -> cs.onPrimary
                    isFuture -> cs.onSurfaceVariant.copy(alpha = 0.38f)
                    day.date == today -> cs.primary
                    else -> cs.onSurface
                }
            )
        }
    }
}

private fun rangeLabel(start: LocalDate?, end: LocalDate?): String = when {
    start == null -> "Select range"
    end == null -> start.short()
    else -> "${start.short()} to ${end.short()}"
}

private fun LocalDate.short(): String =
    "$dayOfMonth ${month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
