package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import kotlinx.datetime.TimeZone

/**
 * Serves the site of the profile that was in use when it was created, and refuses
 * once another is. A fetch that outlives a switch of site would otherwise fill the
 * cache of the site left behind with the tickets of the one now in use.
 */
class ProfileApiServiceProvider(
    private val apiServiceProvider: ApiServiceProvider,
    private val profileStore: ProfileStore
) : ApiServiceProvider {

    private val profileId = profileStore.activeProfileId()

    override fun getService(): FrappeApiService {
        check(profileStore.activeProfileId() == profileId) { "The site is no longer in use" }
        return apiServiceProvider.getService()
    }

    override fun invalidate() = apiServiceProvider.invalidate()

    override suspend fun siteTimeZone(): TimeZone {
        check(profileStore.activeProfileId() == profileId) { "The site is no longer in use" }
        return apiServiceProvider.siteTimeZone()
    }

    override fun siteBaseUrl(): String? = apiServiceProvider.siteBaseUrl()
}
