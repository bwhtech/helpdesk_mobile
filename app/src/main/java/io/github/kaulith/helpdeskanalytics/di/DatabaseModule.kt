package io.github.kaulith.helpdeskanalytics.di

import androidx.room.Room
import io.github.kaulith.helpdeskanalytics.data.local.database.AppDatabase
import io.github.kaulith.helpdeskanalytics.util.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Versions before 6 shipped without exported schemas, so nothing can migrate them.
private val PRE_SCHEMA_EXPORT_VERSIONS = intArrayOf(1, 2, 3, 4, 5)

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigrationFrom(*PRE_SCHEMA_EXPORT_VERSIONS)
            .build()
    }
    single { get<AppDatabase>().ticketDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().commentDao() }
    single { get<AppDatabase>().agentDao() }
    single { get<AppDatabase>().teamDao() }
    single { get<AppDatabase>().reportTemplateDao() }
}
