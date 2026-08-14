package com.nexflix.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL

class NexflixMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: "post"
        val title = message.data["title"] ?: message.notification?.title ?: "New post"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val imageUrl = message.data["image"] ?: message.notification?.imageUrl?.toString()
        val postUrl = message.data["url"] ?: ""

        showNotification(title, body, imageUrl, postUrl, type)
    }

    private fun showNotification(title: String, body: String, imageUrl: String?, postUrl: String, type: String) {
        // Update notifications get their own channel/id so they never
        // overwrite or get mixed up with new-post notifications
        val channelId = if (type == "update") "nexflix_updates" else "nexflix_posts"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channelName = if (type == "update") "App Updates" else "New Posts"
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        // Tapping an "update" notification opens the download link directly (browser),
        // tapping a "post" notification opens that exact post inside the app
        val intent: Intent
        if (type == "update") {
            intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(postUrl))
        } else {
            intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("post_url", postUrl)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // Load the thumbnail (if provided) and show it as a big picture, like a proper news app
        val bitmap: Bitmap? = try {
            if (!imageUrl.isNullOrEmpty()) {
                val stream = URL(imageUrl).openStream()
                android.graphics.BitmapFactory.decodeStream(stream)
            } else null
        } catch (e: Exception) {
            null
        }

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(body)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        nm.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onNewToken(token: String) {
        // Not needed since we notify everyone via the "new_posts" topic,
        // but logged here in case you want per-device targeting later.
    }
}
