package com.example.helpdeskanalytics.di

import com.example.helpdeskanalytics.ui.screens.analytics.AnalyticsViewModel
import com.example.helpdeskanalytics.ui.screens.auth.LoginViewModel
import com.example.helpdeskanalytics.ui.screens.dashboard.DashboardViewModel
import com.example.helpdeskanalytics.ui.screens.leaderboard.LeaderboardViewModel
import com.example.helpdeskanalytics.ui.screens.reports.ReportBuilderViewModel
import com.example.helpdeskanalytics.ui.screens.reports.ReportTemplatesViewModel
import com.example.helpdeskanalytics.ui.screens.settings.SettingsViewModel
import com.example.helpdeskanalytics.ui.screens.tickets.TicketDetailViewModel
import com.example.helpdeskanalytics.ui.screens.tickets.TicketListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { TicketListViewModel(get(), get()) }
    viewModel { TicketDetailViewModel(get(), get()) }
    viewModel { AnalyticsViewModel(get(), get()) }
    viewModel { LeaderboardViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { ReportTemplatesViewModel(get()) }
    viewModel { ReportBuilderViewModel(get(), get(), get()) }
}
