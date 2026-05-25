package com.example.teamtaskmanager.notifications

import com.example.teamtaskmanager.R
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import android.app.NotificationManager

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notification = NotificationCompat.Builder(this, "task_channel")
            .setContentTitle(remoteMessage.notification?.title ?: "Nowe zadanie")
            .setContentText(remoteMessage.notification?.body ?: "")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random.nextInt(), notification)
    }
}