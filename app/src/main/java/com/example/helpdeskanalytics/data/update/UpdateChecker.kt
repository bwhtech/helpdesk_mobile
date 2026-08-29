package com.example.helpdeskanalytics.data.update

import com.example.helpdeskanalytics.util.Constants
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class AppUpdate(val versionName: String, val releaseUrl: String)

private data class GithubReleaseDto(
    @SerializedName("tag_name") val tagName: String?,
    @SerializedName("html_url") val htmlUrl: String?,
    @SerializedName("draft") val draft: Boolean = false,
    @SerializedName("prerelease") val prerelease: Boolean = false
)

private interface GithubReleaseService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubReleaseDto
}

/**
 * Reads the newest published release off GitHub so the app can point at it. The
 * APK is installed by the user from the release page; the app never fetches or
 * installs it, which keeps REQUEST_INSTALL_PACKAGES off the manifest.
 */
class UpdateChecker(private val installedVersionName: String) {

    private val service: GithubReleaseService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(Constants.GITHUB_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubReleaseService::class.java)
    }

    suspend fun availableUpdate(): AppUpdate? {
        val release = runCatching {
            service.latestRelease(Constants.RELEASES_OWNER, Constants.RELEASES_REPO)
        }.getOrNull() ?: return null

        if (release.draft || release.prerelease) return null

        val version = release.tagName?.trim()?.removePrefix("v").orEmpty()
        val releaseUrl = release.htmlUrl.orEmpty()
        if (version.isEmpty() || releaseUrl.isEmpty()) return null

        return AppUpdate(version, releaseUrl).takeIf { isNewerVersion(version, installedVersionName) }
    }
}

internal fun isNewerVersion(candidate: String, installed: String): Boolean {
    val left = versionParts(candidate)
    val right = versionParts(installed)
    repeat(maxOf(left.size, right.size)) { index ->
        val a = left.getOrElse(index) { 0 }
        val b = right.getOrElse(index) { 0 }
        if (a != b) return a > b
    }
    return false
}

// Build suffixes (-debug, -internal) are not part of the ordering.
private fun versionParts(version: String): List<Int> =
    version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
