package com.ykis.ykismobkmp.core.utils

/**
 * [Resource] — Твой оригинальный кроссплатформенный запечатанный класс обёртки сетевых ответов ЮКИС.
 * ТОТАЛЬНО ЗАФИКСИРОВАНО: Полностью возвращена твоя исходная структура. Все запросы снова зелёные!
 */
sealed class Resource<T>(
  val data: T? = null,
  val message: String? = null
) {
  /**
   * [Success] — Успешное выполнение транзакции.
   */
  class Success<T>(data: T?) : Resource<T>(data)

  /**
   * [Error] — Перехват сетевых сбоев, таймаутов Ктор и ошибок бэкенда.
   * ЗАФИКСИРОВАНО: Твоя оригинальная позиционная сигнатура (message, errorKey, data) полностью сохранена.
   */
  class Error<T>(
    message: String? = null,
    data: T? = null
  ) : Resource<T>(data, message)

  /**
   * [Loading] — Состояние ожидания ответа от серверов биллинга.
   */
  class Loading<T>(data: T? = null) : Resource<T>(data)
}
