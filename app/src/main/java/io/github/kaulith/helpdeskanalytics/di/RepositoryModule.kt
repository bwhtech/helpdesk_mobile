package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.data.repository.FrappeAgentRepository
import io.github.kaulith.helpdeskanalytics.data.repository.FrappeAuthRepository
import io.github.kaulith.helpdeskanalytics.data.repository.FrappeReportDataRepository
import io.github.kaulith.helpdeskanalytics.data.repository.FrappeTeamRepository
import io.github.kaulith.helpdeskanalytics.data.repository.FrappeTicketRepository
import io.github.kaulith.helpdeskanalytics.data.repository.ReportTemplateRepositoryImpl
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.ReportDataRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.ReportRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.TeamRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<TicketRepository> { FrappeTicketRepository(get(), get(), get(), get(), get(), get()) }
    single<AuthRepository> { FrappeAuthRepository(get(), get(), get()) }
    single<AgentRepository> { FrappeAgentRepository(get(), get(), get(), get()) }
    single<TeamRepository> { FrappeTeamRepository(get(), get()) }
    single<ReportRepository> { ReportTemplateRepositoryImpl(get()) }
    single<ReportDataRepository> { FrappeReportDataRepository(get()) }
}
