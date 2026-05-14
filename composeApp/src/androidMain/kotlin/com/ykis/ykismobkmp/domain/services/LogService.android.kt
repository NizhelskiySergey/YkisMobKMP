package com.ykis.ykismobkmp.domain.services

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * [LogService] — Android-реализация. Связана напрямую с облачными консолями Google.
 */
actual class LogService {

  private val crashlytics get() = FirebaseCrashlytics.getInstance()
  // На Android Analytics инициализируется через контекст, но Firebase предоставляет глобальный синглтон во многих SDK
  // Если в проекте Analytics не подключена в gradle нативно, закомментируй блок analytics
  private val analytics = FirebaseAnalytics.getInstance(com.google.firebase.FirebaseApp.getInstance().applicationContext)

  actual fun logNonFatalCrash(throwable: Throwable) {
    println("[LogService.android]: Перехват нефатальної помилки: ${throwable.message}")
    crashlytics.recordException(throwable) // Отправка стэк-трейса в Firebase Crashlytics
  }

  actual fun logEvent(event: String, params: Map<String, Any>) {
    println("[LogService.android]: Лог події [$event] з параметрами: $params")

    // Переводим Map<String, Any> в нативный Android Bundle для корректного разбора Google Analytics
    val bundle = Bundle()
    params.forEach { (key, value) ->
      when (value) {
        is String -> bundle.putString(key, value)
        is Int -> bundle.putInt(key, value)
        is Long -> bundle.putLong(key, value)
        is Double -> bundle.putDouble(key, value)
        is Boolean -> bundle.putBoolean(key, value)
        else -> bundle.putString(key, value.toString())
      }
    }
    analytics.logEvent(event, bundle)
  }
}
