package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetFamilyList] — Сценарий получения состава семьи по квартире.
 * Поддерживает стратегию: Сначала локальный кэш SQLDelight -> Сеть Ktor -> Синхронизация.
 * Кроссплатформенно оперирует типами Long для addressId.
 */
class GetFamilyList(
    private val repository: ApartmentRepository,
  // Изменили тип аргумента с Int на Long для бесшовной интеграции с SQLDelight в Koin
    private val getLocal: suspend (Long) -> List<FamilyEntity> = { emptyList() },
    private val saveLocal: suspend (List<FamilyEntity>) -> Unit = {}
) {
  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<FamilyEntity>>> = flow {
      val methodName = "UseCase.GetFamilyList"

      try {
          emit(Resource.Loading())

          // 1. БЫСТРЫЙ СТАРТ: Сначала показываем то, что уже есть в базе данных
          val cached = getLocal(addressId)
          if (cached.isNotEmpty()) {
              println("[$methodName]: [LOCAL_HIT] Найдено ${cached.size} членов семьи в кэше")
              emit(Resource.Success(cached))
          }

          // 2. ОБНОВЛЕНИЕ: Запрашиваем свежий список через репозиторий (Ktor)
          println("[$methodName]: [NETWORK_START] ID: $addressId")

          // Теперь метод возвращает GetFamilyResponse вместо сырого списка
          val response = repository.getFamilyList(uid,addressId)
          val remoteFamily = response.family

          // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ КЭША
          if (response.success == 1 && remoteFamily.isNotEmpty()) {
              // Сохраняем актуальный список в БД (в Koin тут будет транзакция SQLDelight)
              saveLocal(remoteFamily)

              println("[$methodName]: [SUCCESS] Локальная база успешно синхронизирована")
              emit(Resource.Success(remoteFamily))
          } else {
              // Если сервер вернул success = 0 или список пуст, но локально что-то было — мы это уже отдали.
              // Ошибку кидаем только если в базе пусто и сеть не отдала данные.
              if (cached.isEmpty()) {
                  val errorMsg = response.message.ifBlank { "Дані про склад сім'ї відсутні" }
                  println("[$methodName]: [SERVER_REJECT] $errorMsg")
                  emit(Resource.Error(message = errorMsg))
              }
          }

      } catch (ex: Exception) {
          println("[$methodName]: [FATAL_ERROR] ${ex.message}")

          // OFFLINE RECOVERY: Если сеть полностью упала, аварийно пробуем выдать кэш еще раз
          val fallback = getLocal(addressId)
          if (fallback.isNotEmpty()) {
              println("[$methodName]: [OFFLINE_MODE] Отдаем кэш для оффлайн режима")
              emit(Resource.Success(fallback))
          } else {
              emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
          }
      }
  }.flowOn(Dispatchers.Default) // Оставляем выполнение фильтрации списков на пуле корутин
}
