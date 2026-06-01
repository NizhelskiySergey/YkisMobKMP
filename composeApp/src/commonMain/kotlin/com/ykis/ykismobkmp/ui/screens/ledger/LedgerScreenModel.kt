package com.ykis.ykismobkmp.ui.screens.ledger

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
data class ServiceState(
  val services: List<ServiceEntity> = emptyList(),
  val isLoading: Boolean = false
)

data class TotalDebtState(
  val totalDebt: ServiceEntity = ServiceEntity(),
  val isLoading: Boolean = false,
  val showDetail: Boolean = false,
  val serviceDetail: ContentDetail = ContentDetail.UNKNOWN,
  val error: String = ""
)

class LedgerScreenModel(
  private val ledgerService: LedgerService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "GetFlatServices"

  private val _detailState = MutableStateFlow(ServiceState())
  val detailState: StateFlow<ServiceState> = _detailState.asStateFlow()

  private val _totalDebtState = MutableStateFlow(TotalDebtState())
  val totalDebtState: StateFlow<TotalDebtState> = _totalDebtState.asStateFlow()

  private var detailJob: kotlinx.coroutines.Job? = null
  private var totalDebtJob: kotlinx.coroutines.Job? = null

  fun setContentDetail(contentDetail: ContentDetail) {
    _totalDebtState.update {
      it.copy(serviceDetail = contentDetail, showDetail = true)
    }
  }

  fun closeContentDetail() {
    _totalDebtState.update {
      it.copy(showDetail = false)
    }
  }
  fun getTotalServiceDebt(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ) {
    val methodName = "getTotalServiceDebt"
    if (addressId <= 0L || uid.isBlank()) {
      println("[$className.$methodName]: [CANCEL] Невалідні параметри ЖКГ: ID=$addressId")
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
      _totalDebtState.update { currentState ->
        when (result) {
          is Resource.Success -> {
            println("[$className.$methodName]: [SUCCESS] Фінансовий зведений баланс успішно оновлено")
            currentState.copy(
              totalDebt = result.data ?: ServiceEntity(),
              isLoading = false
            )
          }
          is Resource.Error -> {
            println("[$className.$methodName]: [ERROR] Сбой загрузки баланса: ${result.message}")
            currentState.copy(
              isLoading = false,
              error = result.message ?: "Помилка завантаження"
            )
          }
          is Resource.Loading -> {
            println("[$className.$methodName]: [LOADING] Запит фінансового балансу ГІОЦ г. Южный...")
            currentState.copy(
              isLoading = true
            )
          }
        }
      }
    }.launchIn(screenModelScope)
  }

  /**
   * [getDetailService] — Атомарный метод загрузки ИСТОРИИ начислений по конкретной службе (total = 0).
   */
  fun getDetailService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ) {
    val methodName = "getDetailService"

    detailJob?.cancel()

    _totalDebtState.update { it.copy(isLoading = true) }

    detailJob = screenModelScope.launch {
      ledgerService.getFlatServices(
        uid = uid,
        addressId = addressId,
        houseId = houseId,
        year = year,
        service = service,
        total = 0.toByte()
      ).collect { result ->
        when (result) {
          is Resource.Success -> {
            println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Отримано список послуг: ${result.data?.size} міс.")

            _detailState.update {
              it.copy(
                services = result.data ?: emptyList(),
                isLoading = false
              )
            }
            _totalDebtState.update { it.copy(isLoading = false, error = "") }
          }
          is Resource.Error -> {
            val errorMessage = result.message ?: "Помилка білінгу"
            if (errorMessage.contains("Success", ignoreCase = true) || errorMessage.contains("Успешно", ignoreCase = true)) {
              println("[YkisLogKMP.$className.$methodName]: [FIXED_GATE] Перехват ложной ошибки. Восстановление кадра из SQLite.")

              _detailState.update {
                it.copy(
                  services = result.data ?: it.services,
                  isLoading = false
                )
              }
              _totalDebtState.update { it.copy(isLoading = false, error = "") }
            } else {
              println("[YkisLogKMP.$className.$methodName]: [REAL_ERROR] Сбой білінгу: $errorMessage")
              _detailState.update { it.copy(isLoading = false) }
              _totalDebtState.update {
                it.copy(
                  isLoading = false,
                  error = errorMessage
                )
              }
            }
          }
          is Resource.Loading -> {
            println("[YkisLogKMP.$className.$methodName]: [LOADING] Запит деталізації нарахувань ЮКИС...")
            _detailState.update { it.copy(isLoading = true) }
          }
        }
      }
    }
  }
}
