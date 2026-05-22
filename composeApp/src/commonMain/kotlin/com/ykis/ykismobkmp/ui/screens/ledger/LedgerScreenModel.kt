package com.ykis.ykismobkmp.ui.screens.ledger

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.ledger.payment.list.PaymentState
import kotlinx.coroutines.flow.*



// Заглушки доменных сущностей и КМР стейтов финансового учета

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


/**
 * [LedgerScreenModel] — Кроссплатформенная модель управления долгами, начислениями БТИ и платежами Xpay ЮКИС.
 * Полностью типизирована под Long и готова к выполнению на Mac Desktop (JVM) и мобильных ОС.
 */
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
    year: String,
    service: Byte,
    total: Byte
  ) {
    val methodName = "getTotalServiceDebt"

    // ПРЕДОХРАНИТЕЛЬ: Если ID квартиры или UID невалидны — мгновенно выходим, не спамя сеть
    if (addressId <= 0L || uid.isBlank()) {
      println("[$className.$methodName]: [CANCEL] Невалідні параметри ЖКГ: ID=$addressId")
      return
    }

    // Вызываем сценарий биллинга через наш запечатанный LedgerService на прямых параметрах
    ledgerService.getTotalDebtServices(
      uid = uid,
      addressId = addressId,
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
    }.launchIn(screenModelScope) // Выполняем подписку внутри жизненного цикла ScreenModel скоупа Voyager
  }
  fun getDetailService(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ) {
    val methodName = "getDetailService"

    // Вызываем сценарий детализации биллинга через наш запечатанный LedgerService на прямых параметрах
    ledgerService.getFlatServices(
      uid = uid,
      addressId = addressId,
      year = year,
      service = service,
      total = total
    ).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[$className.$methodName]: [SUCCESS] Отримано список послуг: ${result.data?.size}")
          _detailState.update {
            it.copy(
              services = result.data ?: emptyList(),
              isLoading = false
            )
          }
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
    }.launchIn(screenModelScope) // Выполняем подписку внутри жизненного цикла ScreenModel скоупа Voyager
  }
  fun getPaymentList(addressId: Long, year: String, uid: String) {
    val methodName = "getPaymentList"

    // Вызываем сценарий получения архива оплат через наш запечатанный LedgerService
    ledgerService.getPaymentList(
      uid = uid,
      addressId = addressId,
      year = year
    ).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[$className.$methodName]: [SUCCESS] Отримано архів платіжок за рік $year: ${result.data?.size}")
          _paymentState.update {
            it.copy(
              paymentList = result.data ?: emptyList(),
              isLoading = false
            )
          }
        }

        is Resource.Error -> {
          println("[$className.$methodName]: [ERROR] Сбой загрузки архива оплат: ${result.message}")
          _paymentState.update {
            it.copy(
              isLoading = false
            )
          }
          _totalDebtState.update {
            it.copy(
              isLoading = false,
              error = result.message ?: "Помилка мережі"
            )
          }
        }

        is Resource.Loading -> {
          println("[$className.$methodName]: [LOADING] Архів оплат (Рік: $year) з розрахункового центру...")
          _paymentState.update {
            it.copy(
              isLoading = true
            )
          }
        }
      }
    }.launchIn(screenModelScope) // Выполняем подписку внутри жизненного цикла ScreenModel скоупа Voyager
  }

}

  /**
   * [insertPayment] — Генерация защищенной ссылки на инвойс Xpay для оплаты в браузере или WebView.
   */


