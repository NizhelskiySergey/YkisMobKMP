package com.ykis.ykismobkmp.ui.screens.meter

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.MeterReadingsParams
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
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
  private val waterMeterRepository: WaterMeterRepository,
  private val heatMeterRepository: HeatMeterRepository,
  logService: LogService
) : BaseScreenModel(logService)
{

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
  fun getWaterMeterList(uid: String, addressId: Long) {
    val methodName = "getWaterMeterList"
    screenModelScope.launch {
      _waterMeterState.update {
        it.copy(waterMeterList = emptyList(), isMetersLoading = true)
      }
      try {
        val response = waterMeterRepository.getWaterMeterList(uid, addressId)
        val metersList = response.waterMeters ?: emptyList()
        _waterMeterState.update {
          it.copy(waterMeterList = metersList, isMetersLoading = false)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: [CRITICAL_ERROR] ${e.message}")
        _waterMeterState.update {
          it.copy(error = e.message ?: "Помилка мережі", isMetersLoading = false)
        }
      }
    }
  }

  /**
   * [getHeatMeterList] — Получение списка теплосчетчиков биллинга г. Южного.
   */
  fun getHeatMeterList(uid: String, addressId: Long) {
    val methodName = "getHeatMeterList"
    screenModelScope.launch {
      _heatMeterState.update {
        it.copy(heatMeterList = emptyList(), isMetersLoading = true)
      }
      try {
        val response = heatMeterRepository.getHeatMeterList(uid, addressId)
        _heatMeterState.update {
          it.copy(heatMeterList = response.heatMeters ?: emptyList(), isMetersLoading = false)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching heat meters: ${e.message}")
        _heatMeterState.update {
          it.copy(error = e.message ?: "Unexpected error!", isMetersLoading = false)
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
  fun getWaterReadings(uid: String, vodomerId: Long) {
    val methodName = "getWaterReadings"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isReadingsLoading = true) }
      try {
        val response = withContext(Dispatchers.Default) {
          waterMeterRepository.getWaterReadings(uid, vodomerId)
        }
        _waterMeterState.update { state ->
          state.copy(waterReadings = response.waterReadings ?: emptyList(), isReadingsLoading = false)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching readings: ${e.message}")
        SnackbarManager.showMessage("Помилка завантаження показань")
        _waterMeterState.update { it.copy(isReadingsLoading = false) }
      }
    }
  }

  /**
   * [getLastWaterReading] — Чтение последней квитанции водомера.
   */
  fun getLastWaterReading(uid: String, vodomerId: Long) {
    val methodName = "getLastWaterReading"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isLastReadingLoading = true) }
      try {
        val response = waterMeterRepository.getLastWaterReading(uid, vodomerId)
        _waterMeterState.update { state ->
          // ИСПРАВЛЕНО: Прямое присвоение nullable результата без принудительной подстановки non-null заглушки
          state.copy(lastWaterReading = response.waterReading, isLastReadingLoading = false)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching last reading: ${e.message}")
        SnackbarManager.showMessage("Помилка отримання останнього показання")
        _waterMeterState.update { it.copy(isLastReadingLoading = false) }
      }
    }
  }

  /**
   * [getHeatReadings] — Архив истории гигакалорий теплосети г. Южного.
   */
  fun getHeatReadings(uid: String, teplomerId: Long) {
    val methodName = "getHeatReadings"
    screenModelScope.launch {
      _heatMeterState.update { it.copy(isReadingsLoading = true) }
      try {
        val response = heatMeterRepository.getHeatReadings(uid, teplomerId)
        _heatMeterState.update { state ->
          state.copy(heatReadings = response.heatReadings ?: emptyList(), isReadingsLoading = false)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching heat readings: ${e.message}")
        SnackbarManager.showMessage("Помилка завантаження показань тепла")
        _heatMeterState.update { it.copy(isReadingsLoading = false) }
      }
    }
  }

  /**
   * [getLastHeatReading] — Чтение последней квитанции тепломера.
   */
  fun getLastHeatReading(uid: String, teplomerId: Long) {
    val methodName = "getLastHeatReading"
    screenModelScope.launch {
      _heatMeterState.update { it.copy(isReadingsLoading = true) }
      try {
        val response = heatMeterRepository.getLastHeatReading(uid, teplomerId)
        _heatMeterState.update { state ->
          // ИСПРАВЛЕНО: Прямое присвоение зануляемого результата для корректной работы safeLastReading в верстке
          state.copy(lastHeatReading = response.heatReading, isReadingsLoading = false)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching last heat reading: ${e.message}")
        SnackbarManager.showMessage("Помилка отримання останнього показання тепла")
        _heatMeterState.update { it.copy(isReadingsLoading = false) }
      }
    }
  }

  /**
   * [addWaterReading] — Отправка новых кубометров воды.
   */
  fun addWaterReading(uid: String, newValue: Long, currentValue: Double, vodomerId: Long) {
    val methodName = "addWaterReading"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isLastReadingLoading = true) }
      try {
        // Выполняем безопасный кастинг .toDouble() перед отправкой параметров в Ktor-клиент
        val params = MeterReadingsParams(
          uid = uid,
          newValue = newValue.toDouble(),
          currentValue = currentValue,
          meterId = vodomerId
        )
        val response = waterMeterRepository.addWaterReading(params)

        if (response.success == 1) {
          SnackbarManager.showMessage("Показання додані")
          getLastWaterReading(uid, vodomerId) // Каскадный автоматический перезапрос
        } else {
          SnackbarManager.showMessage(response.message ?: "Помилка додавання")
        }
        _waterMeterState.update { it.copy(isLastReadingLoading = false) }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Add reading error: ${e.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером")
        _waterMeterState.update { it.copy(isLastReadingLoading = false) }
      }
    }
  }

  fun deleteLastWaterReading(uid: String, vodomerId: Long, readingId: Long) {
    val methodName = "deleteLastWaterReading"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isLastReadingLoading = true) }
      try {
        val response = waterMeterRepository.deleteLastWaterReading(uid, readingId)
        if (response.success == 1) {
          SnackbarManager.showMessage("Показання видалені")
          getLastWaterReading(uid, vodomerId)
        } else {
          SnackbarManager.showMessage(response.message ?: "Помилка видалення")
        }
        _waterMeterState.update { it.copy(isLastReadingLoading = false) }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Delete error: ${e.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером")
        _waterMeterState.update { it.copy(isLastReadingLoading = false) }
      }
    }
  }

  fun deleteLastHeatReading(readingId: Long, teplomerId: Long, uid: String) {
    val methodName = "deleteLastHeatReading"
    screenModelScope.launch {
      _heatMeterState.update { it.copy(isLastReadingLoading = true) }
      try {
        val response = heatMeterRepository.deleteLastHeatReading(uid, readingId)
        if (response.success == 1) {
          SnackbarManager.showMessage("Показання видалені")
          getLastHeatReading(uid, teplomerId)
        } else {
          SnackbarManager.showMessage(response.message ?: "Помилка видалення")
        }
        _heatMeterState.update { it.copy(isLastReadingLoading = false) }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Delete heat error: ${e.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером")
        _heatMeterState.update { it.copy(isLastReadingLoading = false) }
      }
    }
  }

  fun addHeatReading(uid: String, teplomerId: Long, currentValue: Double, newValue: Double) {
    val methodName = "addHeatReading"
    screenModelScope.launch {
      _heatMeterState.update { it.copy(isLastReadingLoading = true) }
      try {
        val params = MeterReadingsParams(
          uid = uid,
          newValue = newValue,
          currentValue = currentValue,
          meterId = teplomerId
        )
        val response = heatMeterRepository.addHeatReading(params)
        if (response.success == 1) {
          SnackbarManager.showMessage("Показання додані")
          getLastHeatReading(uid, teplomerId)
        } else {
          SnackbarManager.showMessage(response.message ?: "Помилка додавання")
        }
        _heatMeterState.update { it.copy(isLastReadingLoading = false) }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Add heat reading error: ${e.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером")
        _heatMeterState.update { it.copy(isLastReadingLoading = false) }
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
