package com.ykis.ykismobkmp.domain.repository.ledger

import com.ykis.ykismobkmp.data.remote.ledger.LedgerRemoteRepository
import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

/**
 * [LedgerRepositoryImpl] — Реалізація репозиторію нарахувань.
 * УНІФІКОВАНО: Обробка помилок через Res.string.
 */
class LedgerRepositoryImpl(
  private val remote: LedgerRemoteRepository
) : LedgerRepository {

  private val className = "LedgerRepositoryImpl"

  override suspend fun getFlatDetailService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    return try {
      remote.getFlatDetailServices(uid, addressId, houseId, year, service, total)
    } catch (ex: Exception) {
      println("[YkisLogKMP.$className.getFlatDetailService]: [ERROR] ${ex.message}")
      GetServiceResponse(
        success = 0,
        message = getString(Res.string.error_network_request_failed)
      )
    }
  }

  override suspend fun getTotalDebtService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    return try {
      remote.getTotalDebtService(uid, addressId, houseId, year, service, total)
    } catch (ex: Exception) {
      println("[YkisLogKMP.$className.getTotalDebtService]: [ERROR] ${ex.message}")
      GetServiceResponse(
        success = 0,
        message = getString(Res.string.error_network_request_failed)
      )
    }
  }
}
