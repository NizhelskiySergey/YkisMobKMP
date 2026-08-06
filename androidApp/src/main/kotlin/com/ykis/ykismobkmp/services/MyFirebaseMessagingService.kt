package com.ykis.ykismobkmp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ykis.ykismobkmp.MainActivity
import com.ykis.ykismobkmp.R
import com.ykis.ykismobkmp.domain.services.FirebaseService
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
        getBitmapWithCoil(imageUrl)
      } else null

      withContext(Dispatchers.Main) {
        sendNotification(title, body, chatId, bitmap)
      }
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    scope.launch {
      firebaseService.addFcmToken()
    }
  }

  private suspend fun getBitmapWithCoil(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val loader = ImageLoader(this@MyFirebaseMessagingService)
      val request = ImageRequest.Builder(this@MyFirebaseMessagingService)
        .data(imageUrl)
        .allowHardware(false)
        .build()

      val result = loader.execute(request)
      if (result is SuccessResult) {
        // Пытаемся получить Bitmap из Coil Image (Android extension)
        result.image.toBitmap()
      } else null
    } catch (e: Exception) {
      null
    }
  }

  @SuppressLint("WrongConstant")
  private fun sendNotification(title: String, body: String, chatId: String?, image: Bitmap? = null, badgeCount: Int = 0) {
    val intent = Intent(this, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      putExtra("chatId", chatId)
    }

    val pendingIntent = PendingIntent.getActivity(
      this, 0, intent,
      PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
    )

    val channelId = "ykis_chat_notifications"
    val notificationBuilder = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
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
      )
      notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
  }

  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}
