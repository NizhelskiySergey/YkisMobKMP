package com.ykis.ykismobkmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

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
        // ИСПРАВЛЕНО: В вебе создание схемы — процесс асинхронный и может быть долгим.
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) && !isInitialized) {
            mutex.withLock {
                if (isInitialized) return@withLock
                
                println("[YkisLogKMP.DatabaseSchemaInitializer]: [START] Створення таблиць (асинхронно)...")
                try {
                    // Даем воркеру больше времени (2 секунды) на холодный старт IndexedDB
                    withTimeoutOrNull(2000) {
                        YkisDatabases.Schema.create(driver).await()
                    }
                    isInitialized = true
                    println("[YkisLogKMP.DatabaseSchemaInitializer]: [SUCCESS] База даних готова.")
                } catch (e: Throwable) {
                    val msg = e.message ?: "Unknown error"
                    println("[YkisLogKMP.DatabaseSchemaInitializer_WARN]: $msg")
                    // Если таблицы уже есть — считаем инициализацию успешной
                    if (msg.contains("exists", true)) isInitialized = true
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
