package com.ykis.ykismobkmp.ui.screens.ledger

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LedgerScreenModel(
  private val ledgerService: LedgerService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "LedgerScreenModel"

  private var detailJob: kotlinx.coroutines.Job? = null
  private var totalDebtJob: kotlinx.coroutines.Job? = null
  private var singleTokenJob: kotlinx.coroutines.Job? = null

  fun getFastpayTokenByOsbb(uid: String, osbbId: Long) {
      if (uid.isBlank() || osbbId == 0L) {
          println("[YkisLogKMP.LedgerScreenModel]: getFastpayTokenByOsbb скасовано (пусті параметри)")
          return
      }
      singleTokenJob?.cancel()
      singleTokenJob = screenModelScope.launch {
          println("[YkisLogKMP.LedgerScreenModel]: Початок завантаження токена для OSBB $osbbId...")
          ledgerService.getFastpayTokenByOsbb(uid, osbbId).collect { result ->
              when (result) {
                  is Resource.Success -> {
                      val newToken = result.data
                      println("[YkisLogKMP.LedgerScreenModel]: Токен для $osbbId завантажено УСПІШНО. Значення: ${newToken?.token?.take(5)}...")
                      if (newToken != null) {
                          _uiState.update { currentState ->
                              val currentTokens = currentState.fastpayTokens.toMutableList()
                              val existingIndex = currentTokens.indexOfFirst { it.osbbId == osbbId }
                              if (existingIndex != -1) {
                                  currentTokens[existingIndex] = newToken
                              } else {
                                  currentTokens.add(newToken)
                              }
                              currentState.copy(fastpayTokens = currentTokens)
                          }
                      }
                  }
                  is Resource.Error -> {
                      println("[YkisLogKMP.LedgerScreenModel]: ПОМИЛКА завантаження токена для $osbbId: ${result.message}")
                  }
                  is Resource.Loading -> {
                      println("[YkisLogKMP.LedgerScreenModel]: Завантаження токена...")
                  }
              }
          }
      }
  }

  /**
   * [setContentDetail] — Установка выбранной службы для детализации начислений.
   */
  fun setContentDetail(contentDetail: ContentDetail, uid: String? = null, osmdId: Long? = null) {
    _uiState.update {
      it.copy(serviceDetail = contentDetail, showDetail = true)
    }

    // Автоматичне завантаження токена при виборі служби (Тільки якщо є UID)
    if (!uid.isNullOrBlank() && contentDetail != ContentDetail.UNKNOWN) {
        val targetOsbbId = when (contentDetail) {
            ContentDetail.WATER_SERVICE   -> com.ykis.ykismobkmp.core.Constants.WATER_SERVICE_ID
            ContentDetail.WARM_SERVICE    -> com.ykis.ykismobkmp.core.Constants.WARM_SERVICE_ID
            ContentDetail.GARBAGE_SERVICE -> com.ykis.ykismobkmp.core.Constants.GARBAGE_SERVICE_ID
            else -> osmdId ?: 0L
        }
        
        if (targetOsbbId != 0L && _uiState.value.fastpayTokens.none { it.osbbId == targetOsbbId }) {
            getFastpayTokenByOsbb(uid, targetOsbbId)
        }
    }
  }

  /**
   * [closeContentDetail] — Возврат к общему списку задолженностей.
   */
  fun closeContentDetail() {
    _uiState.update {
      it.copy(showDetail = false)
    }
  }

  /**
   * [getTotalServiceDebt] — Загрузка сводного баланса по всем коммунальным службам.
   */
  fun getTotalServiceDebt(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ) {
    val methodName = "getTotalServiceDebt"
    if ((addressId <= 0L) || uid.isBlank()) {
      println("[YkisLogKMP.$className.$methodName]: [CANCEL] Невалидные параметры: ID=$addressId")
      return
    }

    totalDebtJob?.cancel()
    
    totalDebtJob = ledgerService.getTotalDebtServices(
      uid = uid,
      addressId = addressId,
      houseId = houseId,
      year = year,
      service = service,
      total = total
    ).onEach { result ->
      _uiState.update { currentState ->
        when (result) {
          is Resource.Success -> {
            println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Финансовый баланс обновлен.")
            currentState.copy(
              totalDebt = result.data ?: ServiceEntity(),
              isLoading = false
            )
          }
          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой загрузки баланса: ${result.message}")
            currentState.copy(
              isLoading = false,
              error = result.message ?: "Ошибка загрузки"
            )
          }
          is Resource.Loading -> {
            println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос баланса ГИОЦ г. Южный...")
            currentState.copy(
              isLoading = true
            )
          }
        }
      }
    }.launchIn(screenModelScope)
  }

  fun getDetailService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte
  ) {
    val methodName = "getDetailService"

    detailJob?.cancel()

    _uiState.update { it.copy(isLoading = true) }

    detailJob = screenModelScope.launch {
      ledgerService.getFlatServices(
        uid = uid,
        addressId = addressId,
        houseId = houseId,
        year = year,
        service = service,
        total = 0.toByte()
      ).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Получена история начислений: ${result.data?.size} мес.")
              currentState.copy(
                monthlyServices = result.data ?: emptyList(),
                isLoading = false,
                error = ""
              )
            }
            is Resource.Error -> {
              val errorMessage = result.message ?: "Ошибка биллинга"
              if (errorMessage.contains("Success", ignoreCase = true) || errorMessage.contains("Успешно", ignoreCase = true)) {
                println("[YkisLogKMP.$className.$methodName]: [FIXED_GATE] Восстановление данных из кэша.")
                currentState.copy(
                  monthlyServices = result.data ?: currentState.monthlyServices,
                  isLoading = false,
                  error = ""
                )
              } else {
                println("[YkisLogKMP.$className.$methodName]: [REAL_ERROR] Сбой биллинга: $errorMessage")
                currentState.copy(
                  isLoading = false,
                  error = errorMessage
                )
              }
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос детализации ЮКИС...")
              currentState.copy(isLoading = true)
            }
          }
        }
      }
    }
  }
}
