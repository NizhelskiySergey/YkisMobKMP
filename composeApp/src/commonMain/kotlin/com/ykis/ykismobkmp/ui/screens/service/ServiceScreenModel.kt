package com.ykis.ykismobkmp.ui.screens.service

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.repository.services.ServiceParams
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.service.payment.list.PaymentState
import kotlinx.coroutines.flow.*

private const val tag = "ServiceScreenModel"

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


// Определение функциональных типов КМР Use Cases финансового блока
typealias GetFlatServices = (ServiceParams) -> Flow<Resource<List<ServiceEntity>>>
typealias GetTotalDebtServices = (ServiceParams) -> Flow<Resource<ServiceEntity>>
typealias GetPaymentList = (Long, String, String) -> Flow<Resource<List<PaymentEntity>>>

/**
 * [ServiceScreenModel] — Кроссплатформенная модель управления долгами, начислениями БТИ и платежами Xpay ЮКИС.
 * Полностью типизирована под Long и готова к выполнению на Mac Desktop (JVM) и мобильных ОС.
 */
class ServiceScreenModel(
  private val getFlatService: GetFlatServices,
  private val getTotalDebtServices: GetTotalDebtServices,
  private val getPaymentListRepo: GetPaymentList,
  logService: LogService
) : BaseScreenModel(logService) {

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

  /**
   * [getTotalServiceDebt] — Загрузка общего баланса лицевого счета (актуальный долг/переплата ГИОЦ Южное).
   */
  fun getTotalServiceDebt(params: ServiceParams) {
    val methodName = "getTotalServiceDebt"

    // ПРЕДОХРАНИТЕЛЬ: Если ID квартиры или UID невалидны - выходим
    if (params.addressId <= 0L || params.uid.isBlank()) {
      println("[$tag.$methodName]: [CANCEL] Невалідні параметри ЖКГ: ID=${params.addressId}")
      return
    }

    getTotalDebtServices(params).onEach { result ->
      _totalDebtState.update { currentState ->
        when (result) {
          is Resource.Success -> {
            println("[$tag.$methodName]: [SUCCESS] Баланс успешно обновлен")
            currentState.copy(totalDebt = result.data ?: ServiceEntity(), isLoading = false)
          }

          is Resource.Error -> {
            println("[$tag.$methodName]: [ERROR] ${result.message}")
            currentState.copy(isLoading = false, error = result.message ?: "Помилка завантаження")
          }

          is Resource.Loading -> {
            println("[$tag.$methodName]: [LOADING] Запит фінансового балансу...")
            currentState.copy(isLoading = true)
          }
        }
      }
    }.launchIn(screenModelScope) // ИСПРАВЛЕНО: launchIn переведен на screenModelScope Voyager
  }

  /**
   * [getDetailService] — Получение детализации начислений по видам услуг (квартплата, отопление, вода).
   */
  fun getDetailService(params: ServiceParams) {
    val methodName = "getDetailService"

    getFlatService(params).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[$tag.$methodName]: [SUCCESS] Отримано список послуг: ${result.data?.size}")
          _detailState.update {
            it.copy(services = result.data ?: emptyList(), isLoading = false)
          }
        }

        is Resource.Error -> {
          println("[$tag.$methodName]: [ERROR] ${result.message}")
          _detailState.update { it.copy(isLoading = false) }
          _totalDebtState.update {
            it.copy(isLoading = false, error = result.message ?: "Помилка білінгу")
          }
        }

        is Resource.Loading -> {
          println("[$tag.$methodName]: [LOADING] Запит деталізації...")
          _detailState.update { it.copy(isLoading = true) }
        }
      }
    }.launchIn(screenModelScope)
  }

  /**
   * [getPaymentList] — Архив совершенных абонентом оплат по годам.
   * ИСПРАВЛЕНО: addressId переведен на тип Long.
   */
  fun getPaymentList(addressId: Long, year: String, uid: String) {
    val methodName = "getPaymentList"

    getPaymentListRepo(addressId, year, uid).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[$tag.$methodName]: [SUCCESS] Отримано архів платіжок за рік $year: ${result.data?.size}")
          _paymentState.update {
            it.copy(paymentList = result.data ?: emptyList(), isLoading = false)
          }
        }

        is Resource.Error -> {
          println("[$tag.$methodName]: [ERROR] ${result.message}")
          _paymentState.update { it.copy(isLoading = false) }
          _totalDebtState.update {
            it.copy(isLoading = false, error = result.message ?: "Помилка мережі")
          }
        }

        is Resource.Loading -> {
          println("[$tag.$methodName]: [LOADING] Архів оплат (Рік: $year)...")
          _paymentState.update { it.copy(isLoading = true) }
        }
      }
    }.launchIn(screenModelScope)
  }
}

  /**
   * [insertPayment] — Генерация защищенной ссылки на инвойс Xpay для оплаты в браузере или WebView.
   */


