package com.ykis.ykismobkmp.core.utils

private const val className = "Resource"

/**
 * [Resource] — Кроссплатформенный запечатанный контейнер сетевых ответов Ktor и Firestore API ЮКИС.
 * ИСПРАВЛЕНО: Наследники переведены на KMP-стандарты стабильности стейтов (data class / data object).
 */
sealed class Resource<out T>(
  val data: T? = null,
  val message: String? = null,
  // В KMP мы отказались от Int для ресурсов. Используем строковый ключ для JetBrains Res.string
  val errorKey: String? = null
) {
  /**
   * [Success] — Успешное выполнение операции (например, получение начислений ГИОЦ).
   */
  data class Success<out T>(val successData: T?) : Resource<T>(data = successData)

  /**
   * [Error] — Сбой транзакции, сети или валидации секретного кода админа ОСМД.
   */
  data class Error<out T>(
    val errorMessage: String? = null,
    val errorStringKey: String? = null,
    val errorData: T? = null
  ) : Resource<T>(data = errorData, message = errorMessage, errorKey = errorStringKey)

  /**
   * [Loading] — Фоновый лоадер или холодный старт синхронизации лицевых счетов.
   * ИСПРАВЛЕНО: Переведен на data class для обеспечения стабильности рекомпозиций в Compose Multiplatform.
   */
  data class Loading<out T>(val loadingData: T? = null) : Resource<T>(data = loadingData)
}
