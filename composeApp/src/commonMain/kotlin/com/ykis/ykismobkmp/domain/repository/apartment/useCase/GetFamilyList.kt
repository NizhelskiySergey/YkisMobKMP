package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetFamilyList] — Сценарий получения состава семьи по лицевому счету квартиры.
 */
class GetFamilyList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetFamilyList"

  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<FamilyEntity>>> = flow {
    val methodName = "invoke"

    try {
      emit(Resource.Loading())

      // 1. БЫСТРЫЙ СТАРТ: Безопасно для Web (с таймаутом)
      var cached: List<FamilyEntity> = emptyList()
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            kotlinx.coroutines.withTimeoutOrNull(500) {
                cached = cache.getFamilyByApartment(addressId)
            }
        } else {
            cached = cache.getFamilyByApartment(addressId)
        }

        if (cached.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Найдено ${cached.size} членов семьи")
          emit(Resource.Success(cached))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ОБНОВЛЕНИЕ: Запрашиваем свежий список через репозиторий (Ktor)
      println("[$className.$methodName]: [NETWORK_START] ID: $addressId")
      val response = repository.getFamilyList(uid, addressId)
      val remoteFamily = response.family ?: emptyList()

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ КЭША
      if (response.success == 1) {
        try {
            if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
                cache.syncFamilyList(addressId = addressId, familyList = remoteFamily)
            }
        } catch (dbEx: Exception) {
            println("[$className.$methodName]: Помилка запису в кеш: ${dbEx.message}")
        }

        println("[$className.$methodName]: [SUCCESS] Отримано ${remoteFamily.size} мешканців")
        emit(Resource.Success(remoteFamily))
      } else {
        // Если сервер вернул success = 0 или список пуст, но локально что-то было — мы это уже отдали.
        // Ошибку кидаем только если в базе пусто и сеть не вернула данные.
        if (cached.isEmpty()) {
          val errorMsg = response.message?.ifBlank { "Дані про склад сім'ї відсутні" } ?: "Дані про склад сім'ї відсутні"
          println("[$className.$methodName]: [SERVER_REJECT] $errorMsg")

          // Позиционная передача строки в конструктор Resource.Error без 'message ='
          emit(Resource.Error<List<FamilyEntity>>(errorMsg))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой загрузки состава семьи: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: Если сеть полностью упала, аварийно пробуем выдать кэш еще раз
      val fallback = cache.getFamilyByApartment(addressId)
      if (fallback.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_MODE] Отдаем кэш для оффлайн режима")
        emit(Resource.Success(fallback))
      } else {
        // Позиционная передача строки в конструктор Resource.Error без 'message ='
        emit(Resource.Error<List<FamilyEntity>>("Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // Выполнение фильтрации и маппинг списков выполняются в фоновом пуле корутин
}

