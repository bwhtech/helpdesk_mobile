package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries the OAuth redirect from the activity that receives it to the login screen.
 * The browser hands the code back through a new intent, not a screen result.
 */
class OAuthRedirectHolder {

    private val _redirect = MutableStateFlow<Uri?>(null)
    val redirect: StateFlow<Uri?> = _redirect.asStateFlow()

    fun submit(uri: Uri) {
        _redirect.value = uri
    }

    fun clear() {
        _redirect.value = null
    }
}
