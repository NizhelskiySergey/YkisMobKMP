package com.ykis.ykismobkmp.domain.repository.apartment.usecase


import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetApartmentList] — Сценарий получения списка всех квартир пользователя.
 * Реализует стратегию «Сначала Кэш -> Запрос в сеть -> Синхронизация БД».
 */
class GetApartmentList(
  private val repository: ApartmentRepository,
  // Получение списка из SQLDelight через лямбду
  private val getLocal: suspend (String) -> List<ApartmentEntity> = { emptyList() },
  // Атомарное обновление всей базы (очистка + вставка новых)
  private val syncDatabase: suspend (List<ApartmentEntity>, String) -> Unit = { _, _ -> }
) {
  operator fun invoke(uid: String): Flow<Resource<List<ApartmentEntity>>> = flow {
    val methodName = "UseCase.GetApartmentList"

    if (uid.isBlank()) {
      println("[$methodName]: [ABORT] UID пустой")
      emit(Resource.Error("Помилка авторизації"))
      return@flow
    }

    try {
      emit(Resource.Loading())

      // 1. БЫСТРЫЙ СТАРТ: Показываем то, что уже есть в базе данных
      val localList = getLocal(uid)
      if (localList.isNotEmpty()) {
        println("[$methodName]: [LOCAL_HIT] Найдено ${localList.size} квартир в кэше")
        emit(Resource.Success(localList))
      }

      // 2. ОБНОВЛЕНИЕ: Идем на сервер за свежими данными (Ktor HTTP Client)
      println("[$methodName]: [NETWORK_START] Запрос для UID: ${uid.takeLast(5)}")
      val response = repository.getApartmentList(uid)

      if (response.success == 1) {
        val remoteApartments = response.apartments ?: emptyList()
        // Прошиваем UID пользователя для каждой квартиры (нужно для фильтрации в БД)
        val apartmentsWithUid = remoteApartments.map { it.copy(uid = uid) }

        // 3. СИНХРОНИЗАЦИЯ: Очищаем старое, записываем новое (внутри транзакции SQLDelight)
        syncDatabase(apartmentsWithUid, uid)

        println("[$methodName]: [SYNC_SUCCESS] База обновлена (${apartmentsWithUid.size} кв.)")
        emit(Resource.Success(apartmentsWithUid))
      } else {
        // Если сервер вернул ошибку, но у нас был кэш — мы его уже показали выше
        if (localList.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Не вдалося отримати список"))
        }
      }

    } catch (ex: Exception) {
      println("[$methodName]: [FATAL_ERROR] ${ex.message}")

      // OFFLINE MODE: Если нет сети, пробуем еще раз взять данные из базы
      val fallback = getLocal(uid)
      if (fallback.isNotEmpty()) {
        println("[$methodName]: [OFFLINE] Сеть недоступна, работаем на локальных данных")
        emit(Resource.Success(fallback))
      } else {
        emit(Resource.Error(message = "Сервіс недоступний. Перевірте інтернет"))
      }
    }
  }.flowOn(Dispatchers.Default) // Оставляем выполнение тяжелой фильтрации на пуле корутин
}








