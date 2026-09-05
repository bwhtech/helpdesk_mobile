package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Opens the authorize page in a Custom Tab, so the sign-in runs in the user's own
 * browser session and the app never sees their password.
 */
fun launchAuthorization(context: Context, authorizationUrl: String): Boolean =
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(authorizationUrl))
        true
    } catch (e: ActivityNotFoundException) {
        Log.d("OAuthLauncher", "No browser available for the authorize page", e)
        false
    }
