package com.ykis.ykismobkmp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ykis.ykismobkmp.MainActivity
import com.ykis.ykismobkmp.R
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.IO + job)
  private val firebaseService: FirebaseService by inject()

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)

    val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Нове повідомлення"
    val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
    val chatId = remoteMessage.data["chatId"] ?: remoteMessage.data["chat_id"]
    val imageUrl = remoteMessage.data["image"] ?: remoteMessage.notification?.imageUrl?.toString()

    println("[MyFirebaseMessagingService]: Отримано пуш. Title: $title, ChatId: $chatId")

    scope.launch {
      val bitmap = if (!imageUrl.isNullOrBlank()) {
        getBitmapFromUrl(imageUrl)
      } else null

      withContext(Dispatchers.Main) {
        sendNotification(title, body, chatId, bitmap)
      }
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    println("[MyFirebaseMessagingService]: Отримано новий токен: $token")
    scope.launch {
      firebaseService.addFcmToken()
    }
  }

  private suspend fun getBitmapFromUrl(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val url = URL(imageUrl)
      val connection = url.openConnection() as HttpURLConnection
      connection.doInput = true
      connection.connect()
      val input = connection.inputStream
      BitmapFactory.decodeStream(input)
    } catch (e: Exception) {
      null
    }
  }

  @SuppressLint("WrongConstant")
  private fun sendNotification(title: String, body: String, chatId: String?, image: Bitmap? = null, badgeCount: Int = 0) {
    val intent = Intent(this, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      putExtra("chatId", chatId)
      putExtra("chat_id", chatId)
    }

    val pendingIntent = PendingIntent.getActivity(
      this, 0, intent,
      PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
    )

    val channelId = "ykis_chat_notifications"
    val notificationBuilder = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher) // Используем иконку приложения
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setNumber(badgeCount)
      .setPriority(NotificationCompat.PRIORITY_HIGH)

    if (image != null) {
      notificationBuilder.setLargeIcon(image)
      notificationBuilder.setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(image)
          .setSummaryText(body)
      )
    }

    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Чат повідомлення",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Сповіщення про нові повідомлення у чаті"
      }
      notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
  }

  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}
