package com.example.helpdeskanalytics.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.helpdeskanalytics.MainActivity
import com.example.helpdeskanalytics.R

class NotificationHelper(private val context: Context) {

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)

            val defaultChannel = NotificationChannel(
                CHANNEL_ID,
                "Ticket Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for helpdesk ticket activity"
            }

            val repliesChannel = NotificationChannel(
                CHANNEL_TICKET_REPLIES,
                "Ticket Replies",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new replies on your tickets"
            }

            val slaChannel = NotificationChannel(
                CHANNEL_SLA_WARNINGS,
                "SLA Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications for upcoming SLA breaches"
            }

            manager.createNotificationChannels(
                listOf(defaultChannel, repliesChannel, slaChannel)
            )
        }
    }

    fun showNotification(
        title: String,
        body: String,
        channelId: String = CHANNEL_ID,
        ticketId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            if (ticketId != null) data = Uri.parse("helpdesk://ticket/${Uri.encode(ticketId)}")
        }

        // One request code per ticket, otherwise FLAG_UPDATE_CURRENT hands every
        // notification the extras of whichever one was built first.
        val requestCode = ticketId?.hashCode() ?: 0
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = ticketId?.hashCode() ?: System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "helpdesk_tickets_v2"
        private const val LEGACY_CHANNEL_ID = "helpdesk_tickets"
        const val CHANNEL_TICKET_REPLIES = "ticket_replies"
        const val CHANNEL_SLA_WARNINGS = "sla_warnings"
    }
}
