package com.ykis.ykismobkmp.domain.repository.services

import com.ykis.ykismobkmp.core.Constants.TOTAL
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetServiceResponse

private const val className = "ServiceRepositoryImpl"

/**
 * [ServiceRepositoryImpl] — Оптимизированная КМР-реализация репозитория коммунальных служб ЮКИС.
 * ИСПРАВЛЕНО: Промежуточный слой Remote полностью удален. Репозиторий напрямую работает с KtorApiService.
 */
class ServiceRepositoryImpl(
  private val apiService: KtorApiService // Прямой инжект Ktor клиента по стандарту YkisMobKMP
) : ServiceRepository {

  /**
   * [getFlatDetailService] — Получение детальной информации по начислениям и квитанциям.
   */
  override suspend fun getFlatDetailService(params: ServiceParams): GetServiceResponse {
    println("[$className.getFlatDetailService]: Запрос деталей ЖЕК/ОСББ для квартиры ID=${params.addressId}")

    return try {
      val params = mapOf(
        "uid" to params.uid,
        "address_id" to params.addressId.toString(),
        "year" to params.year,
        "service" to params.service.toString(),
        "total" to params.total.toString()
      )
      // Резолвим сетевой запрос напрямую через Ktor API без посредников
      apiService.getFlatService(params)
    } catch (ex: Exception) {
      println("[$className.getFlatDetailService] Критическая ошибка сети Ktor: ${ex.message}")
      GetServiceResponse(
        success = 0,
        message = ex.message ?: "Невідома помилка мережі розрахункового центру"
      )
    }
  }

  /**
   * [getTotalDebtService] — Получение суммарной задолженности по коммунальным предприятиям Южного.
   */
  override suspend fun getTotalDebtService(params: ServiceParams): GetServiceResponse {
    println("[$className.getTotalDebtService]: Запрос баланса ГИОЦ для лицевого счета Long ID=${params.addressId}")

    return try {
      val params = mapOf(
        "uid" to params.uid,
        "address_id" to params.addressId.toString(),
        "year" to params.year,
        "service" to params.service.toString(),
        "total" to params.total.toString()
      )
      // Резолвим сетевой запрос напрямую через Ktor API без посредников
      apiService.getFlatService(params)
    } catch (ex: Exception) {
      println("[$className.getTotalDebtService] Критическая ошибка сети Ktor: ${ex.message}")
      GetServiceResponse(
        success = 0,
        message = ex.message ?: "Помилка синхронізації заборгованостей"
      )
    }
  }
}
