package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous

/**
 * [DatabaseDriverFactory] — Безопасная actual-реализация для iOS с резервным in-memory сценарием.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    val schema = YkisDatabases.Schema.synchronous()
    return try {
      // 1. Стандартная попытка открыть физический файл базы данных на iPhone
      NativeSqliteDriver(
        schema = schema,
        name = "ykis.db"
      )
    } catch (t: Throwable) {
      println("[DatabaseDriverFactory.ios.CRITICAL]: Нативный сбой диска iOS: ${t.message}")
      // 2. РЕШЕНИЕ НАМЕРТВО: Передаем пустую строку в качестве имени файла!
      NativeSqliteDriver(
        schema = schema,
        name = ""
      )
    }
  }
}

actual fun getInternalDriver(queries: Any): SqlDriver {
    error("Not used on iOS")
}
