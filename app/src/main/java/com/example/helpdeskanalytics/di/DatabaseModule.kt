package com.example.helpdeskanalytics.di

import androidx.room.Room
import com.example.helpdeskanalytics.data.local.database.AppDatabase
import com.example.helpdeskanalytics.util.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().ticketDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().commentDao() }
    single { get<AppDatabase>().agentDao() }
    single { get<AppDatabase>().teamDao() }
    single { get<AppDatabase>().reportTemplateDao() }
}
