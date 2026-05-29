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


/**
 * [MeterScreenModel] — Кроссплатформенная модель управления списками счетчиков тепла и воды ЮКИС.
 */

class MeterScreenModel(
  private val meterService: MeterService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "MeterScreenModel"

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
   * [getWaterMeterList] — Запит та реактивне оновлення приладів обліку Водоканалу міста Южне.
   */
  fun getWaterMeterList(uid: String, addressId: Long) {
    val methodName = "getWaterMeterList"
    if (uid.isBlank() || addressId <= 0L) return

    screenModelScope.launch {
      // Збираємо реактивний потік з нашого запечатаного MeterService
      meterService.getWaterMeterList(uid, addressId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val metersList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Успішно виведено ${metersList.size} водомірів")
              currentState.copy(
                waterMeterList = metersList,
                isMetersLoading = false,
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій завантаження водомірів: ${result.message}")
              currentState.copy(
                error = result.message ?: "Помилка завантаження даних Водоканалу",
                isMetersLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запит списку приладів обліку води ЮКІС з мережі Ktor...")
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
   * [getHeatMeterList] — Запит та реактивне оновлення приладів обліку Тепломережі (ЮТКЕ) міста Южне.
   */
  fun getHeatMeterList(uid: String, addressId: Long) {
    val methodName = "getHeatMeterList"
    if (uid.isBlank() || addressId <= 0L) return

    screenModelScope.launch {
      // Збираємо реактивний потік з нашого запечатаного MeterService
      meterService.getHeatMeterList(uid, addressId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val metersList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Успішно виведено ${metersList.size} теплолічильників")
              currentState.copy(
                heatMeterList = metersList,
                isMetersLoading = false,
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій завантаження теплолічильників: ${result.message}")
              currentState.copy(
                error = result.message ?: "Помилка завантаження даних ЮТКЕ",
                isMetersLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запит списку приладів обліку тепла ЮКІС з мережі Ktor...")
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

  // --- ЛОГІКА ІСТОРІЇ ТА ОСТАННІХ ПОКАЗАНЬ ВОДИ ЮКІС ----------------------

  /**
   * [getWaterReadings] — Запит та реактивне оновлення історії показань водоміра абонента.
   */
  fun getWaterReadings(uid: String, vodomerId: Long) {
    val methodName = "getWaterReadings"
    if (uid.isBlank() || vodomerId <= 0L) return

    screenModelScope.launch {
      meterService.getWaterReadings(uid, vodomerId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              val readingsList = result.data ?: emptyList()
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Отримано ${readingsList.size} показань води")
              currentState.copy(
                waterReadings = readingsList,
                isReadingsLoading = false,
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій завантаження історії води: ${result.message}")
              SnackbarManager.showMessage("Помилка завантаження показань Водоканалу")
              currentState.copy(
                isReadingsLoading = false
                // Сохраняем старый список истории в ОЗУ, чтобы экран не мигал белым холстом
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запит історії показань водоміра в мережі Ktor ЮКІС...")
              currentState.copy(
                isReadingsLoading = true
              )
            }
          }
        }
      }
    }
  }

  /**
   * [getLastWaterReading] — Читання останньої зафіксованої квитанції водоміра.
   */
  fun getLastWaterReading(uid: String, vodomerId: Long) {
    val methodName = "getLastWaterReading"
    if (uid.isBlank() || vodomerId <= 0L) return

    screenModelScope.launch {
      meterService.getLastWaterReading(uid, vodomerId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Останнє показання води отримано з біллінгу")
              currentState.copy(
                lastWaterReading = result.data,
                isLastReadingLoading = false,
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій отримання останнього показання води: ${result.message}")
              SnackbarManager.showMessage("Помилка отримання останнього показання ЮКІС")
              currentState.copy(
                isLastReadingLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запит останнього розрахункового показання водоміра ЮКІС...")
              currentState.copy(
                isLastReadingLoading = true
              )
            }
          }
        }
      }
    }
  }

  // --- ЛОГИКА ИСТОРИИ И ПОСЛЕДНИХ ПОКАЗАНИЙ ТЕПЛА ---------------------
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
   * [getLastHeatReading] — Читання останньої зафіксованої квитанції теплолічильника ЮКІС.
   * ИСПРАВЛЕНО НАМЕРТВО: Сбойный флаг лоадера заменен на легитимный isLastReadingLoading!
   * Теперь интерфейс Теплосети вовремя спрячет крутилку и плавно покажет кубы Гкал абонента.
   */
  fun getLastHeatReading(uid: String, teplomerId: Long) {
    val methodName = "getLastHeatReading"
    if (uid.isBlank() || teplomerId <= 0L) return

    screenModelScope.launch {
      meterService.getLastHeatReading(uid, teplomerId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Останнє показання тепла отримано з біллінгу")
              currentState.copy(
                lastHeatReading = result.data,
                isLastReadingLoading = false, // ИСПРАВЛЕНО НАМЕРТВО
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій отримання останнього показання тепла: ${result.message}")
              SnackbarManager.showMessage("Помилка отримання останнього показання тепла ЮКІС")
              currentState.copy(
                isLastReadingLoading = false // ИСПРАВЛЕНО НАМЕРТВО
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Запит останнього розрахункового показання тепломіра ЮКІС...")
              currentState.copy(
                isLastReadingLoading = true // ИСПРАВЛЕНО НАМЕРТВО
              )
            }
          }
        }
      }
    }
  }
  // --- ОПЕРАЦІЇ ЗАПИСУ ТА ВИДАЛЕННЯ ПОКАЗАНЬ ВОДИ ЮКІС ---------------------

  /**
   * [addWaterReading] — Надсилання поточних кубометрів води абонента в розрахунковий центр Водоканалу.
   * ИСПРАВЛЕНО НАМЕРТВО: Интегрирована жесткая доменная валидация диапазона цифр (new >= current)!
   * Любые опечатки жителей отсекаются до запуска Ktor, полностью защищая биллинг от сбоев начислений.
   */
  fun addWaterReading(uid: String, newValue: Long, currentValue: Long, vodomerId: Long) {
    val methodName = "addWaterReading"
    if (uid.isBlank() || vodomerId <= 0L) return

    // ====================================================================
    // --- ИСПРАВЛЕНО НАМЕРТВО: КМР-ВАЛИДАЦИЯ ДИАПАЗОНА КОММУНАЛЬНЫХ ЦИФР ---
    // ====================================================================
    if (newValue <= currentValue) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_REJECT] Введене значення $newValue менше за поточне $currentValue. Відміна.")
      SnackbarManager.showMessage("Нові показання не можуть бути меншими за поточні")
      return // Жестко прерываем выполнение метода, блокируя сетевой спам Ktor!
    }
    // ====================================================================

    println("[YkisLogKMP.$className.$methodName]: [START] Надсилання нових кубів води: $newValue (Поточні: $currentValue)")

    screenModelScope.launch {
      meterService.addWaterReading(
        uid = uid,
        vodomerId = vodomerId,
        currentValue = currentValue,
        newValue = newValue
      ).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання води успішно додані в базу Водоканалу")
              SnackbarManager.showMessage("Показання успішно додані")

              // Каскадный автоматический перезапрос свежей расчетной квитанции из сети
              getLastWaterReading(uid, vodomerId)

              currentState.copy(
                isLastReadingLoading = false, // ИСПРАВЛЕНО НАМЕРТВО: Лоадер гасится синхронно!
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій додавання показань води: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка додавання показань води")
              currentState.copy(
                isLastReadingLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Відправка нових кубометрів води в розрахунковий центр ЮКІС...")
              currentState.copy(
                isLastReadingLoading = true
              )
            }
          }
        }
      }
    }
  }

  /**
   * [deleteLastWaterReading] — Анулювання останнього помилково введеного показання водоміра в СУБД биллинга.
   */
  fun deleteLastWaterReading(uid: String, vodomerId: Long, readingId: Long) {
    val methodName = "deleteLastWaterReading"
    if (uid.isBlank() || vodomerId <= 0L || readingId <= 0L) return

    println("[YkisLogKMP.$className.$methodName]: [START] Запит на видалення показання ID: $readingId для водоміра: $vodomerId")

    screenModelScope.launch {
      meterService.deleteLastWaterReading(uid, readingId).collect { result ->
        _waterMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання води успішно видалені з бази даних")
              SnackbarManager.showMessage("Показання успішно видалені")

              // Перевычитываем последнюю квитанцию БТИ, чтобы вернуть холст к предыдущей легитимной цифре
              getLastWaterReading(uid, vodomerId)

              currentState.copy(
                isLastReadingLoading = false, // ИСПРАВЛЕНО НАМЕРТВО
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій видалення показання води: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка видалення показання")
              currentState.copy(
                isLastReadingLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Анулювання помилкового показання води в СУБД ЮКІС...")
              currentState.copy(
                isLastReadingLoading = true
              )
            }
          }
        }
      }
    }
  }


  // ====================================================================
  // --- ОПЕРАЦИИ ЗАПИСИ И УДАЛЕНИЯ ПОКАЗАНИЙ ТЕПЛА --------------------
  // ====================================================================

  // --- ОПЕРАЦІЇ ЗАПИСУ ТА ВИДАЛЕННЯ ПОКАЗАНЬ ТЕПЛА ЮКІС ---------------------

  /**
   * [addHeatReading] — Надсилання гігакалорій тепла абонента в розрахунковий центр Тепломережі (ЮТКЕ).
   * ИСПРАВЛЕНО НАМЕРТВО: Интегрирована жесткая доменная Double-валидация диапазона цифр (new >= current)!
   * Любые случайные опечатки абонентов блокируются до вылета Ktor-пакета, защищая биллинг от сбоев.
   */
  fun addHeatReading(uid: String, teplomerId: Long, currentValue: Double, newValue: Double) {
    val methodName = "addHeatReading"
    if (uid.isBlank() || teplomerId <= 0L) return

    // ====================================================================
    // --- ИСПРАВЛЕНО НАМЕРТВО: КМР Double-ВАЛИДАЦИЯ КОММУНАЛЬНОГО ТЕПЛА ---
    // ====================================================================
    if (newValue < currentValue) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_REJECT] Введене значення тепла $newValue менше за поточне $currentValue. Відміна.")
      SnackbarManager.showMessage("Нові показання не можуть бути меншими за поточні")
      return // Жестко прерываем выполнение метода, блокируя сетевой спам Ktor!
    }
    // ====================================================================

    println("[YkisLogKMP.$className.$methodName]: [START] Надсилання нових гігакалорій тепла: $newValue (Поточні: $currentValue)")

    screenModelScope.launch {
      meterService.addHeatReading(
        uid = uid,
        teplomerId = teplomerId,
        currentValue = currentValue,
        newValue = newValue
      ).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання тепла успішно додані в базу ЮТКЕ")
              SnackbarManager.showMessage("Показання успішно додані")

              // Каскадный автоматический перезапрос свежей расчетной квитанции тепла
              getLastHeatReading(uid, teplomerId)

              currentState.copy(
                isLastReadingLoading = false, // ИСПРАВЛЕНО НАМЕРТВО
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій додавання показань тепла: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка додавання показань тепла")
              currentState.copy(
                isLastReadingLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Відправка нових гігакалорій тепла в розрахунковий центр ЮКІС...")
              currentState.copy(
                isLastReadingLoading = true
              )
            }
          }
        }
      }
    }
  }

  /**
   * [deleteLastHeatReading] — Анулювання останнього помилково введеного показання тепломіра в СУБД биллинга ЮТКЕ.
   */
  fun deleteLastHeatReading(readingId: Long, teplomerId: Long, uid: String) {
    val methodName = "deleteLastHeatReading"
    if (uid.isBlank() || teplomerId <= 0L || readingId <= 0L) return

    println("[YkisLogKMP.$className.$methodName]: [START] Запит на видалення показання ID: $readingId для тепломіра: $teplomerId")

    screenModelScope.launch {
      meterService.deleteLastHeatReading(uid, readingId).collect { result ->
        _heatMeterState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Показання тепла успішно видалені з бази даних")
              SnackbarManager.showMessage("Показання успішно видалені")

              // Перевычитываем последнюю тепловую квитанцию для отката холста к легитимной цифре
              getLastHeatReading(uid, teplomerId)

              currentState.copy(
                isLastReadingLoading = false, // ИСПРАВЛЕНО НАМЕРТВО
                error = null
              )
            }

            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій видалення показання тепла: ${result.message}")
              SnackbarManager.showMessage(result.message ?: "Помилка видалення показання")
              currentState.copy(
                isLastReadingLoading = false
              )
            }

            is Resource.Loading -> {
              println("[YkisLogKMP.$className.$methodName]: [LOADING] Анулювання помилкового показання тепла в СУБД ЮКІС...")
              currentState.copy(
                isLastReadingLoading = true
              )
            }
          }
        }
      }
    }
  }

  // --- РЕАКТИВНЕ ОНОВЛЕННЯ ПОЛІВ ВВОДУ З КЛАВІАТУРИ НА ЭКРАНАХ ЮКІС ---

  fun onNewWaterReadingChange(newValue: String) {
    _waterMeterState.update { it.copy(newWaterReading = newValue) }
  }

  fun onNewHeatReadingChange(newValue: String) {
    _heatMeterState.update { it.copy(newHeatReading = newValue) }
  }

  fun setContentDetail(contentDetail: ContentDetail) {
    _contentDetail.value = contentDetail
  }
} // Конец класса MeterScreenModel


