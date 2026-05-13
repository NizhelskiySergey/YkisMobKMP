package com.ykis.ykismobkmp.domain.repository.family


import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Убедись, что нет импортов из java или android пакетов!

/**
 * [FamilyRepositoryImpl] — Кроссплатформенная реализация репозитория состава семьи.
 * Синхронизирована с типом Long для addressId и очищена от Android-зависимостей.
 */
class FamilyRepositoryImpl(
  private val apiService: KtorApiService
) : FamilyRepository {

  private val tag = "Repository.Family"

  // ИСПРАВЛЕНО: Изменили тип параметра с Int на Long согласно сквозной схеме KMP
  override suspend fun getFamilyList(addressId: Long): GetFamilyResponse {
    println("[$tag.getFamilyList]: addressId=$addressId")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на сервере
      val paramsMap = mapOf("address_id" to addressId.toString())

      // 2. Делаем чистый вызов KtorApiService
      apiService.getFamilyList(map = paramsMap)

    } catch (ex: Exception) {
      println("[$tag.getFamilyList] Критическая ошибка сети: ${ex.message}")
      // Безопасно возвращаем объект ответа с зашитой ошибкой вместо падения приложения
      GetFamilyResponse(
        success = 0,
        message = ex.message ?: "Помилка зв'язку з сервером",
        family = emptyList()
      )
    }
  }


  /**
   * Метод очистки локальной БД.
   * Как мы договорились, вся логика транзакций кэша вынесена на уровень Koin/UseCase,
   * поэтому метод оставляем пустым, выполняя контракт интерфейса.
   */
  override suspend fun clearLocalFamily() {
    // Логика очистки теперь живет в Koin / UseCase / SQLDelight
  }
}


