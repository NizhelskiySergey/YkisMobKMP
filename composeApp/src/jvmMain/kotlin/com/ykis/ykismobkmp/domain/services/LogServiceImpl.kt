package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.domain.services.LogService

class LogServiceImpl : LogService {
  // В JVM реализации МЫ НЕ ИСПОЛЬЗУЕМ FirebaseAnalytics
  // Поэтому никаких импортов Firebase тут быть не должно!

  override fun logEvent(event: String, params: Map<String, Any>) {
    println("[JVM EVENT]: $event | Params: $params")
  }

  override fun logNonFatalCrash(throwable: Throwable) {
    System.err.println("[JVM ERROR]: ${throwable.message}")
    throwable.printStackTrace()
  }
}

