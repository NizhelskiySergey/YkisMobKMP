package com.ykis.ykismobkmp.ui.screens.meter

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.MeterReadingsParams
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
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
import kotlin.collections.emptyList


private const val tag = "MeterScreenModel"

// Перечисление типов счетчиков (замени на свою КМР-сущность ContentDetail, если пакет другой)


/**
 * [MeterScreenModel] — Кроссплатформенная модель управления списками счетчиков тепла и воды ЮКИС.
 * Полностью типизирована под Long идентификаторы и готова к запуску на Mac Desktop и Android.
 */
class MeterScreenModel(
  private val waterMeterRepository: WaterMeterRepository,
  private val heatMeterRepository: HeatMeterRepository,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _waterMeterState = MutableStateFlow(WaterMeterState())
  val waterMeterState: StateFlow<WaterMeterState> = _waterMeterState.asStateFlow()

  private val _baseUIState = MutableStateFlow(BaseUIState())
  val baseUIState: StateFlow<BaseUIState> = _baseUIState.asStateFlow()

  private val _heatMeterState = MutableStateFlow(HeatMeterState())
  val heatMeterState: StateFlow<HeatMeterState> = _heatMeterState.asStateFlow()

  private val _showDetail = MutableStateFlow(false)
  val showDetail: StateFlow<Boolean> = _showDetail.asStateFlow()

  private val _contentDetail = MutableStateFlow(ContentDetail.WATER_METER)
  val contentDetail: StateFlow<ContentDetail> = _contentDetail.asStateFlow()

  // Вкладка на экране списков
  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  fun onTabSelect(index: Int) {
    _selectedTab.value = index
  }

  /**
   * [getWaterMeterList] — Загрузка списка водомеров.
   * ИСПРАВЛЕНО: addressId переведен на Long под КМР-стандарт СУБД и репозиториев.
   */
  fun getWaterMeterList(uid: String, addressId: Long) {
    val methodName = "getWaterMeterList"
    // ИСПРАВЛЕНО: Используем screenModelScope вместо viewModelScope для Voyager
    screenModelScope.launch {
      _waterMeterState.update {
        it.copy(
          waterMeterList = emptyList(),
          isMetersLoading = true
        )
      }
      try {
        // Прямой вызов репозитория Ktor (передаем чистый Long ID)
        val response = waterMeterRepository.getWaterMeterList(uid, addressId)
        val metersList = response.waterMeters ?: emptyList()

        _waterMeterState.update {
          it.copy(
            waterMeterList = metersList,
            isMetersLoading = false
          )
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: [CRITICAL_ERROR] ${e.message}")
        _waterMeterState.update {
          it.copy(
            error = e.message ?: "Помилка мережі",
            isMetersLoading = false
          )
        }
      }
    }
  }

  /**
   * [getHeatMeterList] — Загрузка списка теплосчетчиков.
   * ИСПРАВЛЕНО: addressId переведен на Long.
   */
  fun getHeatMeterList(uid: String, addressId: Long) {
    val methodName = "getHeatMeterList"
    screenModelScope.launch {
      _heatMeterState.update {
        it.copy(
          heatMeterList = emptyList(),
          isMetersLoading = true
        )
      }
      try {
        val response = heatMeterRepository.getHeatMeterList(uid, addressId)

        _heatMeterState.update {
          it.copy(
            heatMeterList = response.heatMeters ?: emptyList(),
            isMetersLoading = false
          )
        }
      } catch (e: Exception) {
        // ИСПРАВЛЕНО: Заменен Android Log.e на универсальный println() под Mac JVM
        println("[$tag.$methodName]: Error fetching heat meters: ${e.message}")
        _heatMeterState.update {
          it.copy(
            error = e.message ?: "Unexpected error!",
            isMetersLoading = false
          )
        }
      }
    }
  }

  fun setWaterMeterDetail(waterMeterEntity: WaterMeterEntity) {
    _waterMeterState.update {
      it.copy(selectedWaterMeter = waterMeterEntity)
    }
    _contentDetail.value = ContentDetail.WATER_METER
    _showDetail.value = true
  }

  fun setHeatMeterDetail(heatMeterEntity: HeatMeterEntity) {
    _heatMeterState.update {
      it.copy(selectedHeatMeter = heatMeterEntity)
    }
    _contentDetail.value = ContentDetail.HEAT_METER
    _showDetail.value = true
  }

  fun closeContentDetail() {
    _showDetail.value = false
  }

  /**
   * [getWaterReadings] — Загрузка показаний по конкретному водомеру.
   * ИСПРАВЛЕНО: vodomerId переведен на Long.
   */
  fun getWaterReadings(uid: String, vodomerId: Long) {
    val methodName = "getWaterReadings"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isReadingsLoading = true) }

      try {
        // ИСПРАВЛЕНО: Заменен Dispatchers.IO на кроссплатформенный Dispatchers.Default
        val response = withContext(Dispatchers.Default) {
          waterMeterRepository.getWaterReadings(uid, vodomerId)
        }

        _waterMeterState.update { state ->
          state.copy(
            waterReadings = response.waterReadings ?: emptyList(),
            isReadingsLoading = false
          )
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching readings: ${e.message}")
        SnackbarManager.showMessage("Помилка завантаження показань")
        _waterMeterState.update { it.copy(isReadingsLoading = false) }
      }
    }
  }

  /**
   * [getLastWaterReading] — Получение последнего зафиксированного показания водомера.
   * ИСПРАВЛЕНО: vodomerId переведен на Long.
   */
  fun getLastWaterReading(uid: String, vodomerId: Long) {
    val methodName = "getLastWaterReading"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isLastReadingLoading = true) }

      try {
        val response = waterMeterRepository.getLastWaterReading(uid, vodomerId)

        _waterMeterState.update { state ->
          state.copy(
            lastWaterReading = response.waterReading ?: WaterReadingEntity(),
            isLastReadingLoading = false
          )
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching last reading: ${e.message}")
        SnackbarManager.showMessage("Помилка отримання останнього показання")
        _waterMeterState.update { it.copy(isLastReadingLoading = false) }
      }
    }
  }


  /**
   * [getHeatReadings] — Загрузка истории показаний по конкретному теплосчетчику г. Южный.
   * ИСПРАВЛЕНО: teplomerId переведен на Long, прямая мутация .value заменена на атомарный .update
   */
  fun getHeatReadings(uid: String, teplomerId: Long) {
    val methodName = "getHeatReadings"
    screenModelScope.launch {
      // Включаем индикатор загрузки атомарно
      _heatMeterState.update { it.copy(isReadingsLoading = true) }

      try {
        // Прямой вызов очищенного Ktor-репозитория (передаем чистый Long ID)
        val response = heatMeterRepository.getHeatReadings(uid, teplomerId)

        _heatMeterState.update { state ->
          state.copy(
            heatReadings = response.heatReadings ?: emptyList(),
            isReadingsLoading = false
          )
        }
      } catch (e: Exception) {
        // ИСПРАВЛЕНО: Платформенный Log.e заменен универсальным println() под Mac JVM
        println("[$tag.$methodName]: Error fetching heat readings: ${e.message}")
        SnackbarManager.showMessage("Помилка завантаження показань тепла")

        // Выключаем лоадер при ошибке
        _heatMeterState.update { it.copy(isReadingsLoading = false) }
      }
    }
  }


  /**
   * [getLastHeatReading] — Получение последнего зафиксированного показания теплосчетчика.
   * ИСПРАВЛЕНО: teplomerId изменен на Long, прямая мутация .value заменена на .update
   */
  fun getLastHeatReading(uid: String, teplomerId: Long) {
    val methodName = "getLastHeatReading"
    screenModelScope.launch {
      _heatMeterState.update { it.copy(isReadingsLoading = true) }
      try {
        val response = heatMeterRepository.getLastHeatReading(uid, teplomerId)

        _heatMeterState.update { state ->
          state.copy(
            lastHeatReading = response.heatReading ?: HeatReadingEntity(),
            isReadingsLoading = false
          )
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: Error fetching last heat reading: ${e.message}")
        SnackbarManager.showMessage("Помилка отримання останнього показання тепла")
        _heatMeterState.update { it.copy(isReadingsLoading = false) }
      }
    }
  }

  /**
   * [addWaterReading] — Отправка новых показаний водомера в расчетный центр г. Южный.
   * ИСПРАВЛЕНО: Идентификаторы переведены на Long, передаваемые кубы на Double.
   */
  fun addWaterReading(uid: String, newValue: Double, currentValue: Double, vodomerId: Long) {
    val methodName = "addWaterReading"
    screenModelScope.launch {
      _waterMeterState.update { it.copy(isLastReadingLoading = true) }
      try {
        // Вызываем конструктор параметров счетчиков воды, который мы зафиксировали ранее
        val params = MeterReadingsParams(
          uid = uid,
          newValue = newValue,
          currentValue = currentValue,
          meterId = vodomerId
        )
        val response = waterMeterRepository.addWaterReading(params)

        if (response.success == 1) {
          SnackbarManager.showMessage("Показання додані")
          getLastWaterReading(uid, vodomerId) // Каскадное обновление данных
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

  /**
   * [deleteLastWaterReading] — Удаление ошибочных показаний водомера.
   * ИСПРАВЛЕНО: Идентификаторы переведены на Long.
   */
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

  /**
   * [deleteLastHeatReading] — Удаление ошибочных показаний счетчика тепла.
   * ИСПРАВЛЕНО: Идентификаторы переведены на Long.
   */
  fun deleteLastHeatReading(uid: String, teplomerId: Long, readingId: Long) {
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

  /**
   * [addHeatReading] — Отправка показаний счетчика тепла.
   * ИСПРАВЛЕНО: Идентификаторы переведены на Long.
   */
  fun addHeatReading(uid: String, newValue: Double, currentValue: Double, teplomerId: Long) {
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

