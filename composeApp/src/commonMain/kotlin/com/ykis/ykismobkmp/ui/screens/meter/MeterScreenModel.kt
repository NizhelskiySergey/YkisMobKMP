package com.ykis.ykismobkmp.ui.screens.meter

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

/**
 * [MeterScreenModel] — Кроссплатформенная модель управления списками счетчиков тепла и воды.
 * УНІФІКОВАНО: Локалізація та стандартне логування.
 */
class MeterScreenModel(
  private val meterService: MeterService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "MeterScreenModel"

  fun onTabSelect(index: Int) {
    _uiState.update { it.copy(selectedTab = index) }
  }

  fun getWaterMeterList(uid: String, addressId: Long) {
    val methodName = "getWaterMeterList"
    if (uid.isBlank() || addressId <= 0L) return

    screenModelScope.launch {
      meterService.getWaterMeterList(uid, addressId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val meters = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] ${meters.size} водомірів")
              currentState.copy(waterMeterList = meters, isMetersLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(error = result.message, isMetersLoading = false)
            }
            is Resource.Loading -> currentState.copy(isMetersLoading = true)
          }
        }
      }
    }
  }

  fun getHeatMeterList(uid: String, addressId: Long) {
    val methodName = "getHeatMeterList"
    if (uid.isBlank() || addressId <= 0L) return

    screenModelScope.launch {
      meterService.getHeatMeterList(uid, addressId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val meters = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] ${meters.size} лічильників тепла")
              currentState.copy(heatMeterList = meters, isMetersLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(error = result.message, isMetersLoading = false)
            }
            is Resource.Loading -> currentState.copy(isMetersLoading = true)
          }
        }
      }
    }
  }

  fun setWaterMeterDetail(waterMeterEntity: WaterMeterEntity) {
    _uiState.update { it.copy(
      selectedWaterMeter = waterMeterEntity,
      selectedContentDetail = ContentDetail.WATER_METER,
      showDetail = true
    ) }
  }

  fun setHeatMeterDetail(heatMeterEntity: HeatMeterEntity) {
    _uiState.update { it.copy(
      selectedHeatMeter = heatMeterEntity,
      selectedContentDetail = ContentDetail.HEAT_METER,
      showDetail = true
    ) }
  }

  fun closeContentDetail() {
    _uiState.update { it.copy(showDetail = false) }
  }

  fun getWaterReadings(uid: String, vodomerId: Long) {
    val methodName = "getWaterReadings"
    if (uid.isBlank() || vodomerId <= 0L) return

    screenModelScope.launch {
      meterService.getWaterReadings(uid, vodomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readings = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] ${readings.size} записів води")
              currentState.copy(waterReadings = readings, isReadingsLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> currentState.copy(isReadingsLoading = true)
          }
        }
      }
    }
  }

  fun getLastWaterReading(uid: String, vodomerId: Long) {
    val methodName = "getLastWaterReading"
    if (uid.isBlank() || vodomerId <= 0L) return

    screenModelScope.launch {
      meterService.getLastWaterReading(uid, vodomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання води отримано")
              currentState.copy(lastWaterReading = result.data, isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> currentState.copy(isLastReadingLoading = true)
          }
        }
      }
    }
  }

  fun getHeatReadings(uid: String, teplomerId: Long) {
    val methodName = "getHeatReadings"
    screenModelScope.launch {
      meterService.getHeatReadings(uid, teplomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readings = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] ${readings.size} записів тепла")
              currentState.copy(heatReadings = readings, isReadingsLoading = false)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> currentState.copy(isReadingsLoading = true)
          }
        }
      }
    }
  }

  fun getLastHeatReading(uid: String, teplomerId: Long) {
    val methodName = "getLastHeatReading"
    if (uid.isBlank() || teplomerId <= 0L) return

    screenModelScope.launch {
      meterService.getLastHeatReading(uid, teplomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання тепла отримано")
              currentState.copy(lastHeatReading = result.data, isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> currentState.copy(isLastReadingLoading = true)
          }
        }
      }
    }
  }

  fun addWaterReading(uid: String, newValue: Long, currentValue: Long, vodomerId: Long) {
    val methodName = "addWaterReading"
    if (uid.isBlank() || vodomerId <= 0L) return

    if (newValue < currentValue) {
      screenModelScope.launch {
          SnackbarManager.showMessage(getString(Res.string.error_incorrect_reading))
      }
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [START] Передача: $newValue")

    screenModelScope.launch {
      meterService.addWaterReading(uid, vodomerId, currentValue, newValue).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання додані")
              getLastWaterReading(uid, vodomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> currentState.copy(isLastReadingLoading = true)
          }
        }
      }
    }
  }

  fun deleteLastWaterReading(uid: String, vodomerId: Long, readingId: Long) {
    val methodName = "deleteLastWaterReading"
    if (uid.isBlank() || vodomerId <= 0L || readingId <= 0L) return

    screenModelScope.launch {
      meterService.deleteLastWaterReading(uid, readingId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання видалено")
              getLastWaterReading(uid, vodomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> currentState.copy(isLastReadingLoading = true)
          }
        }
      }
    }
  }

  fun addHeatReading(uid: String, teplomerId: Long, currentValue: Double, newValue: Double) {
    val methodName = "addHeatReading"
    if (uid.isBlank() || teplomerId <= 0L) return

    if (newValue < currentValue) {
      screenModelScope.launch {
          SnackbarManager.showMessage(getString(Res.string.error_incorrect_reading))
      }
      return
    }

    screenModelScope.launch {
      meterService.addHeatReading(uid, teplomerId, currentValue, newValue).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання додані")
              getLastHeatReading(uid, teplomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> currentState.copy(isLastReadingLoading = true)
          }
        }
      }
    }
  }

  fun deleteLastHeatReading(readingId: Long, teplomerId: Long, uid: String) {
    val methodName = "deleteLastHeatReading"
    if (uid.isBlank() || teplomerId <= 0L || readingId <= 0L) return

    screenModelScope.launch {
      meterService.deleteLastHeatReading(uid, readingId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання видалено")
              getLastHeatReading(uid, teplomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> currentState.copy(isLastReadingLoading = true)
          }
        }
      }
    }
  }

  fun onNewWaterReadingChange(newValue: String) {
    _uiState.update { it.copy(newWaterReading = newValue) }
  }

  fun onNewHeatReadingChange(newValue: String) {
    _uiState.update { it.copy(newHeatReading = newValue) }
  }

  fun setContentDetail(contentDetail: ContentDetail) {
    _uiState.update { it.copy(selectedContentDetail = contentDetail) }
  }
}
