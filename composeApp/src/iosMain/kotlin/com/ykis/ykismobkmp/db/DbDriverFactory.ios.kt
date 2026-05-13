package com.ykis.ykismobkmp.db
import app.cash.sqldelight.db.SqlDriver

/**
 * [com.ykis.ykismobkmp.ui.screens.components.DatabaseDriverFactory] — iOS реализация.
 * Автоматически закроет ошибки для iosArm64 и iosSimulatorArm64.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(
      schema = YkisDatabases.Schema,
      name = "ykis_db.db"
    )
  }
}
