package com.ykis.ykismobkmp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ykis.ykismobkmp.MainActivity
import com.ykis.ykismobkmp.domain.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val firebaseService: FirebaseService by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("[YkisLogKMP.MessagingService]: Получен новый FCM токен: ${token.take(10)}...")
        serviceScope.launch {
            try {
                firebaseService.addFcmToken()
            } catch (e: Exception) {
                println("[YkisLogKMP.MessagingService_ERROR]: Не удалось обновить токен: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        println("[YkisLogKMP.MessagingService]: Получено Push-уведомление от ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "ЮКІС"
        val body = message.notification?.body ?: message.data["body"] ?: "Нове повідомлення"
        val chatId = message.data["chatId"]

        sendNotification(title, body, chatId)
    }

    private fun sendNotification(title: String, body: String, chatId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("chat_id", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "ykis_chat_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.ykis.ykismobkmp.R.drawable.ykis) // Используем нашу новую иконку
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Чат повідомлення ЮКІС",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()
}
