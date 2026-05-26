package com.ykis.ykismobkmp.ui.screens.appartment

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val tag = "ApartmentScreenModel"

// Режимы вложенности списков (Районы г. Южное -> Дома -> Квартиры жителей)
enum class ListMode { RAIONS, HOUSES, APARTMENTS }

/**
 * [ApartmentScreenModel] — Кроссплатформенная модель управления привязкой лицевых счетов и админ-панелей ОСМД.
 * ИСПРАВЛЕНО (ЧАСТЬ 1): Интегрирован блок init сброса лоадера для SMS-входа,
 * префиксы логирования приведены под сквозной стандарт [YkisLogKMP].
 */
class ApartmentScreenModel(
  val firebaseService: FirebaseService,
  private val apartmentService: ApartmentService,
  logService: LogService
) : BaseScreenModel(logService) {
  private val className = "ApartmentScreenModel"
  private val isEmailVerified get() = firebaseService.currentUser?.isEmailVerified ?: false
  val uid get() = firebaseService.uid

  private val displayName get() = firebaseService.displayName
  val email get() = firebaseService.email

  private var observeJob: Job? = null
  private var isHandlingResult = false

  private val _apartment = MutableStateFlow(ApartmentEntity())
  val apartment: StateFlow<ApartmentEntity> get() = _apartment.asStateFlow()

  private var isObservingStarted = false
  private val _secretCode = MutableStateFlow("")
  val secretCode: StateFlow<String> = _secretCode.asStateFlow()
  var lastLoadedAddressId: Long = 0L
    private set

  private var lastHandledResultId: Long? = null

  // LaunchScreen
  private val _showError = MutableStateFlow(false)
  val showError: StateFlow<Boolean> = _showError.asStateFlow()

  val authState = firebaseService.getAuthState(screenModelScope)

  private val _drawerHouses = MutableStateFlow<List<HouseEntity>>(emptyList())
  val drawerHouses = _drawerHouses.asStateFlow()

  private val _drawerApartments = MutableStateFlow<List<ApartmentEntity>>(emptyList())
  val drawerApartments = _drawerApartments.asStateFlow()

  private val _drawerLoading = MutableStateFlow(false)
  val drawerLoading = _drawerLoading.asStateFlow()

  private val _contactUiState = MutableStateFlow(BaseUIState())
  val contactUIState: StateFlow<BaseUIState> = _contactUiState.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()

  private val _apartmentUiState = MutableStateFlow(BaseUIState())
  val baseUIState: StateFlow<BaseUIState> = _apartmentUiState.asStateFlow()

  // ГАРАНТИЯ СОВМЕСТИМОСТИ С ЭКРАНОМ: Направляем uiState на наш главный КМР-поток БТИ

  init {
    println("[YkisLogKMP.$className.init]: Ініціалізація ApartmentScreenModel в КМР шарі.")
    // Принудительно выключаем стартовую блокировку экрана загрузки, если пользователь зашел по SMS
    disableMainLoading()
  }

  /**
   * [disableMainLoading] — Принудительное гашение лоадера жилого фонда биллинга.
   */
  fun disableMainLoading() {
    _apartmentUiState.update { it.copy(mainLoading = false) }
    println("[YkisLogKMP.$className.disableMainLoading]: Глобальний лоадер БТИ примусово переведено в FALSE")
  }

  /**
   * [filteredApartments] — Адаптивный КМР-поток реактивной фильтрации списков БТИ при вводе в поисковую строку.
   */
  val filteredApartments: StateFlow<List<ApartmentEntity>> = combine(
    _searchQuery,
    _apartmentUiState,
    _drawerHouses,
    _drawerApartments
  ) { query, state, houses, drApts ->
    if (query.isEmpty()) return@combine emptyList()

    when (state.listMode) {
      ListMode.HOUSES -> {
        houses.filter { it.house.contains(query, ignoreCase = true) }
          .map { ApartmentEntity(address = it.house, addressId = it.houseId) }
      }

      ListMode.APARTMENTS -> {
        val source =
          if (state.userRole != UserRole.StandardUser && state.userRole != UserRole.OsbbUser) {
            drApts
          } else {
            state.apartments
          }

        source.filter {
          it.address.contains(query, ignoreCase = true) ||
            (it.nanim?.contains(query, ignoreCase = true) ?: false) ||
            it.addressId.toString().contains(query)
        }
      }

      ListMode.RAIONS -> emptyList()
    }
  }.stateIn(
    scope = screenModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun onSearchQueryChanged(newQuery: String) {
    _searchQuery.value = newQuery
  }

  /**
   * [clearState] — Безопасное точечное очищение КМР-графа состояний жилого фонда БТИ ЮКИС.
   * Он сохраняет текущего авторизованного пользователя, очищая только таблицы адресов в ОЗУ.
   */
  fun clearState() {
    val methodName = "clearState"
    println("[YkisLogKMP.$className.$methodName]: [FORCE_RESET] Повна очистка графу привязки особових рахунків БТІ.")

    // Каскадно отменяем активные фоновые корутин-потоки прослушивания Firestore
    observeJob?.cancel()
    lastLoadedAddressId = -1L

    _apartmentUiState.update { currentState ->
      // ИСПРАВЛЕНО: Не создаем пустой класс с нуля, а точечно модифицируем текущий стейт через .copy(),
      // бережно сохраняя uid и роль залогиненного по Google/SMS жителя города Южного!
      currentState.copy(
        // Сбрасываем коммунальные идентификаторы СУБД к дефолтным нулям
        addressId = 0L,
        address = "",
        kod = "",
        apartments = emptyList(),
        apartment = ApartmentEntity(),
        isApartmentsLoaded = false,

        // Системные лоадеры Ktor
        mainLoading = false, // Принудительно гасим лоадер для мгновенного раскрытия формы БТИ
        isLoading = false,
        isGlobalLoading = false,
        apartmentLoading = false,

        // Очищаем ошибки прошлых сетевых сессий
        error = null
      )
    }

    println("[YkisLogKMP.$className.$methodName]: Пам'ять ОЗУ успішно очищена. Активний UID: \"${_apartmentUiState.value.uid}\"")
  }



  /**
   * [onRaionSelected] — Обработка выбора Района в Dropdown (г. Южное / Одесская область).
   */
  fun onRaionSelected(raion: RaionEntity) {
    val methodName = "onRaionSelected"
    val raionIdLong = raion.raionId ?: 0L

    println("[YkisLogKMP.$className.$methodName]: [START] Выбран Район: ${raion.raion} (ID: $raionIdLong)")

    _apartmentUiState.update {
      it.copy(
        selectedRaionId = raionIdLong,
        listMode = ListMode.HOUSES
      )
    }

    _drawerLoading.value = true

    screenModelScope.launch {
      apartmentService.getHouseList(raionIdLong).collect { result ->
        when (result) {
          is Resource.Success -> {
            val houses = result.data ?: emptyList()
            println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Домов загружено в Drawer: ${houses.size}")

            _drawerHouses.value = houses
            _drawerLoading.value = false
          }

          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
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

  /**
   * [goBackLevel] — Навигация назад по уровням вложенности списков ЖКХ-фонда.
   */
  fun goBackLevel() {
    val methodName = "goBackLevel"

    _apartmentUiState.update { state ->
      val newMode = when (state.listMode) {
        ListMode.APARTMENTS -> {
          println("[YkisLogKMP.$className.$methodName]: Возврат к списку домов")
          ListMode.HOUSES
        }

        ListMode.HOUSES -> {
          println("[YkisLogKMP.$className.$methodName]: Возврат к списку районов")
          _drawerHouses.value = emptyList()
          ListMode.RAIONS
        }

        ListMode.RAIONS -> ListMode.RAIONS
      }
      state.copy(listMode = newMode)
    }
  }

  /**
   * [onHouseSelected] — Асинхронный КМР-сбор списка квартир выбранного дома расчетного центра.
   */
  fun onHouseSelected(houseId: Long) {
    val methodName = "onHouseSelected"
    println("[YkisLogKMP.$className.$methodName]: [START] Завантаження квартир для будинку ID: $houseId")

    _drawerLoading.value = true
    _apartmentUiState.update {
      it.copy(
        selectedHouseId = houseId,
        listMode = ListMode.APARTMENTS
      )
    }

    screenModelScope.launch {
      apartmentService.getOsbbApartmentsList(houseId).collect { result ->
        when (result) {
          is Resource.Success -> {
            val apartments = result.data ?: emptyList()
            println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Отримано: ${apartments.size} кв.")

            _drawerApartments.value = apartments
            _drawerLoading.value = false
          }

          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
            _drawerLoading.value = false

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

  /**
   * [onSecretCodeChanged] — Реактивное обновление стейта вводимого секретного кода БТИ.
   */
  fun onSecretCodeChanged(newCode: String) {
    _secretCode.value = newCode
  }


  /**
   * [onAppStart] — Маршрутизатор точки входа в приложение ЮКИС.
   * Заменяет логику роутинга на КМР строковые дескрипторы графов навигации Voyager.
   */
  fun onAppStart(): String {
    val methodName = "onAppStart"
    val userExists = firebaseService.hasUser
    val emailVerified = firebaseService.isEmailVerified
    println("[YkisLogKMP.$className.$methodName]: UserExists=$userExists, Verified=$emailVerified")

    return if (userExists && emailVerified == true) {
      if (_apartmentUiState.value.uid != null && _apartmentUiState.value.uid != firebaseService.uid) {
        println("[YkisLogKMP.$className.$methodName]: [WARNING] Виявлено конфлікт UID в пам'яті! Очищення стейту.")
        clearState()
      }
      "APARTMENT_GRAPH"
    } else {
      clearState()
      "AUTHENTICATION_GRAPH"
    }
  }

  fun observeUserProfile() {
    val methodName = "observeUserProfile"
    val actualUid = firebaseService.uid ?: run {
      println("[YkisLogKMP.$className.$methodName]: [ABORT] UID is null")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [START] actualUid: $actualUid")
    observeJob?.cancel()

    observeJob = screenModelScope.launch {
      _apartmentUiState.update { it.copy(mainLoading = true) }
      try {
        // ПОЛУЧЕННЯ ПРОФІЛЮ (Свіжі КМР дані з Firebase)
        val user = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
          firebaseService.getUserProfile()
        }

        val currentUserRole = UserRole.fromString(user.userRole)

        // Присвоюємо системні фіксовані ID для комунальних організацій міста Южне
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

        println("[YkisLogKMP.$className.$methodName]: [PROFILE_LOADED] Role: $currentUserRole, ID: $currentOsbbId")

        _apartmentUiState.update {
          it.copy(
            uid = user.uid,
            userRole = currentUserRole,
            osbbId = currentOsbbId,
            osmdId = currentOsbbId,
            displayName = user.name ?: ""
          )
        }

        // Каскадний КМР вибір логіки біллінгу на основі ролі аккаунта
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
              // Резервний шлюз для інших комунальних підприємств м. Южне
            }
          }
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [FATAL ERROR] ${e.message}")
        _apartmentUiState.update { it.copy(mainLoading = false) }
      }
    }
  }


  /**
   * [handleOsbbAdminResult] — Обработка ответа сервера при получении списка квартир администратором ОСМД.
   */
  private suspend fun handleOsbbAdminResult(
    result: Resource<List<ApartmentEntity>>,
    uid: String,
    role: UserRole,
    osbbId: Long,
    name: String?
  ) {
    val methodName = "handleOsbbAdminResult"

    _apartmentUiState.update { state ->
      when (result) {
        is Resource.Success -> {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Завантажено ${result.data?.size} кв. для ОСББ ID: $osbbId")
          state.copy(
            apartments = result.data ?: emptyList(),
            listMode = ListMode.APARTMENTS,
            mainLoading = false
          )
        }

        is Resource.Error -> {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбій завантаження списку адміна: ${result.message}")
          state.copy(mainLoading = false)
        }

        is Resource.Loading -> {
          state.copy(mainLoading = true)
        }
      }
    }

    if (result is Resource.Success) {
      firebaseService.updateUserRoleAndPermissions(
        uid = uid,
        addressId = 0L,
        userRole = role,
        osbbId = osbbId,
        displayName = name
      )
    }
  }

  // ====================================================================
  // --- ДОБАВЛЕНО: МЕТОД ВЕРИФИКАЦИИ И ПРИВЯЗКИ ЛИЦЕВЫХ СЧЕТОВ БТИ ---
  // ====================================================================

  /**
   * [addApartment] — Отправка инфо-кода ГИОЦ/БТИ на сервер Ktor и привязка жилья.
   * Требуется экраном AddApartmentScreen для бесшовной компиляции рантайма.
   */



  /**
   * [handleStandardUserResult] — Обробка результату завантаження прив'язаного житлового фонду звичайного мешканця.
   */
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
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Отримано квартир: ${apartments.size}")

          if (apartments.isNotEmpty()) {
            apartments.forEachIndexed { index, apt ->
              println("[YkisLogKMP.$className.$methodName]: [LIST_ITEM] #$index: ID=${apt.addressId}, Адрес=${apt.address}, OSBB_RAW='${apt.osbb}'")
            }

            val target = apartments.find { it.addressId == state.addressId } ?: apartments.first()
            println("[YkisLogKMP.$className.$methodName]: [TARGET_SELECT] Обрано ID=${target.addressId} (Поточний в стейті був: ${state.addressId})")

            val combinedName = "${target.address} | ${target.nanim ?: ""}"
            val rawOsbb = target.osbb?.toString()

            val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") {
              "Мій ОСББ"
            } else {
              rawOsbb
            }
            println("[YkisLogKMP.$className.$methodName]: [FINAL_OSBB] Встановлюємо в UI: '$finalOsbbName' (із сирого: '$rawOsbb')")

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
            println("[YkisLogKMP.$className.$methodName]: [WARNING] Список квартир порожній")
            state.copy(mainLoading = false, isApartmentsLoaded = true)
          }
        }

        is Resource.Error -> {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
          state.copy(mainLoading = false)
        }

        is Resource.Loading -> state.copy(mainLoading = true)
      }
    }
  }

  /**
   * [handleOrganizationResult] — Обробка списку доступних районів Одеської області для комунальних служб Южного.
   */
  private suspend fun handleOrganizationResult(
    result: Resource<List<RaionEntity>>,
    uid: String,
    role: UserRole,
    osbbId: Long,
    name: String?
  ) {
    val methodName = "handleOrganizationResult"
    _apartmentUiState.update { state ->
      when (result) {
        is Resource.Success -> {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Завантажено ${result.data?.size} районів Одеської обл.")
          state.copy(
            raions = result.data ?: emptyList(),
            listMode = ListMode.RAIONS,
            mainLoading = false
          )
        }

        is Resource.Error -> state.copy(mainLoading = false)
        is Resource.Loading -> state.copy(mainLoading = true)
      }
    }
    if (result is Resource.Success) {
      firebaseService.updateUserRoleAndPermissions(uid, 0L, role, osbbId, name)
    }
  }

  /**
   * [resetToAdminMode] — Повернення до панелі адміністрування та моніторингу житлового фонду.
   */
  fun resetToAdminMode() {
    val methodName = "resetToAdminMode"
    println("[YkisLogKMP.$className.$methodName]: [RESET] Повернення до адміністрування комунального фонду")
    _apartmentUiState.update {
      it.copy(
        apartments = emptyList(),
        addressId = 0L,
        address = "",
        mainLoading = true
      )
    }
    observeUserProfile()
  }


  /**
   * [addApartment] — Точка обробки введення інфо-кодів. Валідація ГІОЦ та ОСББ г. Южне.
   */
  fun addApartment(restartApp: () -> Unit) {
    val methodName = "addApartment"
    val input = _secretCode.value.trim() // Считываем значение из реактивного потока
    if (input.isEmpty()) return

    println("[YkisLogKMP.$className.$methodName]: Клік по кнопці. Введено код БТІ: $input")
    val uid = firebaseService.uid ?: return
    val email = firebaseService.email ?: ""

    if (input.all { it.isDigit() }) {
      // --- ЛОГІКА ЖИЛЬЦА ЮЖНОГО ---
      apartmentService.addApartment(input, uid, email).onEach { result ->
        handleApartmentResult(uid, result, restartApp)
      }.launchIn(screenModelScope)
    } else {
      // --- ЛОГІКА АДМИНИСТРАТОРА ОСМД ---
      apartmentService.verifyAdminCode(input, uid).onEach { result ->
        handleAdminResult(result, restartApp)
      }.launchIn(screenModelScope)
    }
  }

  /**
   * [handleApartmentResult] — Обробка відповіді Ktor-сервера при успішній прив'язці інфо-коду ГІОЦ звичайним жильцем Южного.
   */
  private suspend fun handleApartmentResult(
    uid: String,
    result: Resource<GetSimpleResponse>,
    restartApp: () -> Unit
  ) {
    val methodName = "handleApartmentResult"
    if (isHandlingResult && result !is Resource.Loading) {
      println("[YkisLogKMP.$className.$methodName]: [SKIP] Повторний виклик блоковано запобіжником")
      return
    }

    when (result) {
      is Resource.Loading -> {
        println("[YkisLogKMP.$className.$methodName]: [LOADING]")
        _apartmentUiState.update { it.copy(mainLoading = true) }
      }

      is Resource.Success -> {
        isHandlingResult = true
        val data = result.data ?: run {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Мережева відповідь порожня")
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
        println("[YkisLogKMP.$className.$methodName]: [DATA_PARSE] ID: $newAddressId, OSBB_RAW: '$rawOsbbName', FINAL: '$finalOsbbName'")

        try {
          // 2. СИНХРОНИЗАЦИЯ С ОБЛАКОМ (Firestore)
          println("[YkisLogKMP.$className.$methodName]: [STEP 1] Фіксація прав у Firestore. Адреса: $newAddress")
          firebaseService.updateUserRoleAndPermissions(
            uid = uid,
            addressId = newAddressId,
            userRole = UserRole.StandardUser,
            osbbId = newOsbbId,
            displayName = newAddress
          )

          // 3. ОБНОВЛЕНИЕ UI STATE И ЧАТ-КАНАЛОВ
          println("[YkisLogKMP.$className.$methodName]: [STEP 2] Встановлення стейту. Прив'язка під контроль OSBB: $finalOsbbName")
          _apartmentUiState.update {
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
            println("[YkisLogKMP.$className.$methodName]: [STEP 3] Ініціалізація резидент-чатів для особового рахунку $newAddressId")
            // chatScreenModel.initResidentChats(uid, newOsbbId, newAddressId, newAddress, "")
          }

          _secretCode.value = ""
          SnackbarManager.showMessage("Особовий рахунок успішно прив'язано")

          println("[YkisLogKMP.$className.$methodName]: Очікування стабілізації контексту...")
          kotlinx.coroutines.delay(500)
          isHandlingResult = false

          println("[YkisLogKMP.$className.$methodName]: [FINISH] Перезапуск графа навігації (restartApp)")
          restartApp()

        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: [CRITICAL ERROR] ${e.message}")
          isHandlingResult = false
          _apartmentUiState.update { it.copy(mainLoading = false) }
          SnackbarManager.showMessage("Помилка синхронізації профілю квартири")
        }
      }

      is Resource.Error -> {
        println("[YkisLogKMP.$className.$methodName]: [API ERROR] ${result.message}")
        isHandlingResult = false
        _apartmentUiState.update { it.copy(mainLoading = false) }
        SnackbarManager.showMessage(result.message ?: "Помилка додавання особового рахунку")
      }
    }
  }

  /**
   * [handleAdminResult] — Обробка результату при введенні секретного слова доступу керівника або диспетчера служб Южного.
   */
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

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Секретне слово прийнято. Роль: $mappedRole, ID Службы: $newOsbbId")

        screenModelScope.launch {
          try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
              println("[YkisLogKMP.$className.$methodName]: [STEP 1] Запис токена адміна в Firestore")
              firebaseService.updateUserRoleAndPermissions(
                uid = currentUid,
                addressId = 0L,
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

            println("[YkisLogKMP.$className.$methodName]: [STEP 2] Перезапуск трекерів диспетчеризації")
            observeUserProfile()
            // chatScreenModel.trackUserIdentifiersWithRole(mappedRole, newOsbbId)

            _secretCode.value = ""
            SnackbarManager.showMessage("Авторизація адміністратора успішна")
            restartApp()

          } catch (e: Exception) {
            println("[YkisLogKMP.$className.$methodName]: [CRITICAL ERROR] ${e.message}")
            _apartmentUiState.update { it.copy(mainLoading = false) }
            SnackbarManager.showMessage("Помилка авторизації прав доступу")
          }
        }
      }

      is Resource.Error -> {
        println("[YkisLogKMP.$className.$methodName]: [API ERROR] ${result.message}")
        SnackbarManager.showMessage(result.message ?: "Невірне секретне слово доступу")
      }

      is Resource.Loading -> {
        _apartmentUiState.update { it.copy(mainLoading = true) }
      }
    }
  }


// КМР Regex-паттерн валидации электронной почты без привязки к android.util.Patterns
  /**
   * [isValidEmailKmp] — Локальна КМР-валідація регулярного виразу пошти для анкети БТІ.
   */
  private fun String.isValidEmailKmp(): Boolean {
    val emailRegex = "^[A-Za-is0-9_+=%.&-]+@[A-Za-is0-9.-]+\\.[a-zA-is]{2,}\$"
    return this.matches(emailRegex.toRegex())
  }

  /**
   * [initialContactState] — Ініціалізація локального КМР-стану контактів абонента БТІ міста Южне.
   */
  fun initialContactState() {
    val methodName = "initialContactState"
    val currentState = _apartmentUiState.value

    println("[YkisLogKMP.$className.$methodName]: [START] Фіксація знімку анкетних даних БТІ для о/р ${currentState.addressId}")

    _contactUiState.value = BaseUIState(
      email = currentState.apartment.email ?: "",
      phone = currentState.apartment.phone ?: "",
      addressId = currentState.addressId,
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
   * [onUpdateBti] — Оновлення контактних даних абонента БТІ г. Южне на сервері Ktor.
   */
  fun onUpdateBti(uid: String) {
    val methodName = "onUpdateBti"
    val currentEmail = _contactUiState.value.email ?: ""

    if (!currentEmail.isValidEmailKmp() && currentEmail.isNotEmpty()) {
      SnackbarManager.showMessage("Некоректний формат Email адреси")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [START] Надсилання нових контактів БТІ на сервер ЮКІС...")

    apartmentService.updateBti(
      addressId = _contactUiState.value.addressId,
      phone = _contactUiState.value.phone.toString(),
      email = _contactUiState.value.email.toString()
    ).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Дані БТІ успішно оновлені на сервері та в СУБД")
          SnackbarManager.showMessage("Дані БТІ успішно оновлено")
          getApartment(_contactUiState.value.addressId)
        }

        is Resource.Error -> {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій оновлення БТІ: ${result.message}")
          SnackbarManager.showMessage(result.message ?: "Помилка оновлення даних")
        }

        is Resource.Loading -> {
          println("[YkisLogKMP.$className.$methodName]: [LOADING] Синхронізація анкети БТІ з сервером ЮКІС...")
        }
      }
    }.launchIn(screenModelScope)
  }

  private var lastProcessingAddressId: Long = -1L

  /**
   * [getApartment] — Завантаження детальної інформації по конкретній квартирі з біллінгу.
   */
  fun getApartment(addressId: Long = _apartmentUiState.value.addressId) {
    val methodName = "getApartment"
    if (addressId <= 0L) return

    val state = _apartmentUiState.value

    if (state.addressId == addressId && state.apartmentLoading) return
    if (state.addressId == addressId && state.apartment.addressId != 0L) {
      println("[YkisLogKMP.$className.$methodName($addressId)]: -> ATOMIC SKIP (Дані вже актуальні в ОЗУ)")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [FORCE_FETCH] Завантаження о/р $addressId з біллінгу міста Южне")

    observeJob?.cancel()
    lastLoadedAddressId = -1L

    val currentUid = firebaseService.uid
    lastLoadedAddressId = addressId

    observeJob = apartmentService.getApartment(addressId = addressId, uid = currentUid).onEach { result ->
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
              osbbId = if (currentState.osbbId > 9000L) currentState.osbbId else data.osmdId,
              osmdId = if (currentState.osmdId > 9000L) currentState.osmdId else data.osmdId,
              apartmentLoading = false
            )
          }

          if (isStandardUser) {
            val combinedName = "${data.address} | ${data.nanim ?: ""}"
            println("[YkisLogKMP.$className.$methodName]: [RESIDENT_SYNC] Профіль та токен чатів мешканця успішно синхронізовано")

            firebaseService.updateUserRoleAndPermissions(
              uid = currentUid,
              addressId = data.addressId,
              userRole = currentUserRole,
              osbbId = data.osmdId,
              displayName = combinedName
            )
          } else {
            println("[YkisLogKMP.$className.$methodName]: [ADMIN_VIEW] Перегляд о/р ${data.addressId} завершено. Особистий профіль голови ОСББ захищено.")
          }
        }

        is Resource.Error -> {
          println("[YkisLogKMP.$className.$methodName]: -> КРИТИЧНА ПОМИЛКА БІЛІНГУ КТOR: ${result.message}")
          _apartmentUiState.update { it.copy(apartmentLoading = false) }
          lastLoadedAddressId = -1L
        }

        is Resource.Loading -> {
          _apartmentUiState.update { it.copy(apartmentLoading = true) }
        }
      }
    }.launchIn(screenModelScope)
  }
  /**
   * [getApartmentList] — Завантаження повного списку прив'язаних квартир абонента з біллінгу.
   */
  fun getApartmentList(onSuccess: () -> Unit = {}) {
    val methodName = "getApartmentList"
    val currentUid = firebaseService.uid ?: ""
    if (currentUid.isEmpty()) return

    println("[YkisLogKMP.$className.$methodName]: [START] Фонове завантаження рахунків для UID: $currentUid")

    apartmentService.getApartmentList(currentUid).onEach { result ->
      _apartmentUiState.update { state ->
        when (result) {
          is Resource.Success -> {
            val newList = result.data ?: emptyList()
            if (state.addressId == 0L && newList.isNotEmpty()) {
              val first = newList.first()
              println("[YkisLogKMP.$className.$methodName]: [INIT] Первинна ініціалізація графу першим о/р ID: ${first.addressId}")
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
              println("[YkisLogKMP.$className.$methodName]: [UPDATE] Оновлено список рахунків мешканця: ${newList.size}")
              state.copy(
                apartments = newList,
                mainLoading = false
              )
            }
          }
          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій: ${result.message}")
            state.copy(error = result.message ?: "Error", mainLoading = false)
          }
          is Resource.Loading -> state.copy(mainLoading = true)
        }
      }
      if (result is Resource.Success) onSuccess()
    }.launchIn(screenModelScope)
  }

  /**
   * [deleteApartmentFromProfile] — Видалення особового рахунку з профілю абонента в базі даних ГІОЦ.
   */
  fun deleteApartmentFromProfile(addressId: Long, onNavigateToAddScreen: () -> Unit) {
    val methodName = "deleteApartmentFromProfile"
    val uid = firebaseService.uid
    if (addressId <= 0L) return

    launchCatching(showLoader = true) {
      println("[YkisLogKMP.$className.$methodName]: [START] Запит на видалення о/р $addressId для UID: $uid")
      apartmentService.deleteApartment(addressId, uid)
      apartmentService.getApartmentList(uid)

      val updatedList = _apartmentUiState.value.apartments
      println("[YkisLogKMP.$className.$methodName]: [UPDATE] Залишилося квартир в базі даних ГІОЦ: ${updatedList.size}")

      if (updatedList.isEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [NAVIGATE] Квартир більше немає, маршрутизація на екран прив'язку БТІ")
        _apartmentUiState.update { currentState ->
          currentState.copy(
            address = "",
            apartment = ApartmentEntity(),
            apartmentLoading = false
          )
        }
        onNavigateToAddScreen()
      } else {
        val nextApartment = updatedList.first()
        println("[YkisLogKMP.$className.$methodName]: [SWITCH] Автоматичний перехід на наступний рахунок ID: ${nextApartment.addressId}")
        setAddressId(nextApartment.addressId)
        _apartmentUiState.update { it.copy(apartmentLoading = false) }
      }
    }
  }

  /**
   * [setAddressId] — Пошук та активація обраного особового рахунку БТІ в оперативній пам'яті.
   */
  fun setAddressId(addressId: Long) {
    val methodName = "setAddressId"
    val currentState = _apartmentUiState.value
    val isResident = currentState.userRole == UserRole.StandardUser
    println("[YkisLogKMP.$className.$methodName]: [START] Пошук та активація ID: $addressId")

    val target = currentState.apartments.find { it.addressId == addressId }
      ?: _drawerApartments.value.find { it.addressId == addressId }

    if (target != null) {
      val finalOsbbId = if (currentState.osbbId > 9000L) currentState.osbbId else target.osmdId
      val combinedName = "${target.address} | ${target.nanim ?: ""}"
      val rawOsbbFromDb = target.osbb
      println("[YkisLogKMP.$className.$methodName]: [DATA_CHECK] З бази даних SQLDelight отримано ім'я ОСББ: '$rawOsbbFromDb'")

      val finalOsbbDisplayName = if (rawOsbbFromDb.isNullOrBlank() || rawOsbbFromDb == "0") {
        "Мій ОСББ"
      } else {
        rawOsbbFromDb
      }
      println("[YkisLogKMP.$className.$methodName]: [MATCH_FOUND] ${target.address} | Назва ОСББ: $finalOsbbDisplayName")

      _apartmentUiState.update { state ->
        state.copy(
          addressId = target.addressId,
          apartment = target,
          address = target.address,
          osbb = finalOsbbDisplayName,
          houseId = target.houseId,
          displayName = if (isResident) combinedName else state.displayName,
          osbbId = finalOsbbId,
          osmdId = finalOsbbId,
          apartmentLoading = false
        )
      }

      if (isResident) {
        screenModelScope.launch {
          firebaseService.updateUserRoleAndPermissions(
            uid = currentState.uid ?: "",
            addressId = target.addressId,
            userRole = currentState.userRole,
            osbbId = finalOsbbId,
            displayName = combinedName
          )
        }
        println("[YkisLogKMP.$className.$methodName]: [USER_SYNC] Оновлення підписок на бейджі сповіщень квартир мешканця")
      } else {
        println("[YkisLogKMP.$className.$methodName]: [ADMIN_MODE] Перегляд в режимі адміністрації. Особистий профіль захищено.")
      }
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] State о/р успішно оновлено. Поточний AddressId: $addressId | OSBB: $finalOsbbDisplayName")
    } else {
      println("[YkisLogKMP.$className.$methodName]: [WARNING] Об'єкт БТІ не знайдено в пам'яті. Примусове встановлення ID.")
      _apartmentUiState.update { it.copy(addressId = addressId) }
    }
  }
} // Абсолютний кінець і фінальне запечатування всього класу ApartmentScreenModel



