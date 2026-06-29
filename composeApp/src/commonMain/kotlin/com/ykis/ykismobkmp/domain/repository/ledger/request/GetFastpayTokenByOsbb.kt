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
 * [GetFastpayTokenByOsbb] — Use Case отримання конкретного токена оплати за osbbId.
 */
class GetFastpayTokenByOsbb(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {
  private val className = "GetFastpayTokenByOsbb"

  operator fun invoke(uid: String, osbbId: Long): Flow<Resource<FastpayEntity>> = flow {
    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)

    try {
      // 1. КЕШ (якщо не Web)
      var cachedToken: FastpayEntity? = null
      if (!isWeb) {
          cachedToken = ledgerCache.getFastpayTokens().find { it.osbbId == osbbId }
          if (cachedToken != null) emit(Resource.Success(cachedToken))
      }

      // 2. МЕРЕЖА
      val response = repository.getFastpayTokenByOsbb(uid, osbbId)
      val remoteTokens = response.tokens
      println("[$className]: Відповідь сервера отримано. Кількість токенів у масиві: ${remoteTokens.size}")
      
      val remoteToken = remoteTokens.firstOrNull()

      if (response.success == 1 && remoteToken != null) {
        emit(Resource.Success(remoteToken))

        // 3. ОНОВЛЕННЯ КЕШУ
        if (!isWeb) {
            ledgerCache.insertFastpayTokens(listOf(remoteToken))
        }
      } else {
          // Якщо мережа повернула пустий список, але в нас є КЕШ - не видаємо помилку
          if (cachedToken == null) {
              emit(Resource.Error(response.message.ifBlank { "Токен не знайдено" }))
          }
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Error("Помилка мережі"))
    }
  }.flowOn(Dispatchers.Default)
}
