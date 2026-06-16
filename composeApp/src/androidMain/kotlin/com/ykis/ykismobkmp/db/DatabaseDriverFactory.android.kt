@file:JvmName("DatabaseDriverAndroidKt")

package com.ykis.ykismobkmp.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous

/**
 * [DatabaseDriverFactory] — Actual-реализация для операционной системы Android.
 */
actual class DatabaseDriverFactory(private val context: Context) {
  actual fun createDriver(): SqlDriver {
    // На Android используем синхронную обертку над схемой, так как драйвер нативный.
    // Убираем именованные параметры для предотвращения ошибок несоответствия версий в KMP.
    return AndroidSqliteDriver(
      YkisDatabases.Schema.synchronous(),
      context,
      "ykis.db"
    )
  }
}

actual fun getInternalDriver(queries: Any): SqlDriver {
    error("Not used on Android")
}
