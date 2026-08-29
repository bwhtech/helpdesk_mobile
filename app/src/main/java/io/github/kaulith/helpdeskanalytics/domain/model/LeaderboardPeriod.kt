package io.github.kaulith.helpdeskanalytics.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/** The window a leaderboard ranking covers, by when a ticket was resolved. */
sealed interface LeaderboardPeriod {

    val label: String

    data object AllTime : LeaderboardPeriod {
        override val label = "All time"
    }

    data object Today : LeaderboardPeriod {
        override val label = "Today"
    }

    data object LastTwoDays : LeaderboardPeriod {
        override val label = "Last 2 days"
    }

    data object Week : LeaderboardPeriod {
        override val label = "Week"
    }

    data object Month : LeaderboardPeriod {
        override val label = "Month"
    }

    data class Custom(val start: LocalDate, val end: LocalDate) : LeaderboardPeriod {
        override val label get() = "$start to $end"
    }

    /** Inclusive local-date bounds, or null to cover every ticket ever resolved. */
    fun bounds(today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())):
            ClosedRange<LocalDate>? = when (this) {
        AllTime -> null
        Today -> today..today
        LastTwoDays -> today.minus(1, DateTimeUnit.DAY)..today
        Week -> today.minus(7, DateTimeUnit.DAY)..today
        Month -> today.minus(1, DateTimeUnit.MONTH)..today
        is Custom -> start..end
    }

    companion object {
        val presets = listOf(Today, LastTwoDays, Week, Month, AllTime)
    }
}
