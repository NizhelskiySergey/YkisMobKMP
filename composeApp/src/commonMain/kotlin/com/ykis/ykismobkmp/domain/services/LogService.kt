package com.ykis.ykismobkmp.domain.services

expect class LogService() {
  fun logNonFatalCrash(throwable: Throwable)
  fun logEvent(event: String, params: Map<String, Any> = emptyMap())
}
