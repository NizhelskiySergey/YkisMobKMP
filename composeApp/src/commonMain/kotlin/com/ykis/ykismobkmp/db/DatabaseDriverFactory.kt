package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver


/**
 * [DatabaseDriverFactory] — Кроссплатформенный expect-завод генерации SQLite драйверов.
 */
expect class DatabaseDriverFactory {
  fun createDriver(): SqlDriver
}

/**
 * [getInternalDriver] — Вспомогательная функция для доступа к защищенному драйверу SQLDelight.
 */
expect fun getInternalDriver(queries: Any): SqlDriver

/**
 * [LocalAiEngine] — Локальный автономный движок искусственного интеллекта.
 */
class LocalAiEngine {
  fun processLocalPrompt(prompt: String): String = "Локальний ІІ відповідь: Контекст ГІОЦ Южне"
}

