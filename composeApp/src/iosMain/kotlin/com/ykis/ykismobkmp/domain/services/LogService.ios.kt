package com.ykis.ykismobkmp.domain.services

import platform.Foundation.NSLog // Нативный логгер Apple iOS

actual class LogService {
  actual fun logNonFatalCrash(throwable: Throwable) {
    NSLog("[LogService.ios] CRASH: %@ ", throwable.message ?: "Unknown")
  }
  actual fun logEvent(event: String, params: Map<String, Any>) {
    NSLog("[LogService.ios] EVENT: %@, PARAMS: %@", event, params.toString())
  }
}
