@file:JvmName("DatabaseDriverAndroidKt")

package com.ykis.ykismobkmp.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver // Нативный Android SQLite артефакт

/**
 * [DatabaseDriverFactory] — Actual-реализация для операционной системы Android.
 */
actual class DatabaseDriverFactory(private val context: Context) {
  actual fun createDriver(): SqlDriver {
    // Нативно разворачиваем ykis.db на базе сгенерированной SQLDelight схемы таблиц!
    return AndroidSqliteDriver(
      schema = YkisDatabases.Schema,
      context = context,
      name = "ykis.db"
    )
  }
}

actual fun getInternalDriver(queries: Any): SqlDriver {
    error("Not used on Android")
}
