package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory actual constructor(context: Any?) { // Синхронизируем конструктор
  actual fun createDriver(): SqlDriver {
    val appDir = File(System.getProperty("user.home"), ".ykis_app")
    if (!appDir.exists()) appDir.mkdirs()
    val dbFile = File(appDir, "ykis_local_db.db")
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (!dbFile.exists() || dbFile.length() == 0L) {
      YkisDatabases.Schema.create(driver)
    }
    return driver
  }
}
