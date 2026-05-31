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

data class PaymentState(
  val paymentList: List<PaymentEntity> = emptyList(),
  val isLoading: Boolean = false
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

  private val _paymentState = MutableStateFlow(PaymentState())
  val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

  private val _insertPaymentLoading = MutableStateFlow(false)
  val insertPaymentLoading: StateFlow<Boolean> = _insertPaymentLoading.asStateFlow()

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

      println("[$className.$methodName]: [REDIRECT] Перенаправлення детального пакета (Service: $service) на getDetailService")
      getDetailService(uid = uid, addressId = addressId,houseId = houseId, year = year, service = service, total = 1.toByte())



    ledgerService.getTotalDebtServices(
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
            println("[$className.$methodName]: [SUCCESS] Финансовый баланс успешно обновлен")
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

  fun getDetailService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ) {
    val methodName = "getDetailService"

    // Взводим флаг загрузки в главном стейте, чтобы правая панель планшета красиво показывала лоадер
    _totalDebtState.update { it.copy(isLoading = true) }

    ledgerService.getFlatServices(
      uid = uid,
      addressId = addressId,
      houseId = houseId,
      year = year,
      service = service,
      total = 0.toByte()
    ).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[$className.$methodName]: [SUCCESS] Отримано список послуг: ${result.data?.size} міс.")

          _detailState.update {
            it.copy(
              services = result.data ?: emptyList(),
              isLoading = false
            )
          }
          // Гасим лоадер в основном контейнере после успешного прилета 5 месяцев истории
          _totalDebtState.update { it.copy(isLoading = false) }
        }
        is Resource.Error -> {
          println("[$className.$methodName]: [ERROR] Сбой биллинга: ${result.message}")
          _detailState.update {
            it.copy(
              isLoading = false
            )
          }
          _totalDebtState.update {
            it.copy(
              isLoading = false,
              error = result.message ?: "Помилка білінгу"
            )
          }
        }
        is Resource.Loading -> {
          println("[$className.$methodName]: [LOADING] Запит деталізації нарахувань ЮКИС...")
          _detailState.update {
            it.copy(
              isLoading = true
            )
          }
        }
      }
    }.launchIn(screenModelScope)
  }


}



