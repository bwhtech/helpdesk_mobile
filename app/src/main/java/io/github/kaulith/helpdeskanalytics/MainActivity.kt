package io.github.kaulith.helpdeskanalytics

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import io.github.kaulith.helpdeskanalytics.ui.navigation.AppNavGraph
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.OAuthRedirectHolder
import io.github.kaulith.helpdeskanalytics.ui.theme.HelpDeskAnalyticsTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesManager: PreferencesManager by inject()
    private val oAuthRedirectHolder: OAuthRedirectHolder by inject()

    // FCM paints the tray itself for messages carrying a notification block, so a tap
    // arrives as intent extras rather than the helpdesk://ticket/{id} deep link.
    private val pendingTicketId = mutableStateOf<String?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result: the system handles showing and hiding notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        pendingTicketId.value = ticketIdFrom(intent)
        forwardOAuthRedirect(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "system")
            val dynamicColor by preferencesManager.dynamicColor.collectAsState(initial = false)
            val colorScheme by preferencesManager.colorScheme.collectAsState(initial = "ocean")

            HelpDeskAnalyticsTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                colorSchemeKey = colorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        pendingTicketId = pendingTicketId.value,
                        onPendingTicketHandled = { pendingTicketId.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTicketId.value = ticketIdFrom(intent)
        forwardOAuthRedirect(intent)
    }

    // The browser hands the authorization code back as a deep link, not a result.
    private fun forwardOAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.host == OAuthClient.REDIRECT_HOST) oAuthRedirectHolder.submit(uri)
    }

    private fun ticketIdFrom(intent: Intent?): String? =
        intent?.getStringExtra(TICKET_ID_EXTRA)?.takeIf { it.isNotBlank() }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val TICKET_ID_EXTRA = "ticketId"
    }
}
