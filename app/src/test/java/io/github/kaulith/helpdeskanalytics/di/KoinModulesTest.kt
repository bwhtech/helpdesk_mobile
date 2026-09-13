package io.github.kaulith.helpdeskanalytics.di

import org.junit.Test
import org.koin.android.test.verify.androidVerify
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module

@OptIn(KoinExperimentalAPI::class)
class KoinModulesTest {

    @Test
    fun `every definition finds its constructor dependencies`() {
        module {
            includes(appModule, networkModule, databaseModule, repositoryModule, viewModelModule)
        }.androidVerify()
    }
}
