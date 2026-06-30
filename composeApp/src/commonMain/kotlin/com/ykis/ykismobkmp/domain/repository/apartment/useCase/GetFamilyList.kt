package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetFamilyList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetFamilyList"

  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<FamilyEntity>>> = flow {
    if (uid.isBlank() || addressId <= 0L) {
      emit(Resource.Success(emptyList()))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. СПРОБА КЕШУ (Тільки для мобільних додатків)
      if (!isWeb) {
          try {
              val local = cache.getFamilyByApartment(addressId)
              if (local.isNotEmpty()) emit(Resource.Success(local))
          } catch (e: Exception) { }
      }

      // 2. МЕРЕЖЕВИЙ ЗАПИТ (Для всіх)
      println("[$className]: Запит складу сім'ї для ID: $addressId")
      val response = repository.getFamilyList(uid, addressId)
      val remoteFamily = response.family

      if (response.success == 1) {
        emit(Resource.Success(remoteFamily))

        // 3. ОНОВЛЕННЯ КЕШУ (Тільки для мобільних)
        if (!isWeb && remoteFamily.isNotEmpty()) {
            try { cache.syncFamilyList(addressId, remoteFamily) } catch (e: Exception) { }
        }
      } else {
          emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Success(emptyList()))
    }
  }.flowOn(Dispatchers.Default)
}
