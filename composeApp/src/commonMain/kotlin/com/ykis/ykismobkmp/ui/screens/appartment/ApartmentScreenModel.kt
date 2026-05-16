package com.ykis.ykismobkmp.ui.screens.appartment

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.bti.ContactUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val tag = "ApartmentScreenModel"

// Режимы вложенности списков (Районы г. Южное -> Дома -> Квартиры жителей)
enum class ListMode { RAIONS, HOUSES, APARTMENTS }

/**
 * [ApartmentScreenModel] — Кроссплатформенная модель управления привязкой лицевых счетов и админ-панелей ОСМД.
 * Полностью переведена на типы Long и готова к выполнению на Mac Desktop (JVM) и мобильных ОС.
 */
class ApartmentScreenModel(
  private val firebaseService: FirebaseService,
  private val apartmentService: ApartmentService, // Твой КМР доменный сервис квартир
  logService: LogService
) : BaseScreenModel(logService) {

  private val isEmailVerified get() = firebaseService.currentUser?.isEmailVerified ?: false
  val uid get() = firebaseService.uid

  private val displayName get() = firebaseService.displayName
  val email get() = firebaseService.email

  // ИСПРАВЛЕНО: Все жилищно-коммунальные ID переведены на сквозной КМР-тип Long под SQLDelight 2.x
  var lastLoadedAddressId: Long = -1L
  private var observeJob: Job? = null
  private var isHandlingResult = false

  private val _apartment = MutableStateFlow(ApartmentEntity())
  val apartment: StateFlow<ApartmentEntity> get() = _apartment.asStateFlow()

  private var isObservingStarted = false
  private val _secretCode = MutableStateFlow("")
  val secretCode: StateFlow<String> = _secretCode.asStateFlow()

  private var lastHandledResultId: Long? = null // Храним ID последней обработанной операции (Long)

  // LaunchScreen
  private val _showError = MutableStateFlow(false)
  val showError: StateFlow<Boolean> = _showError.asStateFlow()

  // ИСПРАВЛЕНО: Передан КМР screenModelScope контейнера Voyager вместо viewModelScope
  val authState = firebaseService.getAuthState(screenModelScope)

  private val _drawerHouses = MutableStateFlow<List<HouseEntity>>(emptyList())
  val drawerHouses = _drawerHouses.asStateFlow()

  private val _drawerApartments = MutableStateFlow<List<ApartmentEntity>>(emptyList())
  val drawerApartments = _drawerApartments.asStateFlow()

  private val _drawerLoading = MutableStateFlow(false)
  val drawerLoading = _drawerLoading.asStateFlow()

  private val _contactUiState = MutableStateFlow(ContactUIState())
  val contactUIState: StateFlow<ContactUIState> = _contactUiState.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()
  private val _apartmentUiState = MutableStateFlow(BaseUIState())

  // 2. ИСПРАВЛЕНО: Твое привычное имя для UI экранов. Оно возвращает наше новое состояние БТИ
  val baseUIState: StateFlow<BaseUIState> = _apartmentUiState.asStateFlow()

  /**
   * [filteredApartments] — Адаптивный КМР-поток реактивной фильтрации списков БТИ при вводе в поисковую строку.
   * Автоматически обновляет результаты на экране при изменении строки поиска или структуры жилого фонда.
   */
  val filteredApartments: StateFlow<List<ApartmentEntity>> = combine(
    _searchQuery,
    _apartmentUiState, // ИСПРАВЛЕНО: Передаем сам КМР-поток Flow, а не ссылку на его метод .update
    _drawerHouses,
    _drawerApartments
  ) { query, state, houses, drApts ->
    // Если поисковая строка пустая, мгновенно возвращаем пустой список, чтобы UI показал базовый контент
    if (query.isEmpty()) return@combine emptyList()

    when (state.listMode) {
      ListMode.HOUSES -> {
        houses.filter { it.house.contains(query, ignoreCase = true) }
          // Формируем чистые доменные объекты квартир с Long-идентификаторами домов г. Южный
          .map { ApartmentEntity(address = it.house, addressId = it.houseId) }
      }

      ListMode.APARTMENTS -> {
        // Разделяем источники данных: для админов коммунальных служб берем из глобального drawerApartments,
        // для стандартных жителей — из его личного списка привязанных квартир state.apartments
        val source =
          if (state.userRole != UserRole.StandardUser && state.userRole != UserRole.OsbbUser) {
            drApts
          } else {
            state.apartments
          }

        // Выполняем каскадный поиск по адресу, ФИО нанимателя (nanim) или номеру лицевого счета БТИ
        source.filter {
          it.address.contains(query, ignoreCase = true) ||
            (it.nanim?.contains(query, ignoreCase = true) ?: false) ||
            it.addressId.toString().contains(query)
        }
      }

      ListMode.RAIONS -> emptyList() // Списки районов Одесской области в выпадающем меню не фильтруются
    }
  }.stateIn(
    scope = screenModelScope, // Контейнер жизненного цикла Voyager ScreenModel
    started = SharingStarted.WhileSubscribed(5000), // Защита от утечек: поток засыпает через 5 секунд после ухода пользователя с экрана
    initialValue = emptyList()
  )

  fun onSearchQueryChanged(newQuery: String) {
    _searchQuery.value = newQuery
  }

  fun clearState() {
    val methodName = "clearState"
    println("[$tag.$methodName]: [FORCE_RESET] Полная очистка графа привязки")

    observeJob?.cancel()
    lastLoadedAddressId = -1L

    _apartmentUiState.update {
      BaseUIState(uid = "empty", mainLoading = false)
    }
  }

  /**
   * [onRaionSelected] — Обработка выбора Района в Dropdown (г. Южное / Одесская область).
   */
  fun onRaionSelected(raion: RaionEntity) {
    val methodName = "onRaionSelected"
    // ИСПРАВЛЕНО: raionId переведен на Long
    val raionIdLong = raion.raionId ?: 0L

    println("[$tag.$methodName]: [START] Выбран Район: ${raion.raion} (ID: $raionIdLong)")

    _apartmentUiState.update {
      it.copy(
        selectedRaionId = raionIdLong,
        listMode = ListMode.HOUSES
      )
    }

    _drawerLoading.value = true

    // ИСПРАВЛЕНО: Запуск асинхронного сбора потока переведен на КМР screenModelScope
    screenModelScope.launch {
      apartmentService.getHouseList(raionIdLong).collect { result ->
        when (result) {
          is Resource.Success -> {
            val houses = result.data ?: emptyList()
            println("[$tag.$methodName]: [SUCCESS] Домов загружено в Drawer: ${houses.size}")

            _drawerHouses.value = houses
            _drawerLoading.value = false
          }

          is Resource.Error -> {
            // ИСПРАВЛЕНО: Заменен Log.e на println()
            println("[$tag.$methodName]: [ERROR] ${result.message}")
            _drawerLoading.value = false
            SnackbarManager.showMessage(result.message ?: "Помилка завантаження будинків")
          }

          is Resource.Loading -> {
            _drawerLoading.value = true
          }
        }
      }
    }
  }

  fun goBackLevel() {
    val methodName = "goBackLevel"

    _apartmentUiState.update { state ->
      val newMode = when (state.listMode) {
        ListMode.APARTMENTS -> {
          println("[$tag.$methodName]: Возврат к списку домов")
          ListMode.HOUSES
        }

        ListMode.HOUSES -> {
          println("[$tag.$methodName]: Возврат к списку районов")
          _drawerHouses.value = emptyList()
          ListMode.RAIONS
        }

        ListMode.RAIONS -> ListMode.RAIONS
      }
      state.copy(listMode = newMode)
    }
  }

fun onHouseSelected(houseId: Long) {
  val methodName = "onHouseSelected"
  println("[$tag.$methodName]: [START] Завантаження квартир для будинку ID: $houseId")

  _drawerLoading.value = true
  _apartmentUiState.update {
    it.copy(
      selectedHouseId = houseId,
      listMode = ListMode.APARTMENTS // Переключаем LazyColumn на drawerApartments
    )
  }

  // Асинхронный КМР-сбор потока данных через screenModelScope Voyager
  screenModelScope.launch {
    apartmentService.getOsbbApartmentsList(houseId).collect { result ->
      when (result) {
        is Resource.Success -> {
          val apartments = result.data ?: emptyList()
          println("[$tag.$methodName]: [SUCCESS] Отримано: ${apartments.size} кв.")

          _drawerApartments.value = apartments
          _drawerLoading.value = false
        }

        is Resource.Error -> {
          println("[$tag.$methodName]: [ERROR] ${result.message}")
          _drawerLoading.value = false

          // ИСПРАВЛЕНО: Корректный атомарный вызов обновления KMP-состояния при сбое сети
          _apartmentUiState.update { state ->
            state.copy(listMode = ListMode.HOUSES)
          }

          SnackbarManager.showMessage(result.message ?: "Помилка завантаження квартир")
        }

        is Resource.Loading -> {
          _drawerLoading.value = true
        }
      }
    }
  }
}
fun onSecretCodeChanged(newCode: String) {
  _secretCode.value = newCode
}

/**
 * [onAppStart] — Маршрутизатор точки входа в приложение ЮКИС.
 * Заменяет логику роутинга на КМР строковые дескрипторы графов навигации Voyager.
 */
fun onAppStart(): String {
  val userExists = firebaseService.hasUser
  val emailVerified = firebaseService.isEmailVerified
  println("[$tag.AppStart]: UserExists=$userExists, Verified=$emailVerified")

  return if (userExists && emailVerified == true) {
    // Если сессия активна, проверяем соответствие UID с локальным состоянием кэша
    if (_apartmentUiState.value.uid != null && _apartmentUiState.value.uid != firebaseService.uid) {
      println("[$tag.AppStart]: [WARNING] Обнаружен конфликт UID в памяти! Очистка стейта.")
      clearState()
    }
    "APARTMENT_GRAPH" // Твой строковый константный индикатор Graph.APARTMENT
  } else {
    // Если пользователя нет — принудительно очищаем граф перед уходом на авторизацию
    clearState()
    "AUTHENTICATION_GRAPH" // Graph.AUTHENTICATION
  }
}

fun observeUserProfile() {
  val methodName = "observeUserProfile"
  val actualUid = firebaseService.uid ?: run {
    println("[$tag.$methodName]: [ABORT] UID is null")
    return
  }

  println("[$tag.$methodName]: [START] actualUid: $actualUid")
  observeJob?.cancel()

  observeJob = screenModelScope.launch {
    _apartmentUiState.update { it.copy(mainLoading = true) }
    try {
      // 1. ПОЛУЧЕНИЕ ПРОФИЛЯ (Свежие КМР данные из Firebase)
      // ИСПРАВЛЕНО: Dispatchers.IO заменен на универсальный кроссплатформенный Dispatchers.Default
      val user = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        firebaseService.getUserProfile()
      }
      val currentUserRole = UserRole.valueOf(user.userRole) // Предполагаем Enum класс ролей

      // Присваиваем системные фиксированные ID для коммунальных организаций города Южный
      val currentOsbbId: Long = if (currentUserRole != UserRole.StandardUser &&
        currentUserRole != UserRole.OsbbUser && user.osbbId == 0L
      ) {
        when (currentUserRole) {
          UserRole.VodokanalUser -> 9999L
          UserRole.YtkeUser -> 9998L
          UserRole.TboUser -> 9997L
          else -> 0L
        }
      } else user.osbbId

      println("[$tag.$methodName]: [PROFILE_LOADED] Role: $currentUserRole, ID: $currentOsbbId")

      _apartmentUiState.update {
        it.copy(
          uid = user.uid,
          userRole = currentUserRole,
          osbbId = currentOsbbId,
          osmdId = currentOsbbId,
          displayName = user.name ?: ""
        )
      }

      // Каскадный КМР выбор логики биллинга на основе роли аккаунта
      when (currentUserRole) {
        UserRole.StandardUser -> {
          apartmentService.getApartmentList(user.uid).collect { result ->
            handleStandardUserResult(result, user.uid, currentUserRole)
          }
        }

        UserRole.OsbbUser -> {
          apartmentService.getOsbbApartmentsList(currentOsbbId).collect { result ->
            handleOsbbAdminResult(result, user.uid, currentUserRole, currentOsbbId, user.name)
          }
        }

        else -> {
          apartmentService.getRaionList(user.uid).collect { result ->
            // handleOrganizationResult(result, user.uid, currentUserRole, currentOsbbId, user.name)
          }
        }
      }
    } catch (e: Exception) {
      println("[$tag.$methodName]: [FATAL ERROR] ${e.message}")
      _apartmentUiState.update { it.copy(mainLoading = false) }
    }
  }
}

  /**
   * [handleOsbbAdminResult] — Обработка ответа сервера при получении списка квартир администратором ОСМД.
   * ИСПРАВЛЕНО: osbbId переведен на Long, _uiState изменен на _apartmentUiState, Log заменен на println().
   */
  private suspend fun handleOsbbAdminResult(
    result: Resource<List<ApartmentEntity>>,
    uid: String,
    role: UserRole,
    osbbId: Long, // ИСПРАВЛЕНО: Сквозной КМР-тип Long под SQLDelight 2.x
    name: String?
  ) {
    val methodName = "handleOsbbAdminResult"

    // Атомарно обновляем наше локальное переименованное КМР-состояние экрана
    _apartmentUiState.update { state ->
      when (result) {
        is Resource.Success -> {
          println("[$tag.$methodName]: [SUCCESS] Завантажено ${result.data?.size} кв. для ОСББ ID: $osbbId")
          state.copy(
            apartments = result.data ?: emptyList(),
            listMode = ListMode.APARTMENTS,
            mainLoading = false // Выключаем индикатор загрузки холодного старта
          )
        }

        is Resource.Error -> {
          println("[$tag.$methodName]: [ERROR] Сбій завантаження списку адміна: ${result.message}")
          state.copy(mainLoading = false) // Выключаем лоадер при возникновении ошибки PHP
        }

        is Resource.Loading -> {
          state.copy(mainLoading = true) // Включаем системный индикатор прогресса
        }
      }
    }

    // Если сетевой запрос Ktor к биллингу г. Южный прошел успешно — фиксируем токены прав
    if (result is Resource.Success) {
      // Синхронизируем права доступа внутри облачного Firestore
      firebaseService.updateUserRoleAndPermissions(
        uid = uid,
        addressId = 0L, // Администратор ОСМД смотрит весь дом, а не привязан к одной квартире
        userRole = role, // Передаем строковое имя роли под контракт KMP Firebase
        osbbId = osbbId,
        displayName = name
      )

      // Запускаем кроссплатформенный фоновый трекер чат-сообщений (при необходимости раскомментируй)
      // chatScreenModel.trackUserIdentifiersWithRole(role, osbbId)
    }
  }


private suspend fun handleStandardUserResult(
  result: Resource<List<ApartmentEntity>>,
  uid: String,
  role: UserRole
) {
  val methodName = "handleStandardUserResult"
  _apartmentUiState.update { state ->
    when (result) {
      is Resource.Success -> {
        val apartments = result.data ?: emptyList()
        println("[$tag.$methodName]: [SUCCESS] Получено квартир: ${apartments.size}")

        if (apartments.isNotEmpty()) {
          apartments.forEachIndexed { index, apt ->
            println("[$tag.$methodName]: [LIST_ITEM] #$index: ID=${apt.addressId}, Адрес=${apt.address}, OSBB_RAW='${apt.osbb}'")
          }

          // ИСПРАВЛЕНО: Сравнение ID переведено на Long типы
          val target = apartments.find { it.addressId == state.addressId } ?: apartments.first()
          println("[$tag.$methodName]: [TARGET_SELECT] Выбран ID=${target.addressId} (Текущий в стейте был: ${state.addressId})")

          val combinedName = "${target.address} | ${target.nanim ?: ""}"
          val rawOsbb = target.osbb?.toString()

          val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") {
            "Мій ОСББ"
          } else {
            rawOsbb
          }
          println("[$tag.$methodName]: [FINAL_OSBB] Устанавливаем в UI: '$finalOsbbName' (из сырого: '$rawOsbb')")
          firebaseService.updateUserRoleAndPermissions(
            uid = uid,
            addressId = target.addressId,
            userRole =  role,
            osbbId = target.osmdId,
            displayName = combinedName
          )

          state.copy(
            apartments = apartments,
            isApartmentsLoaded = true,
            addressId = target.addressId,
            address = target.address,
            osbbId = target.osmdId,
            osbb = finalOsbbName,
            displayName = combinedName,
            mainLoading = false
          )
        } else {
          println("[$tag.$methodName]: [WARNING] Список квартир пуст")
          state.copy(mainLoading = false, isApartmentsLoaded = true)
        }
      }

      is Resource.Error -> {
        println("[$tag.$methodName]: [ERROR] ${result.message}")
        state.copy(mainLoading = false)
      }

      is Resource.Loading -> state.copy(mainLoading = true)
    }
  }
}

private suspend fun handleOrganizationResult(
  result: Resource<List<RaionEntity>>,
  uid: String,
  role: UserRole,
  osbbId: Long, // ИСПРАВЛЕНО: Long
  name: String?
) {
  _apartmentUiState.update { state ->
    when (result) {
      is Resource.Success -> {
        println("[$tag.handleOrganizationResult]: [SUCCESS] Загружено ${result.data?.size} районов Одесской обл.")
        state.copy(
          raions = result.data ?: emptyList(),
          listMode = ListMode.RAIONS, // Переключаем Drawer на выбор районов коммунальной службы
          mainLoading = false
        )
      }

      is Resource.Error -> state.copy(mainLoading = false)
      is Resource.Loading -> state.copy(mainLoading = true)
    }
  }
  if (result is Resource.Success) {
    firebaseService.updateUserRoleAndPermissions(uid, 0L, role, osbbId, name)
    // chatScreenModel.trackUserIdentifiersWithRole(role, osbbId)
  }
}

fun resetToAdminMode() {
  val methodName = "resetToAdminMode"
  println("[$tag.$methodName]: [RESET] Возврат к администрированию коммунального фонда")
  _apartmentUiState.update {
    it.copy(
      apartments = emptyList(), // Очищаем список, чтобы навигация переключилась на списки БТИ
      addressId = 0L,
      address = "",
      mainLoading = true
    )
  }
  observeUserProfile()
}

fun onSecretCodeChange(newValue: String) {
  _secretCode.value = newValue
}

/**
 * [addApartment] — Точка обработки ввода инфо-кодов.
 * Разветвляет логику: Числа ➡️ Привязка квартиры жильцом, Текст ➡️ Авторизация админа ОСМД.
 */
fun addApartment(restartApp: () -> Unit) {
  val methodName = "addApartment"
  val input = secretCode.value.trim()
  if (input.isEmpty()) return

  println("[$tag.$methodName]: Клик по кнопке. Введен код: $input")
  val uid = firebaseService.uid ?: return
  val email = firebaseService.email ?: ""

  if (input.all { it.isDigit() }) {
    // --- ЛОГИКА ЖИЛЬЦА ЮЖНОГО ---
    apartmentService.addApartment(input, uid, email).onEach { result ->
      handleApartmentResult(uid, result, restartApp)
    }.launchIn(screenModelScope) // ИСПРАВЛЕНО: launchIn переведен на screenModelScope Voyager
  } else {
    // --- ЛОГИКА АДМИНИСТРАТОРА ОСМД ---
    apartmentService.verifyAdminCode(input, uid).onEach { result ->
      handleAdminResult(result, restartApp)
    }.launchIn(screenModelScope)
  }
}

private suspend fun handleApartmentResult(
  uid: String,
  result: Resource<GetSimpleResponse>,
  restartApp: () -> Unit
) {
  val methodName = "handleApartmentResult"
  if (isHandlingResult && result !is Resource.Loading) {
    println("[$tag.$methodName]: [SKIP] Повторный вызов блокирован предохранителем")
    return
  }

  when (result) {
    is Resource.Loading -> {
      println("[$tag.$methodName]: [LOADING]")
      _apartmentUiState.update{ it.copy(mainLoading = true) }
    }

    is Resource.Success -> {
      isHandlingResult = true
      val data = result.data ?: run {
        println("[$tag.$methodName]: [ERROR] Сетевой ответ пуст")
        isHandlingResult = false
        return
      }

      // 1. ИЗВЛЕЧЕНИЕ И СИНХРОНИЗАЦИЯ ТИПОВ ДАННЫХ
      val newAddressId = data.addressId ?: 0L
      val newOsbbId = data.osbbId ?: 0L
      val rawOsbbName = data.osbb?.toString()
      val newAddress = data.address ?: ""

      val finalOsbbName = if (rawOsbbName.isNullOrBlank() || rawOsbbName == "0") {
        "Мій ОСББ"
      } else {
        rawOsbbName
      }
      println("[$tag.$methodName]: [DATA_PARSE] ID: $newAddressId, OSBB_RAW: '$rawOsbbName', FINAL: '$finalOsbbName'")

      try {
        // 2. СИНХРОНИЗАЦИЯ С ОБЛАКОМ (Firestore)
        println("[$tag.$methodName]: [STEP 1] Фиксация прав в Firestore. Адрес: $newAddress")
        firebaseService.updateUserRoleAndPermissions(
          uid = uid,
          addressId = newAddressId,
          userRole = UserRole.StandardUser,
          osbbId = newOsbbId,
          displayName = newAddress
        )

        // 3. ОБНОВЛЕНИЕ UI STATE И ЧАТ-КАНАЛОВ
        println("[$tag.$methodName]: [STEP 2] Установка стейта. Помещение под контроль OSBB: $finalOsbbName")
        _apartmentUiState.update{
          it.copy(
            addressId = newAddressId,
            osmdId = newOsbbId,
            osbbId = newOsbbId,
            osbb = finalOsbbName,
            address = newAddress,
            userRole = UserRole.StandardUser,
            mainLoading = false
          )
        }

        if (lastHandledResultId != newAddressId) {
          lastHandledResultId = newAddressId
          println("[$tag.$methodName]: [STEP 3] Инициализация резидент-чатов для лицевого счета $newAddressId")
          // chatScreenModel.initResidentChats(uid, newOsbbId, newAddressId, newAddress, "")
        }

        _secretCode.value = ""
        SnackbarManager.showMessage("Особовий рахунок успішно прив'язано")

        println("[$tag.$methodName]: Ожидание стабилизации контекста...")
        kotlinx.coroutines.delay(500)
        isHandlingResult = false

        println("[$tag.$methodName]: [FINISH] Перезапуск графа навигации (restartApp)")
        restartApp()

      } catch (e: Exception) {
        println("[$tag.$methodName]: [CRITICAL ERROR] ${e.message}")
        isHandlingResult = false
        _apartmentUiState.update { it.copy(mainLoading = false) }
        SnackbarManager.showMessage("Помилка синхронізації профілю квартири")
      }
    }

    is Resource.Error -> {
      println("[$tag.$methodName]: [API ERROR] ${result.message}")
      isHandlingResult = false
      _apartmentUiState.update { it.copy(mainLoading = false) }
      SnackbarManager.showMessage(result.message ?: "Помилка додавання особового рахунку")
    }
  }
}

private fun handleAdminResult(
  result: Resource<GetSimpleResponse>,
  restartApp: () -> Unit
) {
  val methodName = "handleAdminResult"
  when (result) {
    is Resource.Success -> {
      val data = result.data ?: return
      val mappedRole = UserRole.valueOf(data.userRole ?: "StandardUser")
      val currentUid = firebaseService.uid ?: ""

      // Накатываем глобальные сквозные ID коммунальных предприятий г. Южный
      val newOsbbId: Long = when (mappedRole) {
        UserRole.VodokanalUser -> 9999L
        UserRole.YtkeUser -> 9998L
        UserRole.TboUser -> 9997L
        else -> data.osbbId ?: 0L
      }

      println("[$tag.$methodName]: [SUCCESS] Секретное слово принято. Роль: $mappedRole, ID Службы: $newOsbbId")

      screenModelScope.launch {
        try {
          kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            println("[$tag.$methodName]: [STEP 1] Запись токена админа в Firestore")
            firebaseService.updateUserRoleAndPermissions(
              uid = currentUid,
              addressId = 0L, // Администрация не привязана к одной ячейке БТИ
              userRole = mappedRole,
              osbbId = newOsbbId,
              displayName = null
            )
          }

          _apartmentUiState.update {
            it.copy(
              userRole = mappedRole,
              osbbId = newOsbbId,
              listMode = if (mappedRole == UserRole.OsbbUser) ListMode.APARTMENTS else ListMode.RAIONS,
              mainLoading = true
            )
          }

          println("[$tag.$methodName]: [STEP 2] Перезапуск трекеров диспетчеризации")
          observeUserProfile()
          // chatScreenModel.trackUserIdentifiersWithRole(mappedRole, newOsbbId)

          _secretCode.value = ""
          SnackbarManager.showMessage("Авторизація адміністратора успішна")
          restartApp()

        } catch (e: Exception) {
          println("[$tag.$methodName]: [CRITICAL ERROR] ${e.message}")
          _apartmentUiState.update { it.copy(mainLoading = false) }
          SnackbarManager.showMessage("Помилка авторизації прав доступу")
        }
      }
    }

    is Resource.Error -> {
      println("[$tag.$methodName]: [API ERROR] ${result.message}")
      SnackbarManager.showMessage(result.message ?: "Невірне секретне слово доступу")
    }

    is Resource.Loading -> {
      _apartmentUiState.update { it.copy(mainLoading = true) }
    }
  }
}

// КМР Regex-паттерн валидации электронной почты без привязки к android.util.Patterns
private fun String.isValidEmailKmp(): Boolean {
  val emailRegex = "^[A-Za-is0-9_+=%.&-]+@[A-Za-is0-9.-]+\\.[a-zA-is]{2,}\$"
  return this.matches(emailRegex.toRegex())
}

/**
 * [initialContactState] — Инициализация локального КМР-состояния контактов абонента БТИ.
 * ИСПРАВЛЕНО: Удален ошибочный вызов .update.value, чтение переведено на прямой снимок .value.
 */
fun initialContactState() {
  // 1. Захватываем текущий снимок состояния ЖКХ-фонда города Южный
  val currentState = _apartmentUiState.value

  // 2. ИСПРАВЛЕНО: Безопасно инициализируем поля через currentState без синтаксических ошибок
  _contactUiState.value = ContactUIState(
    email = currentState.apartment.email ?: "",
    phone = currentState.apartment.phone ?: "",
    addressId = currentState.addressId, // Наш сквозной Long ID
    address = currentState.address
  )
}


fun onEmailChange(newValue: String) {
  _contactUiState.value = _contactUiState.value.copy(email = newValue)
}

fun onPhoneChange(newValue: String) {
  _contactUiState.value = _contactUiState.value.copy(phone = newValue)
}

/**
 * [onUpdateBti] — Обновление контактных данных абонента БТИ г. Южный.
 * ИСПРАВЛЕНО: R.string заменен строками, корутина переведена на screenModelScope.
 */
fun onUpdateBti(uid: String) {
  val currentEmail = _contactUiState.value.email

  // Кроссплатформенная валидация
  if (!currentEmail.isValidEmailKmp() && currentEmail.isNotEmpty()) {
    SnackbarManager.showMessage("Некоректний формат Email адреси")
    return
  }

  apartmentService.updateBti(
    ApartmentEntity(
      addressId = _contactUiState.value.addressId,
      address = _contactUiState.value.address,
      phone = _contactUiState.value.phone,
      email = _contactUiState.value.email,
      uid = uid
    )
  ).onEach { result ->
    when (result) {
      is Resource.Success -> {
        SnackbarManager.showMessage("Дані БТІ успішно оновлено")
        getApartment(_apartmentUiState.value.addressId) // Перезапуск каскадного сбора
      }

      is Resource.Error -> {
        SnackbarManager.showMessage(result.message ?: "Помилка оновлення даних")
      }

      is Resource.Loading -> {
        // Опциональный лоадер изменения контактов
      }
    }
  }.launchIn(screenModelScope) // ИСПРАВЛЕНО: screenModelScope
}

private var lastProcessingAddressId: Long = -1L

/**
 * [getApartment] — Загрузка детальной информации по конкретной квартире.
 * ИСПРАВЛЕНО: addressId переведен на Long, атомарные логи переписаны на KMP println()
 */
fun getApartment(addressId: Long = uiState.value.addressId) {
  val methodName = "getApartment"
  if (addressId <= 0L) return

  val state = _apartmentUiState.value

  // УМНЫЙ ЗАМОК: Предотвращаем Race Condition и дублирование параллельных Ktor-сессий
  if (state.addressId == addressId && state.apartmentLoading) return
  if (state.addressId == addressId && state.apartment.addressId != 0L) {
    println("[$tag.$methodName($addressId)]: -> ATOMIC SKIP (Данные уже актуальны)")
    return
  }

  println("[$tag.$methodName]: [FORCE_FETCH] Загрузка о/р $addressId из биллинга города Южный")
  lastProcessingAddressId = addressId
  val currentUid = uid ?: ""

  apartmentService.getApartment(addressId = addressId, uid = currentUid).onEach { result ->
    when (result) {
      is Resource.Success -> {
        val data = result.data ?: ApartmentEntity()
        val currentUserRole = _apartmentUiState.value.userRole
        val isStandardUser = currentUserRole == UserRole.StandardUser

        _apartmentUiState.update { currentState ->
          currentState.copy(
            apartment = data,
            addressId = data.addressId,
            address = data.address,
            // Фиксируем и защищаем системные ID служб организации (Vodokanal 9999L, Ytke 9998L)
            osbbId = if (currentState.osbbId > 9000L) currentState.osbbId else data.osmdId,
            osmdId = if (currentState.osmdId > 9000L) currentState.osmdId else data.osmdId,
            apartmentLoading = false
          )
        }

        if (isStandardUser) {
          // ЛОГИКА ЖИЛЬЦА: привязка чат-веток к лицевому счету и Firestore-профилю
          val combinedName = "${data.address} | ${data.nanim ?: ""}"
          println("[$tag.$methodName]: [RESIDENT_SYNC] Профиль и токен чатов жильца синхронизированы")

          // chatScreenModel.subscribeToAllMyApartments(currentUid, data.osmdId, listOf(data.addressId))

          firebaseService.updateUserRoleAndPermissions(
            uid = currentUid,
            addressId = data.addressId,
            userRole = currentUserRole,
            osbbId = data.osmdId,
            displayName = combinedName
          )
        } else {
          // ЛОГИКА АДМИНИСТРАТОРА: Только просмотр. Никаких перезаписей Firestore профиля!
          // Это гарантирует, что имя админа не затрется фиктивным ФИО жильца квартиры.
          println("[$tag.$methodName]: [ADMIN_VIEW] Просмотр о/р ${data.addressId} завершен. Личный профиль защищен.")
        }
      }

      is Resource.Error -> {
        println("[$tag.$methodName]: -> ERROR: ${result.message}")
        _apartmentUiState.update{ it.copy(apartmentLoading = false) }
      }

      is Resource.Loading -> {
        _apartmentUiState.update{ it.copy(apartmentLoading = true) }
      }
    }
  }.launchIn(screenModelScope)
}

/**
 * [getApartmentList] — Загрузка списка всех привязанных квартир пользователя.
 * ИСПРАВЛЕНО: addressId переведен на Long, логика подмены ОСББ адаптирована к КМР.
 */
fun getApartmentList(onSuccess: () -> Unit = {}) {
  val currentUid = firebaseService.uid ?: ""
  if (currentUid.isEmpty()) return

  apartmentService.getApartmentList(currentUid).onEach { result ->
    _apartmentUiState.update { state ->
      when (result) {
        is Resource.Success -> {
          val newList = result.data ?: emptyList()
          // Если в текущей сессии ничего не выбрано (ID=0), инициализируем граф первой квартирой
          if (state.addressId == 0L && newList.isNotEmpty()) {
            val first = newList.first()
            state.copy(
              apartments = newList,
              apartmentLoading = true,
              addressId = first.addressId,
              address = first.address,
              osbb = first.osbb.toString(),
              osbbId = first.osmdId,
              apartment = first,
              mainLoading = false
            )
          } else {
            state.copy(
              apartments = newList,
              mainLoading = false
            )
          }
        }

        is Resource.Error -> state.copy(error = result.message ?: "Error", mainLoading = false)
        is Resource.Loading -> state.copy(mainLoading = true)
      }
    }
    if (result is Resource.Success) onSuccess()
  }.launchIn(screenModelScope)
}

/**
 * [deleteApartment] — Удаление привязки лицевого счета жильца из системы ЮКИС.
 * ИСПРАВЛЕНО: Все ID переведены на Long, вызовы Room удалены, корутины переведены на screenModelScope.
 */
fun deleteApartment(onNavigate: (String) -> Unit) {
  val methodName = "deleteApartment"

  // 1. СИСТЕМНЫЙ ЗАХВАТ: Фиксируем текущие ID до асинхронного старта корутины
  val captureAddressId = _apartmentUiState.value.addressId
  val captureUid = firebaseService.uid ?: ""
  val captureOsbbId = _apartmentUiState.value.osbbId

  println("[$tag.$methodName]: [START] Запуск удаления. ID=$captureAddressId, UID=$captureUid, OSBB=$captureOsbbId")

  if (captureAddressId == 0L) {
    println("[$tag.$methodName]: [ABORT] captureAddressId равен 0")
    return
  }

  apartmentService.deleteApartment(addressId = captureAddressId, uid = captureUid)
    .onEach { result ->
      when (result) {
        is Resource.Loading -> {
          println("[$tag.$methodName]: [LOADING]")
          _apartmentUiState.update { it.copy(mainLoading = true) }
        }

        is Resource.Success -> {
          println("[$tag.$methodName]: [SUCCESS] MySQL биллинг г. Южный очищен. Переходим к Firebase.")

          // 2. ОЧИСТКА ЧАТ-ПОТОКОВ (Передаем предварительно захваченные Long-значения)
          println("[$tag.$methodName]: [CHAT_CLEAN] Очистка веток обсуждений для счета $captureAddressId")
          // chatScreenModel.deleteChatThreads(uid = captureUid, osbbId = captureOsbbId, addressId = captureAddressId)

          // 3. СБРОС ПРАВ В FIRESTORE (Отвязываем пользователя от коммунального дома)
          println("[$tag.$methodName]: [FIRESTORE_CLEAN] Сброс роли и osbbId в Firestore")
          firebaseService.updateUserRoleAndPermissions(
            uid = captureUid,
            addressId = 0L,
            userRole = UserRole.StandardUser,
            osbbId = 0L,
            displayName = firebaseService.currentUser?.displayName ?: "Користувач"
          )

          SnackbarManager.showMessage("Особовий рахунок успішно видалено")

          // Обнуляем текущий ID в стейте перед каскадным обновлением списка
          _apartmentUiState.update { it.copy(addressId = 0L) }

          // Обновляем список оставшихся лицевых счетов
          getApartmentList {
            val updatedList = _apartmentUiState.value.apartments
            println("[$tag.$methodName]: [UPDATE] Осталось квартир в базе данных: ${updatedList.size}")

            if (updatedList.isEmpty()) {
              println("[$tag.$methodName]: [NAVIGATE] Квартир больше нет, маршрутизация на экран привязки")
              _apartmentUiState.update {
                it.copy(
                  address = "",
                  apartment = ApartmentEntity(),
                  mainLoading = false
                )
              }
              onNavigate("ADD_APARTMENT_ROUTE") // Маршрут экрана Voyager AddApartmentScreen
            } else {
              val nextApartment = updatedList.first()
              println("[$tag.$methodName]: [SWITCH] Автоматический переход на следующий счет ID: ${nextApartment.addressId}")
              setAddressId(nextApartment.addressId)
              _apartmentUiState.update { it.copy(mainLoading = false) }
            }
          }
        }

        is Resource.Error -> {
          println("[$tag.$methodName]: [ERROR] ${result.message}")
          _apartmentUiState.update{ it.copy(mainLoading = false) }
          SnackbarManager.showMessage(result.message ?: "Помилка видалення особового рахунку")
        }
      }
    }.launchIn(screenModelScope) // ИСПРАВЛЕНО: launchIn переведен на screenModelScope Voyager
}

/**
 * [setAddressId] — Атомарное переключение активного лицевого счета БТИ в UI-стейтах.
 * ИСПРАВЛЕНО: addressId переведен на Long, корутины привязаны к screenModelScope.
 */
fun setAddressId(addressId: Long) {
  val methodName = "setAddressId"
  val currentState = _apartmentUiState.value
  val isResident = currentState.userRole == UserRole.StandardUser
  println("[$tag.$methodName]: [START] Поиск и активация ID: $addressId")

  // УМНЫЙ ПОИСК: Каскадный поиск объекта квартиры в основном списке БТИ или в Drawer-списке админа
  val target = currentState.apartments.find { it.addressId == addressId }
    ?: _drawerApartments.value.find { it.addressId == addressId }

  if (target != null) {
    // ИСПРАВЛЕНО: Системная проверка ID организации (Long)
    val finalOsbbId = if (currentState.osbbId > 9000L) currentState.osbbId else target.osmdId
    val combinedName = "${target.address} | ${target.nanim ?: ""}"
    val rawOsbbFromDb = target.osbb

    println("[$tag.$methodName]: [DATA_CHECK] Из базы данных SQLDelight получено имя ОСББ: '$rawOsbbFromDb'")

    // Динамическое формирование имени кнопки для экрана выбора чатов
    val finalOsbbDisplayName = if (rawOsbbFromDb.isNullOrBlank() || rawOsbbFromDb == "0") {
      "Мій ОСББ"
    } else {
      rawOsbbFromDb
    }
    println("[$tag.$methodName]: [MATCH_FOUND] ${target.address} | Название ОСББ: $finalOsbbDisplayName")

    _apartmentUiState.update { state ->
      state.copy(
        addressId = target.addressId,
        apartment = target,
        address = target.address,
        osbb = finalOsbbDisplayName, // Передаем форматированное имя в стейт
        houseId = target.houseId,
        displayName = if (isResident) combinedName else state.displayName,
        osbbId = finalOsbbId,
        osmdId = finalOsbbId,
        apartmentLoading = false
      )
    }

    // 2. СИНХРОНИЗАЦИЯ ПРАВ И БЕЙДЖЕЙ
    if (isResident) {
      // ИСПРАВЛЕНО: Корутина запускается на screenModelScope Voyager
      screenModelScope.launch {
        firebaseService.updateUserRoleAndPermissions(
          uid = currentState.uid ?: "",
          addressId = target.addressId,
          userRole = currentState.userRole,
          osbbId = finalOsbbId,
          displayName = combinedName
        )
      }
      println("[$tag.$methodName]: [USER_SYNC] Обновление подписок на бэйджи уведомлений квартир жильца")
      // chatScreenModel.subscribeToAllMyApartments(uid = currentState.uid ?: "", osbbId = finalOsbbId, apartments = currentState.apartments.map { it.addressId })
    } else {
      println("[$tag.$methodName]: [ADMIN_MODE] Просмотр в режиме администрации. Личный профиль защищен.")
      // chatScreenModel.trackUserIdentifiersWithRole(currentState.userRole, finalOsbbId)
    }
    println("[$tag.$methodName]: [SUCCESS] State о/р успешно обновлен. Текущий AddressId: $addressId | OSBB: $finalOsbbDisplayName")

  } else {
    println("[$tag.$methodName]: [WARNING] Объект БТИ не найден в памяти. Принудительная установка ID.")
    _apartmentUiState.update { it.copy(addressId = addressId) }
  }
}
}

