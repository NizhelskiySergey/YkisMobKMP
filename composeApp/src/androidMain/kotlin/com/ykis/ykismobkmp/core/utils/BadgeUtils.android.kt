package com.ykis.ykismobkmp.core.utils


import android.app.NotificationManager
import android.content.Context
import android.util.Log

actual fun applyAppBadgeCount(count: Int) {
  // На Android 8.0+ бейджи (точки) привязаны к уведомлениям.
  try {
    // ИСПРАВЛЕНО: Безопасное получение контекста без прямого обращения к FirebaseApp в init
    val context = try {
       com.google.firebase.FirebaseApp.getInstance().applicationContext
    } catch (e: Exception) {
       return // Firebase еще не готов, пропускаем обновление
    }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    if (count == 0) {
      notificationManager.cancelAll()
      Log.d("YkisLog", "[BadgeUtils]: Notifications cleared (count is 0)")
    } else {
      Log.d("YkisLog", "[BadgeUtils]: Setting Android badge to $count (via notifications)")
    }
  } catch (e: Exception) {
    Log.e("YkisLog", "[BadgeUtils]: Error during badge update: ${e.message}")
  }
}
