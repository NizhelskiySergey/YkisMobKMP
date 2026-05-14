package com.ykis.ykismobkmp.domain.services

actual class LogService {
  actual fun logNonFatalCrash(throwable: Throwable) {
    println("[LogService.js] CRASH: ${throwable.message}")
  }
  actual fun logEvent(event: String, params: Map<String, Any>) {
    println("[LogService.js] EVENT: $event, PARAMS: $params")
  }
}
