package com.ykis.ykismobkmp.domain.repository.services.request

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.services.ServiceParams
import com.ykis.ykismobkmp.domain.repository.services.ServiceRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// Временная заглушка кастомного исключения, если оно объявлено в твоем core-пакете
class ExceptionWithResourceMessage(val resourceMessage: String) : Exception()

private const val className = "GetTotalDebtServices"

/**
 * [GetTotalDebtServices] — Кроссплатформенный сценарий (Use Case) получения суммарной задолженности ГИОЦ.
 * ИСПРАВЛЕНО: Room DAO полностью удален, внедрен КМР паттерн инжекции лямбд кэширования по аналогии с GetFamilyList.
 * Ошибки Resource.Error переведены на оригинальный позиционный стандарт.
 */
class GetTotalDebtServices(
  private val repository: ServiceRepository,
  // Нативный лямбда-инжект выборки и записи, полностью изолированный от платформенных баз данных
  private val getLocal: suspend (Long) -> ServiceEntity? = { null },
  private val saveLocal: suspend (List<ServiceEntity>) -> Unit = {}
) {
  /**
   * [invoke] — Точка реактивного вызова бизнес-сценария.
   * Логирование рантайма согласно правилу [Класс.Метод].
   */
  operator fun invoke(params: ServiceParams): Flow<Resource<ServiceEntity>> = flow {
    val methodName = "GetTotalDebt"
    try {
      println("[$className.$methodName]: [START] AddrID: ${params.addressId}, UID: ${params.uid}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Через Ktor клиент репозитория биллинга г. Южного)
      val response = repository.getTotalDebtService(
        ServiceParams(
          uid = params.uid,
          addressId = params.addressId,
          houseId = params.houseId,
          year = params.year,
          service = params.service,
          total = params.total,
        )
      )

      if (response.success == 1 && response.services.isNotEmpty()) {
        val serviceData = response.services[0]
        println("[$className.$methodName]: [NETWORK_SUCCESS] Debt: ${serviceData.dolg}")

        // Вызываем лямбду сохранения пакета в БД (в Koin сюда пробросится транзакция SQLDelight)
        saveLocal(response.services)
        emit(Resource.Success(serviceData))
      } else {
        println("[$className.$methodName]: [SERVER_REJECT] Success=0 или список пуст")
        // Пытаемся взять из локального SQLite кэша, если сеть ответила отказом
        val totalDebt = getLocal(params.addressId)
        if (totalDebt != null) {
          println("[$className.$methodName]: [DB_FALLBACK] Найдено в базе после отказа сети")
          emit(Resource.Success(totalDebt))
        } else {
          // ИСПРАВЛЕНО: Позиционный аргумент без именованного префикса message =
          emit(Resource.Error<ServiceEntity>("Дані відсутні"))
        }
      }

    } catch (e: ResponseException) {
      println("[$className.$methodName]: [HTTP_ERROR] Код статуса Ktor: ${e.response.status}")
      SnackbarManager.showMessage(e.response.status.description)
      emit(Resource.Error<ServiceEntity>())

    } catch (e: Exception) {
      // ИСПРАВЛЕНО: Каскад catch блоков (IOException/Exception) объединен в универсальный КМР-перехват,
      // нативно отрабатывающий оффлайн-режим на Mac, Android и iOS
      println("[$className.$methodName]: [EXCEPTION_OR_OFFLINE] Сбой связи Ktor: ${e.message}. Проверка локального кэша.")

      // Чтение из локальной базы данных SQLDelight при отсутствии интернета
      val totalDebt = getLocal(params.addressId)
      if (totalDebt != null) {
        println("[$className.$methodName]: [OFFLINE_MODE] Выведены оффлайн данные из SQLite кэша")
        emit(Resource.Success(totalDebt))
      } else {
        // ИСПРАВЛЕНО: Позиционный аргумент без именованного префикса message =
        emit(Resource.Error<ServiceEntity>("Перевірте підключення до інтернету"))
      }
    }
  }.flowOn(Dispatchers.Default) // Безопасное КМР-переключение потоков для всех ОС
}


