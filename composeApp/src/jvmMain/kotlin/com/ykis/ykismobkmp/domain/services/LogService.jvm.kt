package com.ykis.ykismobkmp.domain.services

import java.io.File
import java.time.LocalDateTime

/**
 * [LogService] — JVM реализация для Mac Desktop. Пишет аудит действий админа ОСББ на жесткий диск.
 */
actual class LogService {

  private val logFile: File by lazy {
    val userHome = System.getProperty("user.home")
    val appDir = File(userHome, ".ykis_app")
    if (!appDir.exists()) appDir.mkdirs()
    File(appDir, "ykis_desktop_logs.txt")
  }

  private fun writeToFile(level: String, message: String) {
    try {
      val timestamp = LocalDateTime.now().toString()
      logFile.appendText("[$timestamp] [$level] $message\n")
    } catch (e: Exception) {
      println("[LogService.jvm] Ошибка записи в файл логов: ${e.message}")
    }
  }

  actual fun logNonFatalCrash(throwable: Throwable) {
    val errorText = "Крэш: ${throwable.message}\n${throwable.stackTraceToString()}"
    System.err.println("[LogService.jvm]: CRASH REPORT -> $errorText")
    writeToFile("ERROR", errorText)
  }

  actual fun logEvent(event: String, params: Map<String, Any>) {
    val eventText = "Подія: $event, Параметри: $params"
    println("[LogService.jvm]: EVENT -> $eventText")
    writeToFile("INFO", eventText)
  }
}
