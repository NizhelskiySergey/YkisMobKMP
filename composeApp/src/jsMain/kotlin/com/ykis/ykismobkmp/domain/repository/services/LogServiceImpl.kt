package com.ykis.ykismobkmp.domain.repository.services

import com.ykis.ykismobkmp.domain.services.LogService

/**
 * [LogServiceImpl] — JS-реализация логгера для Web-версии.
 * Выводит данные в консоль разработчика браузера (F12).
 */
class LogServiceImpl : LogService {

  private val className = "LogServiceImpl"

  override fun logEvent(event: String, params: Map<String, Any>) {
    // В JS println() автоматически перенаправляется в console.log
    val paramsString = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
    println("[$className.logEvent]: 🌐 JS_EVENT: $event | DATA: {$paramsString}")
  }

  override fun logNonFatalCrash(throwable: Throwable) {
    // Для ошибок в JS лучше всего использовать вывод в консоль ошибок,
    // чтобы в браузере они подсвечивались красным.
    val message = "[$className.logNonFatalCrash]: 🛑 JS_ERROR: ${throwable.message}"

    // Нативный вызов консоли браузера через Kotlin/JS
    console.error(message)

    // Вывод стека ошибки в консоль
    throwable.printStackTrace()
  }
}
