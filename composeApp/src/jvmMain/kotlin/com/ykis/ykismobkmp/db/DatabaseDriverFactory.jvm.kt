@file:JvmName("DatabaseDriverJvmKt")

package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous

/**
 * [DatabaseDriverFactory] — Actual-реализация для Mac Desktop (JVM) / Windows.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      // Используем синхронную версию схемы для JVM JDBC драйвера
      YkisDatabases.Schema.synchronous().create(driver)
    } catch (e: Exception) {
      // Если таблицы уже были созданы ранее, пропускаем шаг миграции
    }
    return driver
  }
}

actual fun getInternalDriver(queries: Any): SqlDriver {
    error("Not used on JVM")
}
