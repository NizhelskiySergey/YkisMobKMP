package com.ykis.ykismobkmp.domain.repository.ledger.request

import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.text.ifBlank
/**
 * [GetFlatServices] — Кроссплатформенный сценарий (Use Case) получения детализации начислений ЮКИС.
 */
/**
 * [GetFlatServices] — Кроссплатформенный сценарий (Use Case) получения детализации начислений ЮКИС.
 */
class GetFlatServices(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {
  private val className = "GetFlatServices"

  /**
   * [invoke] — Точка реактивного вызова бизнес-сценария на базовых параметрах.
   */
  operator fun invoke(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): Flow<Resource<List<ServiceEntity>>> = flow {
    val methodName = "invoke"

    // Переводим строковый маркер службы ГИОЦ в читаемый текстовый КМР-индекс
    val currentServiceType = when (service) {
      1.toByte() -> "voda"
      2.toByte() -> "teplo"
      3.toByte() -> "tbo"
      else -> "kv"
    }

    try {
      emit(Resource.Loading())

      // 1. БЫСТРЫЙ СТАРТ: Сначала мгновенно выдаем то, что уже закэшировано в локальной базе данных SQLDelight
      val cached: List<ServiceEntity> = ledgerCache.getServiceDetail(addressId, currentServiceType, year)
      if (cached.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Найдено ${cached.size} записей начислений в кэше")
        emit(Resource.Success(cached))
      }

      // 2. ОБНОВЛЕНИЕ: Запрашиваем свежий список через репозиторий (Ktor-клиент напрямую)
      println("[$className.$methodName]: [NETWORK_START] Запрос начислений для квартиры ID: $addressId")
      val response =
        repository.getFlatDetailService(
          uid = uid,
          addressId = addressId,
          houseId = houseId,
          year = year,
          service = service,
          total = total
        )

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ КЭША СУБД
      if (response.success == 1) {
        val remoteServices: List<ServiceEntity> = response.services ?: emptyList()

        // КРИТИЧЕСКИЙ ФИКС ДЛЯ СМАРТФОНА: Принудительно пускаем скачанный массив из 6 месяцев в UI слой НАПРЯМУЮ!
        // Теперь экран смартфона МГНОВЕННО перерисуется живыми данными из ОЗУ, полностью ликвидируя
        // пустые экраны, а фоновое сохранение SQLite выполнится параллельно в пуле потоков!
        emit(Resource.Success(remoteServices))

        if (remoteServices.isNotEmpty()) {
          // Очищаем старый кэш по данному лицевому счету и массово сохраняем новые квитанции ГИОЦ
          try {
            ledgerCache.deleteServiceByApartment(addressId)
            ledgerCache.addService(remoteServices)
            println("[$className.$methodName]: [SUCCESS] Локальный кэш услуг успешно обновлен в SQLDelight")
          } catch (dbEx: Exception) {
            println("[$className.${methodName}_WARN]: Ошибка перезаписи кэша начислений в СУБД: ${dbEx.message}")
          }
        }
      } else {
        // Сюда приложение зайдёт только если сервер Южного вернул success == 0 (Missing fields / DB Error)
        if (cached.isEmpty()) {
          val errorMsg = response.message?.ifBlank { "Дані про нарахування заборгованостей відсутні" }
            ?: "Дані про нарахування заборгованостей відсутні"
          println("[$className.$methodName]: [SERVER_REJECT] Реальный отказ сервера: $errorMsg")

          emit(Resource.Error<List<ServiceEntity>>(errorMsg))
        }
      }

    } catch (e: ResponseException) {
      val errorDescription = e.response.status.description
      println("[$className.$methodName]: [KTOR_RESPONSE_ERROR] Ошибка сервера биллинга: $errorDescription")
      SnackbarManager.showMessage(errorDescription)
      emit(Resource.Error<List<ServiceEntity>>("Помилка сервера білінгу: $errorDescription"))

    } catch (e: Exception) {
      println("[$className.$methodName]: [OFFLINE_OR_EXCEPTION] Сбой сети или процесса: ${e.message}. Проверка кэша...")

      val fallback: List<ServiceEntity> =
        ledgerCache.getServiceDetail(addressId, currentServiceType, year)

      if (fallback.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_RECOVERY] Сети нет, но выведен локальный кэш ГИОЦ")
        emit(Resource.Success(fallback))
        return@flow
      }

      SnackbarManager.showMessage("Помилка з'єднання з розрахунковим центром. Перевірте мережу.")
      emit(Resource.Error<List<ServiceEntity>>(e.message?.ifBlank { "Unknown Error" } ?: "Unknown Error"))
    }
  }.flowOn(Dispatchers.Default)
}

