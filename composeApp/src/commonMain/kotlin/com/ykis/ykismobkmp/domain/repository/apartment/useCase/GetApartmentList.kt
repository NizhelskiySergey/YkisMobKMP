package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetApartmentList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetApartmentList"

  operator fun invoke(uid: String): Flow<Resource<List<ApartmentEntity>>> = flow {

    if (uid.isBlank()) {
      emit(Resource.Error("Помилка авторизації"))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. СЕТЕВОЙ ЗАПРОС (ОСНОВНОЙ ДЛЯ WEB)
      val response: GetApartmentsResponse = repository.getApartmentList(uid)

      if (response.success == 1) {
        val apartments = response.apartments ?: emptyList()
        val apartmentsWithUid = apartments.map { it.copy(uid = uid) }
        
        emit(Resource.Success(apartmentsWithUid))

        // Пишем в кэш только если НЕ Web
        if (!isWeb && apartmentsWithUid.isNotEmpty()) {
            try { cache.insertApartmentList(apartmentsWithUid) } catch (e: Exception) {}
        }
      } else {
        // Если сервер ответил ошибкой - для веба просто отдаем пустой список (гасим лоадер)
        emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.$className]: Network fail: ${ex.message}")
      
      // Fallback: для веба отдаем пусто, для мобилок пробуем кэш
      if (isWeb) {
          emit(Resource.Success(emptyList()))
      } else {
          try {
              val local = cache.getApartmentsByUser().filter { it.uid == uid }
              emit(Resource.Success(local))
          } catch (e: Exception) {
              emit(Resource.Success(emptyList()))
          }
      }
    }
  }.flowOn(Dispatchers.Default)
}
