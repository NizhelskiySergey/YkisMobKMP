package com.ykis.ykismobkmp.services

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.ykis.ykismobkmp.domain.services.LogService

/**
 * [LogServiceImpl] — Android-реализация логгера.
 * Использует Firebase Analytics для событий и Crashlytics для ошибок.
 */
class LogServiceImpl : LogService {

  private val analytics: FirebaseAnalytics = Firebase.analytics

  override fun logEvent(event: String, params: Map<String, Any>) {
    // 1. Конвертируем Map в Android Bundle (требование Firebase SDK)
    val bundle = Bundle().apply {
      params.forEach { (key, value) ->
        when (value) {
          is String -> putString(key, value)
          is Int -> putInt(key, value)
          is Long -> putLong(key, value)
          is Double -> putDouble(key, value)
          is Boolean -> putBoolean(key, value)
          else -> putString(key, value.toString()) // Резервный вариант
        }
      }
    }
    // 2. Отправляем событие в облако
    analytics.logEvent(event, bundle)
  }
  override fun logNonFatalCrash(throwable: Throwable) {
    // Отправка некритической ошибки в Crashlytics
    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(throwable)
  }
}
