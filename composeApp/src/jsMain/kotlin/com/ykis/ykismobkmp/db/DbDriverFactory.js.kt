package com.ykis.ykismobkmp.db


import app.cash.sqldelight.db.SqlDriver

/**
 * [com.ykis.ykismobkmp.ui.screens.components.DatabaseDriverFactory] — JS заглушка для успешной компиляции Web-версии.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    // Возвращаем пустую заглушку или выбрасываем исключение при вызове,
    // так как в Web-версии локальный кэш пока не используется.
    throw UnsupportedOperationException("SQLDelight driver for JS is not implemented yet")
  }
}
