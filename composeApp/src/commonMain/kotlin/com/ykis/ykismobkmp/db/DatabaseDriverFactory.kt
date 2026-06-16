package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [DatabaseDriverFactory] — Кроссплатформенный expect-завод генерации SQLite драйверов.
 */
expect class DatabaseDriverFactory {
  fun createDriver(): SqlDriver
}

/**
 * [DatabaseSchemaInitializer] — Глобальний координатор ініціалізації таблиць для Web.
 */
class DatabaseSchemaInitializer {
    private val mutex = Mutex()
    private var isInitialized = false

    suspend fun ensureSchema(driver: SqlDriver) {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) && !isInitialized) {
            mutex.withLock {
                if (isInitialized) return@withLock
                println("[YkisLogKMP.DatabaseSchemaInitializer]: [START] Створення ВСІХ таблиць БД...")
                try {
                    YkisDatabases.Schema.create(driver).await()
                    isInitialized = true
                    println("[YkisLogKMP.DatabaseSchemaInitializer]: [SUCCESS] Всі таблиці БД успішно створені")
                } catch (e: Throwable) {
                    val msg = e.message ?: "Unknown error"
                    println("[YkisLogKMP.DatabaseSchemaInitializer_WARN]: Помилка створення: $msg")
                    if (msg.contains("already exists", true) || msg.contains("exists", true)) {
                        isInitialized = true
                    }
                }
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
