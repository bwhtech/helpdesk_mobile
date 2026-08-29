package com.example.helpdeskanalytics.service

import com.example.helpdeskanalytics.notifications.DeviceTokenManager
import com.example.helpdeskanalytics.notifications.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class HelpdeskFirebaseMessagingService : FirebaseMessagingService() {

    private val deviceTokenManager: DeviceTokenManager by inject()
    private val notificationHelper: NotificationHelper by inject()

    // Through WorkManager, because this process is often started only to deliver
    // one message and is torn down before an in-service coroutine can finish.
    override fun onNewToken(token: String) {
        deviceTokenManager.registerNow()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: return
        val body = message.notification?.body ?: data["body"] ?: data["message"] ?: ""

        notificationHelper.showNotification(
            title = title,
            body = body,
            channelId = channelFor(data["type"]),
            ticketId = data["ticketId"]
        )
    }

    private fun channelFor(type: String?): String = when (type) {
        "customer_reply", "new_comment" -> NotificationHelper.CHANNEL_TICKET_REPLIES
        "sla_warning" -> NotificationHelper.CHANNEL_SLA_WARNINGS
        else -> NotificationHelper.CHANNEL_ID
    }
}
