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
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    single { FrappeTicketRepository(get(), get(), get(), get(), get(), get()) } bind TicketRepository::class
    single { FrappeAuthRepository(get(), get(), get(), get()) } bind AuthRepository::class
    single { FrappeAgentRepository(get(), get(), get(), get()) } bind AgentRepository::class
    single { FrappeTeamRepository(get(), get()) } bind TeamRepository::class
    single { ReportTemplateRepositoryImpl(get()) } bind ReportRepository::class
    single { FrappeReportDataRepository(get()) } bind ReportDataRepository::class
}
