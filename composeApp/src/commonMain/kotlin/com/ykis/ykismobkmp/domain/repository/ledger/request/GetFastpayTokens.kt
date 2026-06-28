package com.ykis.ykismobkmp.domain.repository.ledger.request

import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.FastpayEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetFastpayTokens] — Use Case отримання токенів швидкої оплати Privat24.
 */
class GetFastpayTokens(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {
  private val className = "GetFastpayTokens"

  operator fun invoke(): Flow<Resource<List<FastpayEntity>>> = flow {
    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)

    try {
      // 1. КЕШ (якщо не Web)
      if (!isWeb) {
          val local = ledgerCache.getFastpayTokens()
          if (local.isNotEmpty()) emit(Resource.Success(local))
      }

      // 2. МЕРЕЖА
      val response = repository.getFastpayTokens()
      val remoteTokens = response.tokens

      if (response.success == 1) {
        emit(Resource.Success(remoteTokens))

        // 3. ОНОВЛЕННЯ КЕШУ
        if (!isWeb && remoteTokens.isNotEmpty()) {
            ledgerCache.insertFastpayTokens(remoteTokens)
        }
      } else if (!isWeb) {
          // Якщо мережа впала, але кеш був порожній - видаємо помилку
          val local = ledgerCache.getFastpayTokens()
          if (local.isEmpty()) emit(Resource.Error(response.message))
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      if (!isWeb) {
          val local = ledgerCache.getFastpayTokens()
          if (local.isNotEmpty()) emit(Resource.Success(local))
          else emit(Resource.Error("Помилка завантаження токенів"))
      } else {
          emit(Resource.Error("Помилка мережі"))
      }
    }
  }.flowOn(Dispatchers.Default)
}
