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
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { TicketListViewModel(get(), get()) }
    viewModel { TicketDetailViewModel(get(), get()) }
    viewModel { AnalyticsViewModel(get(), get()) }
    viewModel { LeaderboardViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { AgentSwitcherViewModel(get()) }
    viewModel { ReportTemplatesViewModel(get()) }
    viewModel { ReportBuilderViewModel(get(), get(), get()) }
}
