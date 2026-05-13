package com.ykis.ykismobkmp.domain.services

interface LogService {
  fun logNonFatalCrash(throwable: Throwable)
  fun logEvent(event: String, params: Map<String, Any> = emptyMap())
}
