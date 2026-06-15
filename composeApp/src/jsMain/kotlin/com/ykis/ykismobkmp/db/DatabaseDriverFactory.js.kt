package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * [DatabaseDriverFactory] — Бойова Web-реалізація СУБД.
 * ІСПРАВЛЕНО: Використовуємо WebWorkerDriver для збереження даних у IndexedDB браузера.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    // Створюємо воркер, який буде займатися записом на "диск" браузера
    val driver = WebWorkerDriver(
      Worker("sqldelight-worker.js")
    )
    return driver
  }
}

actual fun getInternalDriver(queries: Any): SqlDriver {
    return queries.asDynamic().driver as SqlDriver
}
