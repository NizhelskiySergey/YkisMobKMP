package com.ykis.ykismobkmp.domain.services

import platform.Foundation.NSLog // Родной логгер iOS

class LogServiceImpl : LogService {

  override fun logEvent(event: String, params: Map<String, Any>) {
    // На iOS выводим в системный лог NSLog
    NSLog("[EVENT] $event: $params")
  }

  override fun logNonFatalCrash(throwable: Throwable) {
    // В базовой версии выводим ошибку в консоль
    NSLog("[ERROR] Non-fatal: ${throwable.message}")
    // На iOS можно добавить интеграцию с Sentry или Crashlytics позже
  }
}
