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
 * [GetTotalDebtServices] — Use Case отримання сумарної заборгованості.
 * УНІФІКОВАНО: Прямий запит для Web, кешування для мобільних.
 */
class GetTotalDebtServices(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {
  private val className = "GetTotalDebtServices"

  operator fun invoke(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): Flow<Resource<ServiceEntity>> = flow {
    if (uid.isBlank() || addressId <= 0L) {
      emit(Resource.Success(ServiceEntity()))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. МЕРЕЖА
      println("[YkisLogKMP.$className]: Запит балансу для ID: $addressId")
      val response = repository.getTotalDebtService(uid, addressId, houseId, year, service, total)
      val services = response.services ?: emptyList()

      if (response.success == 1 && services.isNotEmpty()) {
        val serviceData = services[0]
        emit(Resource.Success(serviceData))

        // 2. ОНОВЛЕННЯ КЕШУ
        if (!isWeb) {
            try { ledgerCache.addService(services) } catch (e: Exception) { }
        }
      } else {
        // FALLBACK КЕШ
        if (!isWeb) {
            val local = try { ledgerCache.getTotalDebt(addressId) } catch (e: Exception) { null }
            if (local != null) emit(Resource.Success(local)) else emit(Resource.Success(ServiceEntity()))
        } else {
            emit(Resource.Success(ServiceEntity()))
        }
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Success(ServiceEntity()))
    }
  }.flowOn(Dispatchers.Default)
}
