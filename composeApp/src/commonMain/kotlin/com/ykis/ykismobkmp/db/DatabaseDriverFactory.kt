package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver


/**
 * [DatabaseDriverFactory] — Кроссплатформенный expect-завод генерации SQLite драйверов.
 */
expect class DatabaseDriverFactory {
  fun createDriver(): SqlDriver
}

/**
 * [LocalAiEngine] — Локальный автономный движок искусственного интеллекта.
 */
class LocalAiEngine {
  fun processLocalPrompt(prompt: String): String = "Локальний ІІ відповідь: Контекст ГІОЦ Южне"
}

