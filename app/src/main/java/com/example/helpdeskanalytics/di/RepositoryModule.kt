package com.example.helpdeskanalytics.di

import com.example.helpdeskanalytics.data.repository.FrappeAgentRepository
import com.example.helpdeskanalytics.data.repository.FrappeAuthRepository
import com.example.helpdeskanalytics.data.repository.FrappeReportDataRepository
import com.example.helpdeskanalytics.data.repository.FrappeTeamRepository
import com.example.helpdeskanalytics.data.repository.FrappeTicketRepository
import com.example.helpdeskanalytics.data.repository.ReportTemplateRepositoryImpl
import com.example.helpdeskanalytics.domain.repository.AgentRepository
import com.example.helpdeskanalytics.domain.repository.AuthRepository
import com.example.helpdeskanalytics.domain.repository.ReportDataRepository
import com.example.helpdeskanalytics.domain.repository.ReportRepository
import com.example.helpdeskanalytics.domain.repository.TeamRepository
import com.example.helpdeskanalytics.domain.repository.TicketRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<TicketRepository> { FrappeTicketRepository(get(), get(), get(), get(), get(), get()) }
    single<AuthRepository> { FrappeAuthRepository(get(), get(), get()) }
    single<AgentRepository> { FrappeAgentRepository(get(), get(), get(), get()) }
    single<TeamRepository> { FrappeTeamRepository(get(), get()) }
    single<ReportRepository> { ReportTemplateRepositoryImpl(get()) }
    single<ReportDataRepository> { FrappeReportDataRepository(get()) }
}
