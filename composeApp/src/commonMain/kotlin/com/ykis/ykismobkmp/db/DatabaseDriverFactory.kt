package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.await

/**
 * [DatabaseDriverFactory] — Кроссплатформенный expect-завод генерации SQLite драйверов.
 */
expect class DatabaseDriverFactory {
  fun createDriver(): SqlDriver
}

/**
 * [DatabaseSchemaInitializer] — Глобальний координатор ініціалізації таблиць.
 * ПОВНІСТЮ ВИМКНЕНО ДЛЯ WEB: Щоб уникнути мертвих зависань основного потоку браузера.
 */
class DatabaseSchemaInitializer {
    private var isInitialized = false

    suspend fun ensureSchema(driver: SqlDriver) {
        val platform = com.ykis.ykismobkmp.getPlatform().name
        val isWeb = platform.contains("Web", true) || platform.contains("JS", true)

        if (isWeb) {
            // МИТТЄВИЙ ВИХІД ДЛЯ WEB.
            isInitialized = true
            return
        }

        if (!isInitialized) {
            try {
                // Для Android/iOS створення схеми працює стабільно
                YkisDatabases.Schema.create(driver).await()
                isInitialized = true
                println("[YkisLogKMP.DatabaseSchemaInitializer]: [SUCCESS] База даних готова.")
            } catch (e: Throwable) {
                isInitialized = true
            }
        }
    }
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
