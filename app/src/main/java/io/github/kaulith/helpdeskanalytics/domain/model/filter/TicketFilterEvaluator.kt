package io.github.kaulith.helpdeskanalytics.domain.model.filter

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private val zone = TimeZone.currentSystemDefault()

/** Sensible starting value when a field or operator changes, so a fresh row is usable. */
fun <T> defaultValueFor(field: FilterableField<T>, operator: FilterOperator): String = when {
    operator == FilterOperator.IS -> IS_SET
    field.type == FilterFieldType.SELECT -> field.options.firstOrNull().orEmpty()
    else -> ""
}

fun <T> List<FilterCondition<T>>.matches(item: T): Boolean = all { it.matches(item) }

fun <T> FilterCondition<T>.matches(item: T): Boolean = when (field.type) {
    FilterFieldType.DATE -> matchesDate(field.instant(item), operator, value)
    FilterFieldType.NUMBER -> matchesNumber(field.number(item), operator, value)
    else -> {
        val values = field.list(item).ifEmpty { listOfNotNull(field.text(item)) }
        matchesText(values, operator, value)
    }
}

private fun matchesText(actual: List<String>, op: FilterOperator, value: String): Boolean = when (op) {
    FilterOperator.EQUALS -> actual.any { it.equals(value, ignoreCase = true) }
    FilterOperator.NOT_EQUALS -> actual.none { it.equals(value, ignoreCase = true) }
    FilterOperator.LIKE -> actual.any { it.contains(value, ignoreCase = true) }
    FilterOperator.NOT_LIKE -> actual.none { it.contains(value, ignoreCase = true) }
    FilterOperator.IN -> value.toCommaList().any { v -> actual.any { it.equals(v, ignoreCase = true) } }
    FilterOperator.NOT_IN -> value.toCommaList().none { v -> actual.any { it.equals(v, ignoreCase = true) } }
    FilterOperator.IS -> if (value == IS_NOT_SET) actual.all { it.isBlank() } else actual.any { it.isNotBlank() }
    else -> true
}

private fun matchesNumber(actual: Double?, op: FilterOperator, value: String): Boolean {
    if (op == FilterOperator.IS) return if (value == IS_NOT_SET) actual == null else actual != null
    if (actual == null) return false
    return when (op) {
        FilterOperator.EQUALS -> value.toDoubleOrNull()?.let { actual == it } ?: true
        FilterOperator.NOT_EQUALS -> value.toDoubleOrNull()?.let { actual != it } ?: true
        FilterOperator.LT -> value.toDoubleOrNull()?.let { actual < it } ?: true
        FilterOperator.GT -> value.toDoubleOrNull()?.let { actual > it } ?: true
        FilterOperator.LTE -> value.toDoubleOrNull()?.let { actual <= it } ?: true
        FilterOperator.GTE -> value.toDoubleOrNull()?.let { actual >= it } ?: true
        FilterOperator.IN -> value.toCommaList().mapNotNull { it.toDoubleOrNull() }.any { it == actual }
        FilterOperator.NOT_IN -> value.toCommaList().mapNotNull { it.toDoubleOrNull() }.none { it == actual }
        else -> true
    }
}

private fun matchesDate(actual: Instant?, op: FilterOperator, value: String): Boolean {
    if (op == FilterOperator.IS) {
        return if (value == IS_NOT_SET) actual == null else actual != null
    }
    if (actual == null) return false
    val date = actual.toLocalDateTime(zone).date
    return when (op) {
        FilterOperator.TIMESPAN -> {
            val span = runCatching { Timespan.valueOf(value) }.getOrNull() ?: return true
            val (start, endExclusive) = span.range()
            actual >= start && actual < endExclusive
        }
        FilterOperator.BETWEEN -> {
            val (from, to) = value.toDatePair() ?: return true
            date >= from && date <= to
        }
        else -> {
            val target = value.toDate() ?: return true
            when (op) {
                FilterOperator.EQUALS -> date == target
                FilterOperator.LT -> date < target
                FilterOperator.GT -> date > target
                FilterOperator.LTE -> date <= target
                FilterOperator.GTE -> date >= target
                else -> true
            }
        }
    }
}

private fun String.toCommaList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }

private fun String.toDate(): LocalDate? =
    toLongOrNull()?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date }

private fun String.toDatePair(): Pair<LocalDate, LocalDate>? {
    val parts = split(",")
    if (parts.size != 2) return null
    val from = parts[0].toDate() ?: return null
    val to = parts[1].toDate() ?: return null
    return from to to
}

private fun Timespan.range(): Pair<Instant, Instant> {
    val today = Clock.System.now().toLocalDateTime(zone).date
    val (startDate, endInclusive) = when (this) {
        Timespan.TODAY -> today to today
        Timespan.YESTERDAY -> today.minus(DatePeriod(days = 1)) to today.minus(DatePeriod(days = 1))
        Timespan.LAST_7_DAYS -> today.minus(DatePeriod(days = 6)) to today
        Timespan.LAST_30_DAYS -> today.minus(DatePeriod(days = 29)) to today
        Timespan.THIS_MONTH -> {
            val first = LocalDate(today.year, today.month, 1)
            first to first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        }
        Timespan.LAST_MONTH -> {
            val firstThis = LocalDate(today.year, today.month, 1)
            val firstLast = firstThis.minus(DatePeriod(months = 1))
            firstLast to firstThis.minus(DatePeriod(days = 1))
        }
        Timespan.THIS_YEAR -> LocalDate(today.year, 1, 1) to today
    }
    val start = startDate.atStartOfDayIn(zone)
    val endExclusive = endInclusive.plus(DatePeriod(days = 1)).atStartOfDayIn(zone)
    return start to endExclusive
}
