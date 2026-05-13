package com.ykis.ykismobkmp.domain.repository.apartment.usecase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


/**
 * [GetOsbbApartmentsList] — Сценарий получения списка квартир для ОСББ или дома.
 * Реализует глубокую синхронизацию (Очистка устаревших связанных таблиц + Массовая вставка).
 * Кроссплатформенно оперирует типами Long для targetId.
 */
class GetOsbbApartmentsList(
  private val repository: ApartmentRepository,
  // Лямбды полностью изолируют UseCase от деталей реализации БД (SQLDelight)
  private val getLocalAll: suspend () -> List<ApartmentEntity> = { emptyList() },
  private val syncFullDatabase: suspend (List<ApartmentEntity>) -> Unit = {}
) {
  operator fun invoke(targetId: Long, isHouseSearch: Boolean = false): Flow<Resource<List<ApartmentEntity>>> = flow {
    val type = if (isHouseSearch) "HOUSE" else "OSBB"
    val methodName = "UseCase.GetOsbbApartmentsList[$type]"

    try {
      println("[$methodName]: [START] TargetID: $targetId")
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (SQLDelight)
      val localList = getLocalAll()
      if (localList.isNotEmpty()) {
        println("[$methodName]: [LOCAL_HIT] Найдено ${localList.size} кв. в базе данных")
        emit(Resource.Success(localList))
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client)
      // В интерфейсе репозитория метод getOsbbApartmentsList тоже должен принимать Long
      val response = repository.getOsbbApartmentsList(targetId, isHouseSearch)
      val remoteApartments = response.apartments ?: emptyList()

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ БАЗЫ ДАННЫХ
      if (remoteApartments.isNotEmpty()) {
        // Вызываем синхронизацию (в Koin тут будет транзакция SQLDelight с очисткой хвостов)
        syncFullDatabase(remoteApartments)

        println("[$methodName]: [NETWORK_SUCCESS] Список успешно синхронизирован")
        emit(Resource.Success(remoteApartments))
      } else {
        println("[$methodName]: [NETWORK_EMPTY] Получен пустой ответ от сервера")
        if (localList.isEmpty()) emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[$methodName]: [FATAL_ERROR] ${ex.message}")

      // OFFLINE RECOVERY: Если нет связи, аварийно возвращаем локальный список
      val fallback = getLocalAll()
      if (fallback.isNotEmpty()) {
        println("[$methodName]: [OFFLINE_MODE] Сеть недоступна, работаем на локальном списке")
        emit(Resource.Success(fallback))
      } else {
        emit(Resource.Error(message = "Відсутній зв'язок. Список мешканців недоступний."))
      }
    }
  }.flowOn(Dispatchers.Default) // Очистка связанных таблиц — тяжелая операция, выполняем на фоне
}






