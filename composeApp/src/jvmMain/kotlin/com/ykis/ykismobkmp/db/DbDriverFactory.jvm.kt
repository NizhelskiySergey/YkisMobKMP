@file:JvmName("DatabaseDriverJvmKt")

package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver // Десктопный JDBC SQLite артефакт

/**
 * [DatabaseDriverFactory] — Actual-реализация для Mac Desktop (JVM) / Windows.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    // Создаем драйвер в памяти или привязываем к локальному файлу ykis.db на диске Mac
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      // Принудительно накатываем структуру таблиц биллинга ЮКИС при первом запуске десктопа
      YkisDatabases.Schema.create(driver)
    } catch (e: Exception) {
      // Если таблицы уже были созданы ранее, пропускаем шаг миграции
    }
    return driver
  }
}
