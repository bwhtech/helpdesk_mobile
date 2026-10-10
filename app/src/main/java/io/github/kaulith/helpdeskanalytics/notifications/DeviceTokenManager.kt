package io.github.kaulith.helpdeskanalytics.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.DeviceRegistration
import io.github.kaulith.helpdeskanalytics.data.remote.api.NotificationApiClient
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RegisterDeviceRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UnregisterDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class DeviceTokenManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val notificationApiClient: NotificationApiClient
) : DeviceRegistration {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            var previousEmail: String? = null

            combine(
                preferencesManager.loggedInUserEmail,
                preferencesManager.activeAgentEmail
            ) { loggedIn, activeAgent ->
                activeAgent ?: loggedIn
            }
                .distinctUntilChanged()
                .collect { effectiveEmail ->
                    val token = getFcmToken() ?: return@collect
                    // Signing out unregisters by itself, before the session is gone.
                    previousEmail?.let { old ->
                        if (old != effectiveEmail && effectiveEmail != null) {
                            unregisterDevice(token, old)
                        }
                    }
                    if (effectiveEmail != null) {
                        registerDevice(token, effectiveEmail)
                    } else {
                        workManager().cancelUniqueWork(TicketPollWorker.PERIODIC_WORK_NAME)
                    }
                    previousEmail = effectiveEmail
                }
        }

        scheduleRegistrationRefresh()
    }

    // WorkManager, not an in-process loop: the old heartbeat died with the process
    // every time the app was swiped out of recents, so the device fell out of the
    // push backend's registry and stopped receiving anything.
    fun scheduleRegistrationRefresh() {
        val request = PeriodicWorkRequestBuilder<DeviceRegistrationWorker>(
            REFRESH_INTERVAL_HOURS, TimeUnit.HOURS
        ).setConstraints(networkConstraints()).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DeviceRegistrationWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun registerNow() {
        val request = OneTimeWorkRequestBuilder<DeviceRegistrationWorker>()
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DeviceRegistrationWorker.ONE_SHOT_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun registerCurrentDevice(): Boolean {
        val email = activeAgentEmail() ?: loggedInEmail() ?: return true
        val token = getFcmToken() ?: return false
        return registerDevice(token, email)
    }

    // Bounded, because signing out must not hang on FCM or on a site slow to answer.
    override suspend fun unregisterCurrentDevice() {
        withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
            val email = activeAgentEmail() ?: loggedInEmail() ?: return@withTimeoutOrNull
            val token = getFcmToken() ?: return@withTimeoutOrNull
            unregisterDevice(token, email)
        }
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private suspend fun activeAgentEmail(): String? = preferencesManager.activeAgentEmail.first()

    private suspend fun loggedInEmail(): String? = preferencesManager.loggedInUserEmail.first()

    private suspend fun getFcmToken(): String? {
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> cont.resume(token) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get FCM token", e)
                    cont.resume(null)
                }
        }
    }

    private suspend fun registerDevice(token: String, agentEmail: String): Boolean {
        return try {
            notificationApiClient.service.registerDevice(RegisterDeviceRequest(token, agentEmail))
            Log.d(TAG, "Device registered ($agentEmail)")
            workManager().cancelUniqueWork(TicketPollWorker.PERIODIC_WORK_NAME)
            true
        } catch (e: HttpException) {
            val pushAppMissing = e.response()?.errorBody()?.string().orEmpty().contains(APP_NOT_INSTALLED)
            if (pushAppMissing) {
                scheduleTicketPolling()
            } else {
                Log.e(TAG, "Failed to register device ($agentEmail)", e)
            }
            pushAppMissing
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device ($agentEmail)", e)
            false
        }
    }

    // A site without helpdesk_push has nothing to push from, so the phone checks
    // for ticket changes itself. KEEP, because every registration attempt lands here.
    private fun scheduleTicketPolling() {
        val request = PeriodicWorkRequestBuilder<TicketPollWorker>(
            POLL_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).setConstraints(networkConstraints()).build()

        workManager().enqueueUniquePeriodicWork(
            TicketPollWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun workManager() = WorkManager.getInstance(context)

    private suspend fun unregisterDevice(token: String, agentEmail: String) {
        try {
            notificationApiClient.service.unregisterDevice(UnregisterDeviceRequest(token))
            Log.d(TAG, "Device unregistered ($agentEmail)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister device ($agentEmail)", e)
        }
    }

    companion object {
        private const val TAG = "DeviceTokenManager"
        private const val REFRESH_INTERVAL_HOURS = 6L
        private const val UNREGISTER_TIMEOUT_MS = 3_000L
        private const val POLL_INTERVAL_MINUTES = 15L

        // exc_type Frappe answers with for a call into an app the site does not have.
        private const val APP_NOT_INSTALLED = "AppNotInstalledError"
    }
}
