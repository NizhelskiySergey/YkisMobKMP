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


// Режимы вложенности списков (Районы г. Южное -> Дома -> Квартиры жителей)
enum class ListMode { RAIONS, HOUSES, APARTMENTS }

/**
 * [ApartmentScreenModel] — Кроссплатформенная модель управления привязкой лицевых счетов и админ-панелей ОСМД.
  */
class ApartmentScreenModel(
  val firebaseService: FirebaseService,
  private val apartmentService: ApartmentService,
  logService: LogService
) : BaseScreenModel(logService) {
  private val className = "ApartmentScreenModel"

  val uid get() = firebaseService.uid
  val email get() = firebaseService.email

  private var observeJob: Job? = null
  private var isHandlingResult = false
  private val _secretCode = MutableStateFlow("")
  val secretCode: StateFlow<String> = _secretCode.asStateFlow()
  private var lastHandledResultId: Long? = null
  // LaunchScreen
  private val _showError = MutableStateFlow(false)
//  val showError: StateFlow<Boolean> = _showError.asStateFlow()

//  val authState = firebaseService.getAuthState(screenModelScope)

  private val _drawerHouses = MutableStateFlow<List<HouseEntity>>(emptyList())
  val drawerHouses = _drawerHouses.asStateFlow()

  private val _drawerApartments = MutableStateFlow<List<ApartmentEntity>>(emptyList())
  val drawerApartments = _drawerApartments.asStateFlow()

  private val _drawerLoading = MutableStateFlow(false)
//  val drawerLoading = _drawerLoading.asStateFlow()

  private val _contactUiState = MutableStateFlow(BaseUIState())
  val contactUIState: StateFlow<BaseUIState> = _contactUiState.asStateFlow()

  private val _apartmentUiState = MutableStateFlow(BaseUIState())
  val apartmentUiState: StateFlow<BaseUIState> = _apartmentUiState.asStateFlow()
  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()


  init {
    println("[YkisLogKMP.$className.init]: Ініціалізація ApartmentScreenModel в КМР шарі.")
    disableMainLoading()
  }

  fun disableMainLoading() {
    _apartmentUiState.update { it.copy(mainLoading = false) }
    println("[YkisLogKMP.$className.disableMainLoading]: Глобальний лоадер БТИ примусово переведено в FALSE")
  }

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
    var calculatedTarget: ApartmentEntity? = null
    var calculatedCombinedName = ""

    _apartmentUiState.update { state ->
      when (result) {
        is Resource.Success -> {
          val incomingApartments = result.data ?: emptyList()
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Завантажено ${incomingApartments.size} кв. для ОСББ ID: $osbbId")

          // Исключаем дубликаты комнат на уровне ОЗУ по первичному ключу addressId
          val combinedApartments = (state.apartments + incomingApartments)
            .distinctBy { it.addressId }

          if (combinedApartments.isNotEmpty()) {
            // Стратегия выбора целевой квартиры: берем форсированную или самую первую из Use Case
            val target = when {
              state.addressId == 0L -> combinedApartments.first()
              else -> combinedApartments.find { it.addressId == state.addressId } ?: combinedApartments.first()
            }

            calculatedTarget = target
            calculatedCombinedName = "${target.address} | ${target.nanim ?: ""}"

            println("[YkisLogKMP.$className.$methodName]: [ADMIN_TARGET_SELECT] Фіксація адреси будинку ID=${target.addressId}")

            val rawOsbb = target.osbb?.toString()
            val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") {
              name ?: "Мій ОСББ"
            } else {
              rawOsbb
            }

            // Выполняем глубокую распаковку полей БТИ в корень стейта по стандарту жильца
            val finalNanim = target.nanim ?: "Власник не вказаний"
            val finalAreaFull = target.areaFull ?: target.areaFull?.toString() ?: "0.00"
            val finalAreaOtopl = target.areaOtopl ?: target.areaOtopl?.toString() ?: "0.00"
            val finalRoom = target.room ?: target.room?.toString() ?: "0"
            val finalTenantTbo = target.tenantTbo ?: target.tenantTbo?.toString() ?: "0"

            state.copy(
              apartments = combinedApartments, // Наполняем общий список для ApartmentNavigationRail
              apartment = target,             // Передаем объект целиком со всеми 20+ полями БТИ
              isApartmentsLoaded = true,
              listMode = ListMode.APARTMENTS,
              addressId = target.addressId,
              address = target.address,
              osbbId = target.osmdId,
              osbb = finalOsbbName,
              nanim = finalNanim,
              areaFull = finalAreaFull.toString(),
              areaOtopl = finalAreaOtopl.toString(),
              room = finalRoom.toString(),
              tenantTbo = finalTenantTbo.toString(),
              displayName = calculatedCombinedName,
              mainLoading = false
            )
          } else {
            println("[YkisLogKMP.$className.$methodName]: Список квартир будинку порожній")
            state.copy(mainLoading = false, isApartmentsLoaded = true)
          }
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

    // Сохраняем вашу оригинальную Firebase-синхронизацию прав, передавая вычисленный контекст квартиры
    val finalTarget = calculatedTarget
    if (result is Resource.Success && finalTarget != null) {
      try {
        println("[YkisLogKMP.$className.$methodName]: [FIREBASE_SYNC] Оновлення прав адміна у Firestore для о/р: ${finalTarget.addressId}L")

        // Синхронизируем роль и права в Firestore на основе первой выбранной квартиры
        firebaseService.updateUserRoleAndPermissions(
          uid = uid,
          addressId =finalTarget.addressId, // Сохраняем твой оригинальный маркер 0L для админ-уровня
          userRole = role,
          osbbId = osbbId,
          displayName = name ?: calculatedCombinedName
        )
        println("[YkisLogKMP.$className.$methodName]: [FIREBASE_SUCCESS] Адмін-права успішно запечатані.")
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.${methodName}_WARN]: Помилка синхронізації прав у хмарі: ${e.message}")
      }

      // Прогреваем кэш контактов расчетного центра для выбранного лицевого счета
      initialContactState()
    }
  }


  /**
   * [handleStandardUserResult] — Обробка та атомарна фіксація списку квартир мешканця м. Южне в СУБД ЮКІС.
   */
  private suspend fun handleStandardUserResult(
    result: Resource<List<ApartmentEntity>>,
    uid: String,
    role: UserRole,
    forcedAddressId: Long = 0L // ИСПРАВЛЕНО НАМЕРТВО: Необязательный целевой ID для сценария добавления!
  ) {
    val methodName = "handleStandardUserResult"
    var calculatedTarget: ApartmentEntity? = null
    var calculatedCombinedName = ""
    _apartmentUiState.update { state ->
      when (result) {
        is Resource.Success -> {
          val incomingApartments = result.data ?: emptyList()
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Отримано квартир з СУБД: ${incomingApartments.size}")

          val combinedApartments = (state.apartments + incomingApartments)
            .distinctBy { it.addressId }
          if (combinedApartments.isNotEmpty()) {
            combinedApartments.forEachIndexed { index, apt ->
              println("[YkisLogKMP.$className.$methodName]: [LIST_ITEM] #$index: ID=${apt.addressId}, Адрес=${apt.address}, OSBB_RAW='${apt.osbb}'")
            }
            val targetFromList = when {
              forcedAddressId != 0L -> combinedApartments.find { it.addressId == forcedAddressId } ?: combinedApartments.first()
              state.addressId == 0L -> combinedApartments.first()
              else -> combinedApartments.find { it.addressId == state.addressId } ?: combinedApartments.first()
            }
            val target = if (forcedAddressId != 0L) {
              targetFromList // Берем 100% наполненный сетевой объект ЮКІС со всеми площадями!
            } else {
              if (targetFromList.addressId == state.apartment.addressId && !state.apartment.address.isNullOrBlank()) {
                state.apartment
              } else {
                targetFromList
              }
            }
            calculatedTarget = target
            calculatedCombinedName = "${target.address} | ${target.nanim ?: ""}"
            println("[YkisLogKMP.$className.$methodName]: [TARGET_SELECT] Фіксація адреси мешканця ID=${target.addressId} (Поточний в стейті був: ${state.addressId}L)")
            val rawOsbb = target.osbb?.toString()
            val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") {
              "Мій ОСББ"
            } else {
              rawOsbb
            }
            println("[YkisLogKMP.$className.$methodName]: [FINAL_OSBB] Встановлюємо в UI: '$finalOsbbName'")
            val finalNanim = target.nanim ?: "Власник не вказаний"
            val finalAreaFull = target.areaFull ?: target.areaFull?.toString() ?: "0.00"
            val finalAreaOtopl = target.areaOtopl ?: target.areaOtopl?.toString() ?: "0.00"
            val finalRoom = target.room ?: target.room?.toString() ?: "0"
            val finalTenantTbo = target.tenantTbo ?: target.tenantTbo?.toString() ?: "0"
            state.copy(
              apartments = combinedApartments, // Запечатываем полный защищенный массив (3 квартиры)
              apartment = target,             // Копируем объект целиком со всеми 20+ полями БТИ!
              isApartmentsLoaded = true,
              addressId = target.addressId,   // Гарантированно фиксируем новый числовой ID из Ktor!
              address = target.address,
              osbbId = target.osmdId,
              osbb = finalOsbbName,

              // Прошиваем корень стейта для совместимости со старыми панелями экранов
              nanim = finalNanim,
              areaFull = finalAreaFull.toString(),
              areaOtopl = finalAreaOtopl.toString(),
              room = finalRoom.toString(),
              tenantTbo = finalTenantTbo.toString(),

              displayName = calculatedCombinedName,
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
    val finalTarget = calculatedTarget
    if (finalTarget != null) {
      try {
        println("[YkisLogKMP.$className.$methodName]: [FIREBASE_SYNC] Запуск фонового оновлення прав у Firestore для о/р: ${finalTarget.addressId}L")
        firebaseService.updateUserRoleAndPermissions(
          uid = uid,
          addressId = finalTarget.addressId,
          userRole = role,
          osbbId = finalTarget.osmdId,
          displayName = calculatedCombinedName
        )
        println("[YkisLogKMP.$className.$methodName]: [FIREBASE_SUCCESS] Права Firestore успішно запечатані.")
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.${methodName}_WARN]: Помилка фонової синхронізації прав у хмарі: ${e.message}")
      }
      println("[YkisLogKMP.$className.$methodName]: [CONTACT_PREWARM] Каскадний прогрев знімку контактів для о/р: ${finalTarget.addressId}L")
      initialContactState()
    }
  }

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
  fun addApartment() {
    val methodName = "addApartment"
    val input = _secretCode.value.trim() // Считываем значение из реактивного потока
    if (input.isEmpty()) return

    println("[YkisLogKMP.$className.$methodName]: Клік по кнопці верифікації. Введено код ЮКІС: $input")
    val uid = firebaseService.uid ?: return
    val email = firebaseService.email ?: ""

    // ИСПРАВЛЕНО НАМЕРТВО: Моментально выжигаем введенный код из оперативной памяти!
    // Текстовое поле на холсте AddApartmentScreen сразу станет чистым, защищая интерфейс от дребезга.
    _secretCode.value = ""

    if (input.all { it.isDigit() }) {
      // --- ЛОГІКА ЖИЛЬЦА ЮЖНОГО ---
      apartmentService.addApartment(input, uid, email).onEach { result ->
        handleApartmentResult(uid, result, restartApp = {})
      }.launchIn(screenModelScope)
    } else {
      // --- ЛОГІКА АДМИНИСТРАТОРА ОСМД ---
      apartmentService.verifyAdminCode(input, uid).onEach { result ->
        handleAdminResult(result, restartApp = {})
      }.launchIn(screenModelScope)
    }
  }



  /**
   * [handleApartmentResult] — Каскадна процедура обробки успішної верифікації коду ЮКІС.
   * ИСПРАВЛЕНО НАМЕРТВО: Преждевременный вызов initialContactState() полностью ВЫРЕЗАН!
   * Порядок сборки стейта ОЗУ перевернут, а фоновый поток СУБД изолирован от затирания анкеты,
   * что полностью уничтожает Race Condition и пустые вкладки БТИ при добавлении квартиры!
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
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Дані відповіді сервера порожні")
          isHandlingResult = false
          return
        }

        // ШАГ 1: ЗАПОМИНАЕМ ПОЛУЧЕННЫЙ ИЗ СЕТИ addressId
        val newAddressId = data.addressId ?: 0L
        val newOsbbId = data.osbbId ?: 0L
        val rawOsbbName = data.osbb?.toString()
        val newAddress = data.address ?: ""

        val finalOsbbName = if (rawOsbbName.isNullOrBlank() || rawOsbbName == "0") {
          "Мій ОСББ"
        } else {
          rawOsbbName
        }
        println("[YkisLogKMP.$className.$methodName]: [DATA_PARSE] Запам'ятовано ID: $newAddressId, OSBB_RAW: '$rawOsbbName', FINAL: '$finalOsbbName'")

        try {
          // ШАГ 2: МГНОВЕННО ОБНОВЛЯЕМ АКТИВНЫЙ АДРЕС И ОБЪЕКТ В СТЭЙТЕ ОЗУ!
          // Прошиваем новый ID (например, 3371L) и базовую заготовку квартиры, благодаря чему
          // стейт-машина хаба сразу перехватит новую траекторию и сотрет из ОЗУ старый адрес!
          println("[YkisLogKMP.$className.$methodName]: [IMPERATIVE_UPDATE] Атомарне встановлення нового о/р: ${newAddressId}L")

          val initialApartmentEntity = ApartmentEntity(
            addressId = newAddressId,
            address = newAddress,
            osbb = finalOsbbName,
            osmdId = newOsbbId,
            uid = uid
          )

          _apartmentUiState.update { state ->
            state.copy(
              addressId = newAddressId,                 // Запечатываем новый ID
              apartment = initialApartmentEntity,       // Заливаем базовую сущность для LaunchedEffect
              address = newAddress,
              osmdId = newOsbbId,
              osbbId = newOsbbId,
              osbb = finalOsbbName,
              userRole = UserRole.StandardUser,
              mainLoading = false                       // Тушим лоадер, плавно открывая InfoApartmentScreen
            )
          }

          // 3. СИНХРОНИЗАЦИЯ С ОБЛАКОМ (Firestore КМР-сессия)
          println("[YkisLogKMP.$className.$methodName]: [STEP 1] Фіксація прав у Firestore. Адреса: $newAddress")
          firebaseService.updateUserRoleAndPermissions(
            uid = uid,
            addressId = newAddressId,
            userRole = UserRole.StandardUser,
            osbbId = newOsbbId,
            displayName = newAddress
          )

          // ШАГ 4: Вызываем getApartmentList(uid), который через последовательный collect{}
          // дождется записи обновленного кэша квартир на диск смартфона в СУБД SQLDelight.
          println("[YkisLogKMP.$className.$methodName]: [STEP 2] Запуск послідовної синхронізації кЕшу СУБД...")
          apartmentService.getApartmentList(uid).collect { syncResult ->
            if (syncResult is Resource.Success) {
              println("[YkisLogKMP.$className.$methodName]: [SYNC_DB_OK] Дисковий кЕш СУБД успішно оновлено.")

              // ШАГ 5: Передаем успешный пакет И НАШ НОВЫЙ forcedAddressId в handleStandardUserResult!
              // Этот метод сам атомарно сделает умное слияние, пропишет apartments (размер станет равен 3),
              // скопирует живую квартиру со всеми 20+ полями БТИ в поле apartment и выставит addressId!
              handleStandardUserResult(
                result = syncResult,
                uid = uid,
                role = UserRole.StandardUser,
                forcedAddressId = newAddressId // Передаем явный маркер-якорь
              )
            }
          }

          if (lastHandledResultId != newAddressId) {
            lastHandledResultId = newAddressId
            println("[YkisLogKMP.$className.$methodName]: [STEP 4] Ініціалізація резидент-чатів для особового рахунку $newAddressId")
          }

          // Очищаем реактивное текстовое поле ввода кода ЮКІС
          _secretCode.value = ""
          SnackbarManager.showMessage("Особовий рахунок успішно прив'язано до профілю ЮКІС")

          println("[YkisLogKMP.$className.$methodName]: Очікування стабілизации контексту...")
          kotlinx.coroutines.delay(500)
          isHandlingResult = false

          println("[YkisLogKMP.$className.$methodName]: [FINISH] Рахунок прив'язано. Реактивно залишаємось у модулі БТІ ЮКІС.")

        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: [CRITICAL ERROR] ${e.message}")
          isHandlingResult = false
          _apartmentUiState.update { it.copy(mainLoading = false) }
          SnackbarManager.showMessage("Помилка синхронізації профілю квартири")
        }
      }

      is Resource.Error -> {
        val errorMessage = result.message ?: "Помилка додавання особового рахунку"
        println("[YkisLogKMP.$className.$methodName]: [API ERROR] $errorMessage")
        isHandlingResult = false
        _secretCode.value = ""
        _apartmentUiState.update { it.copy(mainLoading = false) }
        SnackbarManager.showMessage(errorMessage)
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


  private fun String.isValidEmailKmp(): Boolean {
    // Канонический паттерн RFC, устойчивый к любым текстовым автозаменам в IDE
    val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"
    return this.matches(emailRegex.toRegex())
  }


  /**
   * [initialContactState] — Ініціалізація локального КМР-стану контактів абонента БТІ міста Южне.
   */


  fun initialContactState() {
    val methodName = "initialContactState"
    val currentState = _apartmentUiState.value

    println("[YkisLogKMP.$className.$methodName]: [START] Фіксація знімку анкетних даних БТІ для о/р ${currentState.addressId}")

    val finalEmail = currentState.apartment.email ?: currentState.email ?: ""

    val rawPhone = currentState.apartment.phone ?: currentState.phone ?: ""
    val finalPhone = rawPhone.trim() // Выжигаем скрытые пробелы биллинга, оставляя чистый текст

    // Обновляем наш главный реактивный поток ЮКІС
    _apartmentUiState.update { state ->
      state.copy(
        email = finalEmail,
        phone = finalPhone
      )
    }

    println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Контакти ЮКІС зафіксовано (Тел: '$finalPhone', Email: '$finalEmail')")
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
  /**
   * [onUpdateBti] — Оновлення контактних даних абонента БТІ на сервері Ktor через прямі параметри.
   * ИСПРАВЛЕНО НАМЕРТВО: Метод принимает phone и email напрямую из полей ввода диалога,
   * полностью исключая отправку пустых строк из-за асинхронных задержек обновления UI-стейта!
   */
  fun onUpdateBti(phone: String, email: String) {
    val methodName = "onUpdateBti"
    val currentState = _apartmentUiState.value
    val cleanEmail = email.trim()
    val cleanPhone = phone.trim()
    val currentUid = firebaseService.uid ?: ""
    if (!cleanEmail.isValidEmailKmp() && cleanEmail.isNotEmpty()) {
      SnackbarManager.showMessage("Некоректний формат Email адреси")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [START] Надсилання нових контактів БТІ на сервер ЮКІС... (Тел: '$cleanPhone', Email: '$cleanEmail')")

    // Сразу фиксируем введенные строки в ОЗУ стейта, чтобы UI обновился мгновенно
    _apartmentUiState.update { state ->
      state.copy(
        email = cleanEmail,
        phone = cleanPhone
      )
    }

    apartmentService.updateBti(
      uid= currentUid,
      addressId = currentState.addressId,
      phone = cleanPhone,
      email = cleanEmail
    ).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Дані БТІ успішно оновлені на сервері та в СУБД")
          SnackbarManager.showMessage("Дані БТІ успішно оновлено")
          getApartment(currentState.addressId) // Перевычитываем анкету
        }

        is Resource.Error -> {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Збій оновлення БТІ: ${result.message}")
          _apartmentUiState.update { it.copy(apartmentLoading = false) }
          SnackbarManager.showMessage(result.message ?: "Помилка оновлення даних")
        }

        is Resource.Loading -> {
          println("[YkisLogKMP.$className.$methodName]: [LOADING] Синхронізація анкети БТІ з сервером ЮКІС...")
          _apartmentUiState.update { it.copy(apartmentLoading = true) }
        }
      }
    }.launchIn(screenModelScope)
  }


  private var lastProcessingAddressId: Long = -1L

  /**
   * [getApartment] — Завантаження детальної інформації по конкретній квартирі з біллінгу.
   */
  /**
   * [getApartment] — Завантаження детальної інформації по конкретній квартирі з біллінгу ЮКІС.
   * ИСПРАВЛЕНО НАМЕРТВО: Предохранитель ATOMIC SKIP переведен на жесткую проверку наполнения анкеты!
   * Если адрес пустой, замок пробивается, гарантируя 100% догрузку 20+ полей БТИ с сервера ЮКІС.
   */
  fun getApartment(addressId: Long = _apartmentUiState.value.addressId) {
    val methodName = "getApartment"
    if (addressId <= 0L) return

    val state = _apartmentUiState.value

    if (state.addressId == addressId && state.apartmentLoading) return

    // ====================================================================
    // --- ИСПРАВЛЕНО НАМЕРТВО: УМНЫЙ РЕАКТИВНЫЙ ПРЕДОХРАНИТЕЛЬ КЭША ---
    // ====================================================================
    // Проверяем: если ID совпадает И текстовый адрес уже реально наполнен даными из сети,
    // только тогда мы делаем ATOMIC SKIP. Если адрес пустой — это заглушка, качаем сеть!
    val isAlreadyFullyLoaded = state.addressId == addressId &&
      state.apartment.addressId != 0L &&
      !state.apartment.address.isNullOrBlank()

    if (isAlreadyFullyLoaded) {
      println("[YkisLogKMP.$className.$methodName($addressId)]: -> ATOMIC SKIP (Дані вже актуальні в ОЗУ)")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [FORCE_FETCH] Завантаження о/р $addressId з біллінгу міста Южне")

    // Убрана деструктивная отмена observeJob?.cancel(),
    // чтобы этот метод не сжигал фоновый поток слияния списков handleStandardUserResult!
    val currentUid = firebaseService.uid

    apartmentService.getApartment(addressId = addressId, uid = currentUid).onEach { result ->
      when (result) {
        is Resource.Success -> {
          val data = result.data ?: ApartmentEntity()
          val currentUserRole = _apartmentUiState.value.userRole
          val isStandardUser = currentUserRole == UserRole.StandardUser

          // Прошиваем детальный наполненный объект в наш главный поток ЮКІС
          _apartmentUiState.update { currentState ->
            currentState.copy(
              apartment = data,
              addressId = data.addressId,
              houseId = data.houseId,
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
        }

        is Resource.Loading -> {
          _apartmentUiState.update { it.copy(apartmentLoading = true) }
        }
      }
    }.launchIn(screenModelScope)
  }

  /**
   * [getApartmentList] — Завантаження повного списку прив'язаних квартир абонента з біллінгу ЮКІС.
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
                houseId = first.houseId,
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



  fun deleteApartmentFromProfile(addressId: Long, onNavigateToAddScreen: () -> Unit) {
    val methodName = "deleteApartmentFromProfile"
    val uid = firebaseService.uid ?: ""
    if (addressId <= 0L || uid.isBlank()) return

    launchCatching(showLoader = true) {
      println("[YkisLogKMP.$className.$methodName]: [START] Запит на видалення о/р $addressId для UID: $uid")

      // 1. Физически удаляем квартиру на сервере биллинга Южного через Ktor
      apartmentService.deleteApartment(addressId, uid).collect { deleteResult ->
        if (deleteResult is Resource.Success) {
          println("[YkisLogKMP.$className.$methodName]: [DELETE_SERVER_OK] Квартира успішно видалена з бази Южного.")
        }
      }

      // 2. ИСПРАВЛЕНО НАМЕРТВО: Железная ручная чистка ОЗУ от удаленной квартиры!
      // Мы берем текущий список, фильтруем его, вырезая удаленный addressId, и заливаем обратно в стейт!
      _apartmentUiState.update { currentState ->
        val cleanList = currentState.apartments.filter { it.addressId != addressId }
        currentState.copy(
          apartments = cleanList,
          error = null
        )
      }

      // 3. Запускаем фоновое обновление СУБД "выстрелил и забыл" для полной синхронизации с диском смартфона
      println("[YkisLogKMP.$className.$methodName]: [REFRESH_BACKGROUND] Фоновий прогрев базы данных...")
      apartmentService.getApartmentList(uid).launchIn(screenModelScope)

      // Считываем гарантированно чистый список без удаленного ID
      val updatedList = _apartmentUiState.value.apartments
      println("[YkisLogKMP.$className.$methodName]: [UPDATE] Залишилося квартир в ОЗУ хаба після фільтрації: ${updatedList.size}")

      // 4. ПУЛЕНЕПРОБИВАЕМАЯ МАРШРУТИЗАЦИЯ
      if (updatedList.isEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [NAVIGATE] Квартир більше немає, маршрутизація на екран прив'язку БТІ")
        _apartmentUiState.update { currentState ->
          currentState.copy(
            addressId = 0L,
            address = "",
            apartment = ApartmentEntity(),
            apartmentLoading = false,
            mainLoading = false
          )
        }
        onNavigateToAddScreen()
      } else {
        // Если у жителя Южного остались другие квартиры — берем первую из оставшихся (ID 6314)
        val nextApartment = updatedList.first()
        println("[YkisLogKMP.$className.$methodName]: [SWITCH] Автоматичний перехід на наступний рахунок ID: ${nextApartment.addressId}")

        // Наш золотой метод setAddressId обновит права Firestore и скачает анкету БТИ
        setAddressId(nextApartment.addressId)
        _apartmentUiState.update { it.copy(apartmentLoading = false) }
      }
    }
  }

  fun setAddressId(addressId: Long) {
    val methodName = "setAddressId"
    val currentState = _apartmentUiState.value
    val isResident = currentState.userRole == UserRole.StandardUser
    println("[YkisLogKMP.$className.$methodName]: [START] Пошук та активація ID: $addressId")

    // Поиск объекта квартиры в КМР-списках СУБД SQLDelight
    val target = currentState.apartments.find { it.addressId == addressId }
      ?: _drawerApartments.value.find { it.addressId == addressId }

    if (target != null) {
      val finalOsbbId = if (currentState.osbbId > 9000L) currentState.osmdId else target.osmdId
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
          apartment = target, // Вливаем объект, UI на экране перехватит его addressId
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
      println("[YkisLogKMP.$className.$methodName]: [WARNING] Об'єкт БТІ не знайдено в пам'яті. Примусове встановлення ID та запуск Ktor-акумуляції.")

      _apartmentUiState.update { it.copy(addressId = addressId) }

      // КАСКАДНЫЙ ПРОГРЕВ ХОЛОДНОГО СТАРТА: Если объект еще не успел дойти из СУБД, принудительно
      // посылаем Ktor-запрос за детальной анкетой БТИ, чтобы со стопроцентной гарантией наполнить вкладку!
      if (isResident) {
        screenModelScope.launch {
          println("[YkisLogKMP.$className.$methodName]: [CASCADE_LOAD_COLD_START] Примусовий запуск getApartment для холодного старту.")
          getApartment(addressId = addressId)
        }
      }
    }
  }


} // Абсолютний кінець і фінальне запечатування всього класу ApartmentScreenModel




