package io.github.kaulith.helpdeskanalytics.di

import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules

private val profileModules = listOf(databaseModule, repositoryModule)

/**
 * Starts over everything that holds the database or cached data of a profile, so
 * nothing of the profile left behind reaches the one now in use.
 */
fun reloadProfileModules() {
    unloadKoinModules(profileModules)
    loadKoinModules(profileModules)
}
