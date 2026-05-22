package com.ykis.ykismobkmp.ui.screens.meter

import com.ykis.ykismobkmp.core.utils.Resource
import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val tag = "MeterScreenModel"

/**
 * [MeterScreenModel] — Кроссплатформенная модель управления списками счетчиков тепла и воды ЮКИС.
 */
class MeterScreenModel(
  private val meterService: MeterService,
  logService: LogService
) : BaseScreenModel(logService)
{
  private val className = "YkisLog MeterScreenModel"
  private val _waterMeterState = MutableStateFlow(WaterMeterState())
  val waterMeterState: StateFlow<WaterMeterState> = _waterMeterState.asStateFlow()

  private val _heatMeterState = MutableStateFlow(HeatMeterState())
  val heatMeterState: StateFlow<HeatMeterState> = _heatMeterState.asStateFlow()

  private val _showDetail = MutableStateFlow(false)
  val showDetail: StateFlow<Boolean> = _showDetail.asStateFlow()

  private val _contentDetail = MutableStateFlow(ContentDetail.WATER_METER)
  val contentDetail: StateFlow<ContentDetail> = _contentDetail.asStateFlow()

  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  fun onTabSelect(index: Int) {
    _selectedTab.value = index
  }

  /**
   * [getWaterMeterList] — Получение списка приборов учета холодного/горячего водоснабжения.
   */
  /**
   * [getWaterMeterList] — Запрос и реактивное обновление списка водомеров жильца с поддержкой КМР-кэширования.
   */
  fun getWaterMeterList(uid: String, addressId: Long) {
    val methodName = "getWaterMeterList"

    screenModelScope.launch {
      // Запускаем сбор реактивного потока из нашего запечатанного MeterService
      meterService.getWaterMeterList(uid, addressId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val metersList = result.data ?: emptyList()
              println("[$className.$methodName]: [SUCCESS] Успешно выведено ${metersList.size} водомеров")
              currentState.copy(
                waterMeterList = metersList,
                isMetersLoading = false
              )
            }

            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой загрузки водомеров: ${result.message}")
              currentState.copy(
                error = result.message ?: "Помилка завантаження",
                isMetersLoading = false
              )
            }

            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Запит списку приладів обліку води ГІОЦ...")
              currentState.copy(
                isMetersLoading = true
              )
            }
          }
        }
      }
    }
  }


  /**
   * [getHeatMeterList] — Получение списка теплосчетчиков биллинга г. Южного.
   */
  /**
   * [getHeatMeterList] — Запрос и реактивное обновление списка теплосчетчиков жильца с поддержкой КМР-кэширования.
   * ИСПРАВЛЕНО НАМЕРТВО: Вызовы переведены на сбор реактивного Flow потока из MeterService, выровнены стейты Resource!
   */
  fun getHeatMeterList(uid: String, addressId: Long) {
    val methodName = "getHeatMeterList"

    screenModelScope.launch {
      // Запускаем сбор реактивного потока из нашего запечатанного MeterService
      meterService.getHeatMeterList(uid, addressId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val metersList = result.data ?: emptyList()
              println("[$className.$methodName]: [SUCCESS] Успешно выведено ${metersList.size} теплосчетчиков")
              currentState.copy(
                heatMeterList = metersList,
                isMetersLoading = false
              )
            }

            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой загрузки теплосчетчиков: ${result.message}")
              currentState.copy(
                error = result.message ?: "Помилка завантаження",
                isMetersLoading = false
              )
            }

            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Запит списку приладів обліку тепла ГІОЦ...")
              currentState.copy(
                isMetersLoading = true
              )
            }
          }
        }
      }
    }
  }


  fun setWaterMeterDetail(waterMeterEntity: WaterMeterEntity) {
    _waterMeterState.update { it.copy(selectedWaterMeter = waterMeterEntity) }
    _contentDetail.value = ContentDetail.WATER_METER
    _showDetail.value = true
  }

  fun setHeatMeterDetail(heatMeterEntity: HeatMeterEntity) {
    _heatMeterState.update { it.copy(selectedHeatMeter = heatMeterEntity) }
    _contentDetail.value = ContentDetail.HEAT_METER
    _showDetail.value = true
  }

  fun closeContentDetail() {
    _showDetail.value = false
  }

  /**
   * [getWaterReadings] — Архив истории переданных кубометров.
   */
  // ====================================================================
  // --- ЛОГИКА ИСТОРИИ И ПОСЛЕДНИХ ПОКАЗАНИЙ ВОДЫ ----------------------
  // ====================================================================

  fun getWaterReadings(uid: String, vodomerId: Long) {
    val methodName = "getWaterReadings"
    screenModelScope.launch {
      meterService.getWaterReadings(uid, vodomerId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readingsList = result.data ?: emptyList()
              println("[$className.$methodName]: [SUCCESS] Отримано ${readingsList.size} показань води")
              currentState.copy(
                waterReadings = readingsList,
                isReadingsLoading = false
              )
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой загрузки истории воды: ${result.message}")
              SnackbarManager.showMessage("Помилка завантаження показань")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Запит історії показань водоміра...")
              currentState.copy(isReadingsLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [getLastWaterReading] — Чтение последней квитанции водомера.
   */
  fun getLastWaterReading(uid: String, vodomerId: Long) {
    val methodName = "getLastWaterReading"
    screenModelScope.launch {
      meterService.getLastWaterReading(uid, vodomerId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[$className.$methodName]: [SUCCESS] Останнє показання води отримано")
              currentState.copy(
                lastWaterReading = result.data,
                isLastReadingLoading = false
              )
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой получения последнего показания воды: ${result.message}")
              SnackbarManager.showMessage("Помилка отримання останнього показання")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Запит останнього показання водоміра...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  // ====================================================================
  // --- ЛОГИКА ИСТОРИИ И ПОСЛЕДНИХ ПОКАЗАНИЙ ТЕПЛА ---------------------
  // ====================================================================

  /**
   * [getHeatReadings] — Архив истории гигакалорий теплосети г. Южного.
   */
  fun getHeatReadings(uid: String, teplomerId: Long) {
    val methodName = "getHeatReadings"
    screenModelScope.launch {
      meterService.getHeatReadings(uid, teplomerId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readingsList = result.data ?: emptyList()
              println("[$className.$methodName]: [SUCCESS] Отримано ${readingsList.size} показань тепла")
              currentState.copy(
                heatReadings = readingsList,
                isReadingsLoading = false
              )
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой загрузки истории тепла: ${result.message}")
              SnackbarManager.showMessage("Помилка завантаження показань тепла")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Запит історії показань тепломіра...")
              currentState.copy(isReadingsLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [getLastHeatReading] — Чтение последней квитанции тепломера.
   */
  fun getLastHeatReading(uid: String, teplomerId: Long) {
    val methodName = "getLastHeatReading"
    screenModelScope.launch {
      meterService.getLastHeatReading(uid, teplomerId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[$className.$methodName]: [SUCCESS] Останнє показання тепла отримано")
              currentState.copy(
                lastHeatReading = result.data,
                isReadingsLoading = false
              )
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой получения последнего показания тепла: ${result.message}")
              SnackbarManager.showMessage("Помилка отримання останнього показання тепла")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Запит останнього показання тепломіра...")
              currentState.copy(isReadingsLoading = true)
            }
          }
        }
      }
    }
  }


  /**
   * [addWaterReading] — Отправка новых кубометров воды.
   */
  // ====================================================================
  // --- ОПЕРАЦИИ ЗАПИСИ И УДАЛЕНИЯ ПОКАЗАНИЙ ВОДЫ ---------------------
  // ====================================================================

  fun addWaterReading(uid: String, newValue: Long, currentValue: Long, vodomerId: Long) {
    val methodName = "addWaterReading"
    screenModelScope.launch {
      // ИСПРАВЛЕНО НАМЕРТВО: Прямой проброс базовых Long-параметров без упаковки в MeterReadingsParams!
      meterService.addWaterReading(
        uid = uid,
        vodomerId = vodomerId,
        currentValue = currentValue,
        newValue = newValue
      ).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[$className.$methodName]: [SUCCESS] Показання води успішно додані")
              SnackbarManager.showMessage("Показання успішно додані")
              getLastWaterReading(uid, vodomerId) // Каскадный автоматический перезапрос
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой добавления показаний воды: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка додавання")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Відправка нових кубометрів води в розрахунковий центр...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  fun deleteLastWaterReading(uid: String, vodomerId: Long, readingId: Long) {
    val methodName = "deleteLastWaterReading"
    screenModelScope.launch {
      meterService.deleteLastWaterReading(uid, readingId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[$className.$methodName]: [SUCCESS] Показання води успішно видалені")
              SnackbarManager.showMessage("Показання успішно видалені")
              getLastWaterReading(uid, vodomerId)
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой удаления показания воды: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка видалення")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Анулювання помилкового показання води в СУБД...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  // ====================================================================
  // --- ОПЕРАЦИИ ЗАПИСИ И УДАЛЕНИЯ ПОКАЗАНИЙ ТЕПЛА --------------------
  // ====================================================================

  fun addHeatReading(uid: String, teplomerId: Long, currentValue: Double, newValue: Double) {
    val methodName = "addHeatReading"
    screenModelScope.launch {
      // ИСПРАВЛЕНО НАМЕРТВО: Прямой проброс базовых Double-параметров без упаковки в MeterReadingsParams!
      meterService.addHeatReading(
        uid = uid,
        teplomerId = teplomerId,
        currentValue = currentValue,
        newValue = newValue
      ).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[$className.$methodName]: [SUCCESS] Показання тепла успішно додані")
              SnackbarManager.showMessage("Показання успішно додані")
              getLastHeatReading(uid, teplomerId)
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой добавления показаний тепла: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка додавання")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Відправка нових гігакалорій тепла в розрахунковий центр...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  fun deleteLastHeatReading(readingId: Long, teplomerId: Long, uid: String) {
    val methodName = "deleteLastHeatReading"
    screenModelScope.launch {
      // ИСПРАВЛЕНО НАМЕРТВО: Вызов перенаправлен на монолитный комбайн meterService!
      meterService.deleteLastHeatReading(uid, readingId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[$className.$methodName]: [SUCCESS] Показання тепла успішно видалені")
              SnackbarManager.showMessage("Показання успішно видалені")
              getLastHeatReading(uid, teplomerId)
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Error -> {
              println("[$className.$methodName]: [ERROR] Сбой удаления показания тепла: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка видалення")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[$className.$methodName]: [LOADING] Анулювання помилкового показання тепла в СУБД...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }


  fun onNewWaterReadingChange(newValue: String) {
    _waterMeterState.update { it.copy(newWaterReading = newValue) }
  }

  fun onNewHeatReadingChange(newValue: String) {
    _heatMeterState.update { it.copy(newHeatReading = newValue) }
  }

  fun setContentDetail(contentDetail: ContentDetail) {
    _contentDetail.value = contentDetail
  }
}
