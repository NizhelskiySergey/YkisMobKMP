package com.ykis.ykismobkmp.domain.repository.ledger.request

import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetFlatServices] — Use Case отримання деталізації нарахувань.
 * УНІФІКОВАНО: Пряма робота з мережею для Web, кешування для мобільних.
 */
class GetFlatServices(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {
  private val className = "GetFlatServices"

  operator fun invoke(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): Flow<Resource<List<ServiceEntity>>> = flow {
    if (uid.isBlank() || addressId <= 0L) {
      emit(Resource.Success(emptyList()))
      return@flow
    }

    emit(Resource.Loading())

    val currentServiceType = when (service) {
      1.toByte() -> "voda"
      2.toByte() -> "teplo"
      3.toByte() -> "tbo"
      else -> "kv"
    }

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. КЕШ (Мобільні)
      if (!isWeb) {
          try {
              val local = ledgerCache.getServiceDetail(addressId, currentServiceType, year)
              if (local.isNotEmpty()) emit(Resource.Success(local))
          } catch (e: Exception) { }
      }

      // 2. МЕРЕЖА
      println("[$className]: Запит нарахувань для ID: $addressId, рік: $year")
      val response = repository.getFlatDetailService(uid, addressId, houseId, year, service, total)
      val remoteServices = response.services ?: emptyList()

      if (response.success == 1) {
        emit(Resource.Success(remoteServices))

        // 3. ОНОВЛЕННЯ КЕШУ
        if (!isWeb && remoteServices.isNotEmpty()) {
            try {
                ledgerCache.deleteServiceByApartment(addressId)
                ledgerCache.addService(remoteServices)
            } catch (e: Exception) { }
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
