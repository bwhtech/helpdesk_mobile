package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.data.remote.api.ProfileApiServiceProvider
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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ProfileApiServiceProvider)
    // What caches a site reads it through the provider tied to the profile in use.
    single<TicketRepository> {
        FrappeTicketRepository(get<ProfileApiServiceProvider>(), get(), get(), get(), get(), get())
    }
    singleOf(::FrappeAuthRepository) { bind<AuthRepository>() }
    single<AgentRepository> {
        FrappeAgentRepository(get<ProfileApiServiceProvider>(), get(), get(), get(), get())
    }
    single<TeamRepository> { FrappeTeamRepository(get<ProfileApiServiceProvider>(), get()) }
    singleOf(::ReportTemplateRepositoryImpl) { bind<ReportRepository>() }
    single<ReportDataRepository> { FrappeReportDataRepository(get<ProfileApiServiceProvider>()) }
}
