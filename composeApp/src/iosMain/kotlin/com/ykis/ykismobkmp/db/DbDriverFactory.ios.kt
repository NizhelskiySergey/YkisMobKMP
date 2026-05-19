@file:JvmName("DatabaseDriverIosKt")

package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver // Нативный iOS/Apple артефакт
import kotlin.jvm.JvmName

/**
 * [DatabaseDriverFactory] — Actual-реализация для операционной системы iOS.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(
      schema = YkisDatabases.Schema,
      name = "ykis.db"
    )
  }
}
