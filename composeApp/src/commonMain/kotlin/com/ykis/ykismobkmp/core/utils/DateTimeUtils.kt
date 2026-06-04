package com.ykis.ykismobkmp.core.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

object DateTimeUtils {
    
    /**
     * [isWithinOneHour] — Проверяет, прошло ли меньше часа с момента [dateTimeString].
     * Ожидаемый формат: "YYYY-MM-DD HH:MM:SS"
     */
    fun isWithinOneHour(dateTimeString: String?): Boolean {
        if (dateTimeString.isNullOrBlank() || dateTimeString == "Unknown") return false
        
        return try {
            // Заменяем пробел на 'T' для соответствия стандарту ISO-8601
            // Если дата в формате DD.MM.YYYY HH:MM:SS, парсинг не сработает, 
            // но обычно REST API возвращают ISO или YYYY-MM-DD.
            val normalizedDate = dateTimeString.trim()
                .replace(" ", "T")
                .let { if (it.contains("-")) it else it } // Можно добавить реверс для DD.MM.YYYY если нужно
            
            val readingTime = LocalDateTime.parse(normalizedDate).toInstant(TimeZone.currentSystemDefault())
            val now = Clock.System.now()
            
            val diffMs = now.toEpochMilliseconds() - readingTime.toEpochMilliseconds()
            // Разница между 0 и 60 минутами
            diffMs in 0..(60 * 60 * 1000)
        } catch (e: Exception) {
            println("[DateTimeUtils]: Ошибка парсинга даты '$dateTimeString': ${e.message}")
            false
        }
    }
}
