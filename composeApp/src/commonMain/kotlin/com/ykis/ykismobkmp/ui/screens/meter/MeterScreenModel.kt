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


/**
 * [MeterScreenModel] — Кроссплатформенная модель управления списками счетчиков тепла и воды ЮКИС.
 */

class MeterScreenModel(
  private val meterService: MeterService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "MeterScreenModel"

  /**
   * [onTabSelect] — Переключение между водой и теплом.
   */
  fun onTabSelect(index: Int) {
    _uiState.update { it.copy(selectedTab = index) }
  }

  /**
   * [getWaterMeterList] — Запрос и реактивное обновление приборов учета Водоканала города Южный.
   */
  fun getWaterMeterList(uid: String, addressId: Long) {
    val methodName = "getWaterMeterList"
    if (uid.isBlank() || addressId <= 0L) return

    screenModelScope.launch {
      meterService.getWaterMeterList(uid, addressId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val metersList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Выведено ${metersList.size} водомеров")
              currentState.copy(
                waterMeterList = metersList,
                isMetersLoading = false,
                error = null
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой загрузки водомеров: ${result.message}")
              currentState.copy(
                error = result.message ?: "Ошибка загрузки данных Водоканала",
                isMetersLoading = false
              )
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос списка приборов учета воды...")
              currentState.copy(isMetersLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [getHeatMeterList] — Запрос и реактивное обновление приборов учета Теплосети (ЮТКЕ) города Южный.
   */
  fun getHeatMeterList(uid: String, addressId: Long) {
    val methodName = "getHeatMeterList"
    if (uid.isBlank() || addressId <= 0L) return

    screenModelScope.launch {
      meterService.getHeatMeterList(uid, addressId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val metersList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Выведено ${metersList.size} теплосчетчиков")
              currentState.copy(
                heatMeterList = metersList,
                isMetersLoading = false,
                error = null
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой загрузки теплосчетчиков: ${result.message}")
              currentState.copy(
                error = result.message ?: "Ошибка загрузки данных ЮТКЕ",
                isMetersLoading = false
              )
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос списка приборов учета тепла...")
              currentState.copy(isMetersLoading = true)
            }
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

  /**
   * [getWaterReadings] — Запрос истории показаний водомера.
   */
  fun getWaterReadings(uid: String, vodomerId: Long) {
    val methodName = "getWaterReadings"
    if (uid.isBlank() || vodomerId <= 0L) return

    screenModelScope.launch {
      meterService.getWaterReadings(uid, vodomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readingsList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Получено ${readingsList.size} показаний воды")
              currentState.copy(
                waterReadings = readingsList,
                isReadingsLoading = false,
                error = null
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой загрузки истории воды: ${result.message}")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос истории показаний водомера...")
              currentState.copy(isReadingsLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [getLastWaterReading] — Чтение последнего зафиксированного показания водомера.
   */
  fun getLastWaterReading(uid: String, vodomerId: Long) {
    val methodName = "getLastWaterReading"
    if (uid.isBlank() || vodomerId <= 0L) return

    screenModelScope.launch {
      meterService.getLastWaterReading(uid, vodomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Последнее показание воды получено")
              currentState.copy(
                lastWaterReading = result.data,
                isLastReadingLoading = false,
                error = null
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой получения последнего показания воды: ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос последнего показания водомера...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [getHeatReadings] — Архив истории гигакалорий теплосети.
   */
  fun getHeatReadings(uid: String, teplomerId: Long) {
    val methodName = "getHeatReadings"
    screenModelScope.launch {
      meterService.getHeatReadings(uid, teplomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readingsList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Получено ${readingsList.size} показаний тепла")
              currentState.copy(
                heatReadings = readingsList,
                isReadingsLoading = false
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой загрузки истории тепла: ${result.message}")
              currentState.copy(isReadingsLoading = false)
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос истории показаний тепломера...")
              currentState.copy(isReadingsLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [getLastHeatReading] — Чтение последнего зафиксированного показания теплосчетчика.
   */
  fun getLastHeatReading(uid: String, teplomerId: Long) {
    val methodName = "getLastHeatReading"
    if (uid.isBlank() || teplomerId <= 0L) return

    screenModelScope.launch {
      meterService.getLastHeatReading(uid, teplomerId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Последнее показание тепла получено")
              currentState.copy(
                lastHeatReading = result.data,
                isLastReadingLoading = false,
                error = null
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой получения последнего показания тепла: ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запрос последнего показания тепломера...")
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [addWaterReading] — Передача показаний воды в расчетный центр.
   * ИСПРАВЛЕНО: Интегрирована жесткая валидация (new >= current).
   */
  fun addWaterReading(uid: String, newValue: Long, currentValue: Long, vodomerId: Long) {
    val methodName = "addWaterReading"
    if (uid.isBlank() || vodomerId <= 0L) return

    // ВАЛИДАЦИЯ: Новые показания не могут быть меньше текущих
    if (newValue < currentValue) {
      println("[YkisLogKMP.$className.$methodName]: [REJECT] Введенное значение $newValue меньше текущего $currentValue")
      SnackbarManager.showMessage("Нові показання не можуть бути меншими за поточні")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [START] Передача новых кубов воды: $newValue")

    screenModelScope.launch {
      meterService.addWaterReading(
        uid = uid,
        vodomerId = vodomerId,
        currentValue = currentValue,
        newValue = newValue
      ).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показания воды добавлены")
              SnackbarManager.showMessage("Показання успішно додані")
              getLastWaterReading(uid, vodomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой добавления воды: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Ошибка добавления")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [deleteLastWaterReading] — Удаление последнего показания воды.
   */
  fun deleteLastWaterReading(uid: String, vodomerId: Long, readingId: Long) {
    val methodName = "deleteLastWaterReading"
    if (uid.isBlank() || vodomerId <= 0L || readingId <= 0L) return

    screenModelScope.launch {
      meterService.deleteLastWaterReading(uid, readingId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показание воды удалено")
              SnackbarManager.showMessage("Показання успішно видалені")
              getLastWaterReading(uid, vodomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой удаления: ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [addHeatReading] — Передача показаний тепла.
   */
  fun addHeatReading(uid: String, teplomerId: Long, currentValue: Double, newValue: Double) {
    val methodName = "addHeatReading"
    if (uid.isBlank() || teplomerId <= 0L) return

    if (newValue < currentValue) {
      println("[YkisLogKMP.$className.$methodName]: [REJECT] Введенное тепло $newValue меньше текущего $currentValue")
      SnackbarManager.showMessage("Нові показання не можуть бути меншими за поточні")
      return
    }

    screenModelScope.launch {
      meterService.addHeatReading(
        uid = uid,
        teplomerId = teplomerId,
        currentValue = currentValue,
        newValue = newValue
      ).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показания тепла добавлены")
              SnackbarManager.showMessage("Показання успішно додані")
              getLastHeatReading(uid, teplomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой добавления тепла: ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              currentState.copy(isLastReadingLoading = true)
            }
          }
        }
      }
    }
  }

  /**
   * [deleteLastHeatReading] — Удаление последнего показания тепла.
   */
  fun deleteLastHeatReading(readingId: Long, teplomerId: Long, uid: String) {
    val methodName = "deleteLastHeatReading"
    if (uid.isBlank() || teplomerId <= 0L || readingId <= 0L) return

    screenModelScope.launch {
      meterService.deleteLastHeatReading(uid, readingId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показание тепла удалено")
              SnackbarManager.showMessage("Показання успішно видалені")
              getLastHeatReading(uid, teplomerId)
              currentState.copy(isLastReadingLoading = false, error = null)
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбой удаления тепла: ${result.message}")
              currentState.copy(isLastReadingLoading = false)
            }
            is Resource.Loading -> {
              currentState.copy(isLastReadingLoading = true)
            }
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
// Конец класса MeterScreenModel


