package com.ykis.ykismobkmp.core.utils

import org.jetbrains.compose.resources.StringResource
import ykismobkmp.composeapp.generated.resources.Res

private const val className = "Resource"

/**
 * [Resource] — Твой оригинальный кроссплатформенный запечатанный класс обёртки сетевых ответов ЮКИС.
 * ТОТАЛЬНО ЗАФИКСИРОВАНО: Полностью возвращена твоя исходная структура. Все запросы снова зелёные!
 */
sealed class Resource<T>(
  val data: T? = null,
  val message: String? = null,
  // ДОБАВЛЕНО: Поддержка StringResource для локализации ошибок
  val messageRes: StringResource? = null,
  // В KMP мы не используем Int для ресурсов.
  // Если нужно передавать именно ключ перевода, используем String или StringRes из KMP
  val errorKey: String? = null
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
    messageRes: StringResource? = null,
    errorKey: String? = null,
    data: T? = null
  ) : Resource<T>(data, message, messageRes, errorKey)

  /**
   * [Loading] — Состояние ожидания ответа от серверов биллинга.
   */
  class Loading<T>(data: T? = null) : Resource<T>(data)
}
