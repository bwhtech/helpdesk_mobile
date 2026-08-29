package com.example.helpdeskanalytics.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.helpdeskanalytics.data.local.preferences.PreferencesManager
import com.example.helpdeskanalytics.data.remote.api.NotificationApiClient
import com.example.helpdeskanalytics.data.remote.dto.RegisterDeviceRequest
import com.example.helpdeskanalytics.data.remote.dto.UnregisterDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DeviceTokenManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val notificationApiClient: NotificationApiClient
) {
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
                    previousEmail?.let { old ->
                        if (old != effectiveEmail) {
                            unregisterDevice(token, old)
                        }
                    }
                    if (effectiveEmail != null) {
                        registerDevice(token, effectiveEmail)
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

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private suspend fun activeAgentEmail(): String? = preferencesManager.activeAgentEmail.first()

    private suspend fun loggedInEmail(): String? = preferencesManager.loggedInUserEmail.first()

    private suspend fun getFcmToken(): String? {
        return suspendCoroutine { cont ->
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device ($agentEmail)", e)
            false
        }
    }

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
    }
}
