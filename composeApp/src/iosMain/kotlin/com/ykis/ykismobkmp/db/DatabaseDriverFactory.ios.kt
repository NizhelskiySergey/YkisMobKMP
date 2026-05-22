package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * [DatabaseDriverFactory] — Безопасная actual-реализация для iOS с резервным in-memory сценарием.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    return try {
      // 1. Стандартная попытка открыть физический файл базы данных на iPhone
      NativeSqliteDriver(
        schema = com.ykis.ykismobkmp.db.YkisDatabases.Schema,
        name = "ykis.db"
      )
    } catch (t: Throwable) {
      println("[DatabaseDriverFactory.ios.CRITICAL]: Нативный сбой диска iOS: ${t.message}")
      // 2. РЕШЕНИЕ НАМЕРТВО: Передаем пустую строку в качестве имени файла!
      // По стандарту SQLDelight 2.x это принудительно разворачивает SQLite внутри ОЗУ симулятора,
      // полностью защищая Koin от InstanceCreationException и стирая пустой экран!
      NativeSqliteDriver(
        schema = com.ykis.ykismobkmp.db.YkisDatabases.Schema,
        name = ""
      )
    }
  }
}
