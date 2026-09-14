package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.ui.screens.analytics.AnalyticsViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.LoginViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.dashboard.DashboardViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard.LeaderboardViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportBuilderViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportTemplatesViewModel
import io.github.kaulith.helpdeskanalytics.ui.agent.AgentSwitcherViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.settings.SettingsViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.tickets.TicketDetailViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.tickets.TicketListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::TicketListViewModel)
    viewModelOf(::TicketDetailViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AgentSwitcherViewModel)
    viewModelOf(::ReportTemplatesViewModel)
    viewModelOf(::ReportBuilderViewModel)
}
