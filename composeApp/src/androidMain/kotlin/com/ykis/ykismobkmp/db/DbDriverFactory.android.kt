package com.ykis.ykismobkmp.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory actual constructor(private val context: Any?) {
  actual fun createDriver(): SqlDriver {
    val androidContext = context as? Context ?: throw IllegalArgumentException("Android Context required")
    return AndroidSqliteDriver(
      schema = YkisDatabases.Companion.Schema,
      context = androidContext,
      name = "ykis_db.db"
    )
  }
}

