package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory(context: Any? = null) { // Добавили параметр
  fun createDriver(): SqlDriver
}
