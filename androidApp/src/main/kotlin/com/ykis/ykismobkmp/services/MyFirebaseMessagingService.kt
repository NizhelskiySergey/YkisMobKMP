package com.ykis.ykismobkmp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
    println("[YkisLogKMP.MessagingService]: ПРИШЕЛ ПУШ! От: ${message.from}")
    println("[YkisLogKMP.MessagingService]: Данные: ${message.data}")

    // Поддержка и блока notification, и блока data (для универсальности)
    val title = message.data["title"] ?: message.notification?.title ?: "ЮКІС"
    val body = message.data["body"] ?: message.notification?.body ?: "Нове повідомлення"
    val chatId = message.data["chatId"]
    val imageUrl = message.data["image"] ?: message.data["imageUrl"] ?: message.notification?.imageUrl?.toString()

    // Получаем число для бэйджа (из блока data или notification)
    val badgeCount = (message.data["badge"]?.toIntOrNull())
      ?: (message.notification?.notificationCount)
      ?: 0

    val activeChat = ChatScreenModel.activeChatIdForNotifications
    println("[YkisLogKMP.MessagingService]: ПУШ ПОЛУЧЕН! Чат: $chatId, Бэйдж: $badgeCount")

    if (chatId != null && chatId == activeChat) {
      println("[YkisLogKMP.MessagingService]: Пуш подавлен (Чат уже открыт)")
      // Сбрасываем бэйдж на иконке, раз мы уже в этом чате
      updateLauncherBadge(0)
      return
    }

    updateLauncherBadge(badgeCount)

    serviceScope.launch {
      val bitmap = if (!imageUrl.isNullOrBlank()) getBitmapFromUrl(imageUrl) else null
      withContext(Dispatchers.Main) {
        sendNotification(title, body, chatId, bitmap, badgeCount)
      }
    }
  }

  private fun updateLauncherBadge(count: Int) {
    try {
      // Стандартный способ
      val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
      badgeIntent.putExtra("badge_count", count)
      badgeIntent.putExtra("badge_count_package_name", packageName)
      badgeIntent.putExtra("badge_count_class_name", "com.ykis.ykismobkmp.MainActivity")
      sendBroadcast(badgeIntent)
      println("[YkisLogKMP.MessagingService]: Launcher Badge Update: $count")
    } catch (_: Exception) { }
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
      .setSmallIcon(R.drawable.ykis)
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setNumber(badgeCount) // Передаем число в систему
      .setPriority(NotificationCompat.PRIORITY_HIGH)

    if (image != null) {
      notificationBuilder.setLargeIcon(image)
      notificationBuilder.setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(image)
          .bigLargeIcon(null as Bitmap?)
      )
    }

    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Чат повідомлення ЮКІС",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        setShowBadge(true)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val notificationId = chatId?.hashCode() ?: currentTimeMillis().toInt()
    notificationManager.notify(notificationId, notificationBuilder.build())

    // Попытка отправить универсальный сигнал бэйджа для Samsung/Sony
    if (badgeCount > 0) {
      try {
        val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
        badgeIntent.putExtra("badge_count", badgeCount)
        badgeIntent.putExtra("badge_count_package_name", packageName)
        badgeIntent.putExtra("badge_count_class_name", "com.ykis.ykismobkmp.MainActivity")
        sendBroadcast(badgeIntent)
      } catch (_: Exception) { }
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
      println("[YkisLogKMP.MessagingService_ERROR]: Ошибка загрузки изображения для пуша: ${e.message}")
      null
    }
  }

  private fun currentTimeMillis(): Long = System.currentTimeMillis()
}
