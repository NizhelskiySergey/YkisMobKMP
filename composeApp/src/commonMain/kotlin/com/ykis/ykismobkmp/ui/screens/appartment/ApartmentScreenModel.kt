package com.ykis.ykismobkmp.ui.screens.appartment

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


// Режимы вложенности списков (Районы г. Южное -> Дома -> Квартиры жителей)
enum class ListMode { RAIONS, HOUSES, APARTMENTS }

/**
 * [ApartmentScreenModel] — Кроссплатформенная модель управления привязкой лицевых счетов и админ-панелей ОСМД.
  */
class ApartmentScreenModel(
  val firebaseService: FirebaseService,
  private val apartmentService: ApartmentService,
  logService: LogService,
) : BaseScreenModel(logService) {
  private val className = "ApartmentScreenModel"

  val uid get() = firebaseService.uid
  val email get() = firebaseService.email

  private var observeJob: Job? = null
  private var isHandlingResult = false
  private val _secretCode = MutableStateFlow("")
  val secretCode: StateFlow<String> = _secretCode.asStateFlow()

  private val _drawerHouses = MutableStateFlow<List<HouseEntity>>(emptyList())
  val drawerHouses = _drawerHouses.asStateFlow()

  private val _drawerApartments = MutableStateFlow<List<ApartmentEntity>>(emptyList())
  val drawerApartments = _drawerApartments.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()


  init {
    Log.i("Инициализация ApartmentScreenModel в КМР слое.", tag = className)
    disableMainLoading()
  }

  fun disableMainLoading() {
    _uiState.update { it.copy(mainLoading = false) }
    Log.i("Глобальный лоадер БТИ принудительно переведен в FALSE", tag = className)
  }

  // ОПТИМІЗАЦІЯ: Створюємо вузький потік станів для фільтрації, щоб не перераховувати список при кожній зміні лоадерів
  private val _apartmentFilterData = _uiState.map {
    Triple(it.listMode, it.userRole, it.apartments)
  }.distinctUntilChanged()

  val filteredApartments: StateFlow<List<ApartmentEntity>> = combine(
    _searchQuery,
    _apartmentFilterData,
    _drawerHouses,
    _drawerApartments
  ) { query, filterData, houses, drApts ->
    val (listMode, userRole, apartments) = filterData
    if (query.isEmpty()) return@combine emptyList()

    when (listMode) {
      ListMode.HOUSES -> {
        // ОПТИМІЗАЦІЯ: Використовуємо sequence для великих списків та уникаємо зайвих алокацій
        houses.asSequence()
          .filter { it.house.contains(query, ignoreCase = true) }
          .map { ApartmentEntity(address = it.house, addressId = it.houseId) }
          .toList()
      }

      ListMode.APARTMENTS -> {
        val source =
          if (userRole != UserRole.StandardUser && userRole != UserRole.OsbbUser) {
            drApts
          } else {
            apartments
          }

        source.filter {
          it.address.contains(query, ignoreCase = true) ||
            (it.nanim.contains(query, ignoreCase = true)) ||
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
   * [onRaionSelected] — Обработка выбора Района в Dropdown (г. Южное / Одесская область).
   */
  fun onRaionSelected(raion: RaionEntity) {
    val methodName = "onRaionSelected"
    val raionIdLong = raion.raionId

    println("[YkisLogKMP.$className.$methodName]: [START] Выбран Район: ${raion.raion} (ID: $raionIdLong)")

    _uiState.update {
      it.copy(
        selectedRaionId = raionIdLong,
        listMode = ListMode.HOUSES
      )
    }

    screenModelScope.launch {
      apartmentService.getHouseList(raionIdLong).collect { result ->
        when (result) {
          is Resource.Success -> {
            val houses = result.data ?: emptyList()
            println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Домов загружено в Drawer: ${houses.size}")

            _drawerHouses.value = houses
            _uiState.update { it.copy(isLoading = false) }
          }

          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
            _uiState.update { it.copy(isLoading = false) }
            SnackbarManager.showMessage(result.message ?: "Помилка завантаження будинків")
          }

          is Resource.Loading -> {
            _uiState.update { it.copy(isLoading = true) }
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

    _uiState.update { state ->
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
    println("[YkisLogKMP.$className.$methodName]: [START] Загрузка квартир для дома ID: $houseId")

    _uiState.update {
      it.copy(
        selectedHouseId = houseId,
        listMode = ListMode.APARTMENTS
      )
    }

    screenModelScope.launch {
      apartmentService.getOsbbApartmentsList(houseId, true).collect { result ->
        when (result) {
          is Resource.Success -> {
            val apartments = result.data ?: emptyList()
            println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Отримано: ${apartments.size} кв.")

            _drawerApartments.value = apartments
            _uiState.update { it.copy(isLoading = false) }
          }

          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
            _uiState.update { it.copy(isLoading = false) }

            _uiState.update { state ->
              state.copy(listMode = ListMode.HOUSES)
            }

            SnackbarManager.showMessage(result.message ?: "Помилка завантаження квартир")
          }

          is Resource.Loading -> {
            _uiState.update { it.copy(isLoading = true) }
          }
        }
      }
    }
  }

  fun onSecretCodeChanged(newCode: String) {
    _secretCode.value = newCode
  }

  /**
   * [observeUserProfile] — Фоновый КМР-мониторинг профиля жителя и его лицевых счетов.
   */
  fun observeUserProfile() {
    val methodName = "observeUserProfile"
    val actualUid = firebaseService.uid ?: return

    println("[YkisLogKMP.$className.$methodName]: [START] actualUid: $actualUid")
    observeJob?.cancel()

    observeJob = screenModelScope.launch {
      _uiState.update { it.copy(mainLoading = true) }
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

        _uiState.update {
          it.copy(
            uid = user.uid,
            userRole = currentUserRole,
            osbbId = currentOsbbId,
            osmdId = currentOsbbId,
            displayName = user.name ?: ""
          )
        }

        // ГАРАНТИЯ ПУШЕЙ: Регистрируем токен устройства сразу после входа в профиль
        screenModelScope.launch {
            firebaseService.addFcmToken()
        }

        // ИСПРАВЛЕНО: Принудительно регистрируем FCM токен при каждом запуске профиля
        firebaseService.addFcmToken()

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
              // Резервный шлюз для других коммунальных предприятий г. Южного
              handleOrganizationResult(result, user.uid, currentUserRole, currentOsbbId, user.name)
            }
          }
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [FATAL ERROR] ${e.message}")
        _uiState.update { it.copy(mainLoading = false) }
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

    if (result is Resource.Success) {
      val incomingApartments = result.data ?: emptyList()
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Загружено ${incomingApartments.size} кв. для ОСББ ID: $osbbId")

      _uiState.update { state ->
        val combinedApartments = if (state.apartments.isEmpty()) incomingApartments
                                 else (incomingApartments + state.apartments).distinctBy { it.addressId }

        if (combinedApartments.isNotEmpty()) {
          val target = when {
            state.addressId == 0L -> combinedApartments.first()
            else -> combinedApartments.find { it.addressId == state.addressId } ?: combinedApartments.first()
          }

          calculatedTarget = target

          val rawOsbb = target.osbb
          val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") name ?: "Мій ОСББ" else rawOsbb

          state.copy(
            apartments = combinedApartments,
            apartment = target,
            isApartmentsLoaded = true,
            listMode = ListMode.APARTMENTS,
            addressId = target.addressId,
            address = target.address,
            osbbId = target.osmdId,
            osbb = finalOsbbName,
            nanim = target.nanim ?: "Власник не вказаний",
            areaFull = target.areaFull.toString(),
            areaOtopl = target.areaOtopl.toString(),
            room = target.room.toString(),
            tenantTbo = target.tenantTbo.toString(),
            displayName = target.address,
            mainLoading = false
          )
        } else {
          state.copy(mainLoading = false, isApartmentsLoaded = true)
        }
      }
    } else {
      _uiState.update { state ->
        when (result) {
          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбій завантаження списку адміна: ${result.message}")
            state.copy(mainLoading = false)
          }
          is Resource.Loading -> state.copy(mainLoading = true)
          else -> state
        }
      }
    }

    val finalTarget = calculatedTarget
    if (finalTarget != null) {
      try {
        println("[YkisLogKMP.$className.$methodName]: [FIREBASE_SYNC] Оновлення прав адміна...")
        firebaseService.updateUserRoleAndPermissions(
          uid = uid,
          addressId = finalTarget.addressId,
          userRole = role,
          osbbId = osbbId,
          displayName = finalTarget.address,
          fio = finalTarget.nanim ?: ""
        )
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.${methodName}_WARN]: Помилка синхронізації прав: ${e.message}")
      }
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
    
    // ОПТИМИЗАЦИЯ: Выносим тяжелые вычисления и логирование за пределы блока update{}
    if (result is Resource.Success) {
      val incomingApartments = result.data ?: emptyList()
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Получено квартир из СУБД: ${incomingApartments.size}")

      _uiState.update { state ->
        // ОПТИМІЗАЦІЯ: Розумне злиття списків — мережеві дані мають пріоритет
        val combinedApartments = if (state.apartments.isEmpty()) incomingApartments 
                                 else (incomingApartments + state.apartments).distinctBy { it.addressId }
        
        if (combinedApartments.isNotEmpty()) {
          val targetFromList = when {
            forcedAddressId != 0L -> combinedApartments.find { it.addressId == forcedAddressId } ?: combinedApartments.first()
            state.addressId == 0L -> combinedApartments.first()
            else -> combinedApartments.find { it.addressId == state.addressId } ?: combinedApartments.first()
          }
          val target = if (forcedAddressId != 0L) {
            targetFromList 
          } else {
            if (targetFromList.addressId == state.apartment.addressId && !state.apartment.address.isNullOrBlank()) {
              state.apartment
            } else {
              targetFromList
            }
          }
          calculatedTarget = target
          
          val rawOsbb = target.osbb
          val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") "Мій ОСББ" else rawOsbb
          
          // ОПТИМІЗАЦІЯ: Інтегруємо logic з initialContactState прямо сюди для атомарності
          val finalEmail = target.email.takeIf { it.isNotBlank() } ?: state.email ?: ""
          val finalPhone = target.phone.trim().takeIf { it.isNotBlank() } ?: state.phone ?: ""

          state.copy(
            apartments = combinedApartments,
            apartment = target,
            isApartmentsLoaded = true,
            addressId = target.addressId,
            address = target.address,
            osbbId = target.osmdId,
            osmdId = target.osmdId, // ИСПРАВЛЕНО: Синхронизируем оба ID для биллинга
            osbb = finalOsbbName,
            nanim = target.nanim ?: "Власник не вказаний",
            areaFull = target.areaFull.toString(),
            areaOtopl = target.areaOtopl.toString(),
            room = target.room.toString(),
            tenantTbo = target.tenantTbo.toString(),
            displayName = target.address,
            email = finalEmail,
            phone = finalPhone,
            mainLoading = false
          )
        } else {
          state.copy(mainLoading = false, isApartmentsLoaded = true)
        }
      }
    } else {
      _uiState.update { state ->
        when (result) {
          is Resource.Error -> {
            println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
            state.copy(mainLoading = false)
          }
          is Resource.Loading -> state.copy(mainLoading = true)
          else -> state
        }
      }
    }

    val finalTarget = calculatedTarget
    if (finalTarget != null) {
      try {
        println("[YkisLogKMP.$className.$methodName]: [FIREBASE_SYNC] Синхронізація прав...")
        firebaseService.updateUserRoleAndPermissions(
          uid = uid,
          addressId = finalTarget.addressId,
          userRole = role,
          osbbId = finalTarget.osmdId,
          displayName = finalTarget.address,
          fio = finalTarget.nanim ?: ""
        )
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.${methodName}_WARN]: Помилка фонової синхронізації прав у хмарі: ${e.message}")
      }
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
    _uiState.update { state ->
      when (result) {
        is Resource.Success -> {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Загружено ${result.data?.size} районов Одесской обл.")
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
    println("[YkisLogKMP.$className.$methodName]: [RESET] Возврат к администрированию коммунального фонда")
    
    _uiState.update {
      it.copy(
        apartments = emptyList(),
        addressId = 0L,
        address = "",
        mainLoading = false 
      )
    }
    observeUserProfile()
  }

  fun addApartment() {
    val methodName = "addApartment"
    val input = _secretCode.value.trim() // Считываем значение из реактивного потока
    if (input.isEmpty()) return

    println("[YkisLogKMP.$className.$methodName]: Клик по кнопке верификации. Введен код ЮКИС: $input")
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
        _uiState.update { it.copy(mainLoading = true) }
      }

      is Resource.Success -> {
        isHandlingResult = true
        val data = result.data ?: run {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Дані відповіді сервера порожні")
          isHandlingResult = false
          return
        }

        // ШАГ 1: ЗАПОМИНАЕМ ПОЛУЧЕННЫЙ ИЗ СЕТИ addressId (Приводим к Long для надежности)
        val newAddressId = data.addressId?.toString()?.toLongOrNull() ?: 0L
        val newOsbbId = data.osbbId?.toString()?.toLongOrNull() ?: 0L
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
          val initialApartmentEntity = ApartmentEntity(
            addressId = newAddressId,
            address = newAddress,
            osbb = finalOsbbName,
            osmdId = newOsbbId,
            uid = uid
          )

          _uiState.update { state ->
            state.copy(
              addressId = newAddressId,
              apartment = initialApartmentEntity,
              address = newAddress,
              nanim = data.nanim,
              osmdId = newOsbbId,
              osbbId = newOsbbId,
              osbb = finalOsbbName,
              userRole = UserRole.StandardUser,
              displayName = newAddress,
              mainLoading = false
            )
          }

          // 3. СИНХРОНИЗАЦИЯ С ОБЛАКОМ
          firebaseService.updateUserRoleAndPermissions(
            uid = uid,
            addressId = newAddressId,
            userRole = UserRole.StandardUser,
            osbbId = newOsbbId,
            displayName = newAddress,
            fio = data.nanim ?: ""
          )

          // ИСПРАВЛЕНО НАМЕРТВО: Сразу активируем чаты по данным из сети, используя реальное имя
          println("[YkisLogKMP.$className.$methodName]: [STEP 3] Негайний запуск ініціалізації чатів...")
          apartmentService.initResidentChats(
            scope = screenModelScope,
            uid = uid,
            osbbId = newOsbbId,
            addressId = newAddressId,
            addressText = newAddress,
            nanim = data.nanim ?: data.address ?: "Мешканець"
          )

          // ШАГ 4: Синхронизация с БД
          println("[YkisLogKMP.$className.$methodName]: [STEP 4] Фонова синхронізація локальної СУБД...")
          
          apartmentService.getApartmentList(uid)
             .filter { it is Resource.Success }
             .take(1)
             .collect { syncResult ->
                println("[YkisLogKMP.$className.$methodName]: [SYNC_DB_OK] Дисковий кэш оновлено.")
                handleStandardUserResult(
                    result = syncResult as Resource.Success,
                    uid = uid,
                    role = UserRole.StandardUser,
                    forcedAddressId = newAddressId
                )
             }

          _secretCode.value = ""
          SnackbarManager.showMessage("Рахунок успішно прив'язаний")

          kotlinx.coroutines.delay(500)
          isHandlingResult = false

        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: [CRITICAL ERROR] ${e.message}")
          isHandlingResult = false
          _uiState.update { it.copy(mainLoading = false) }
          SnackbarManager.showMessage("Помилка синхронізації")
        }
      }

      is Resource.Error -> {
        val errorMessage = result.message ?: "Помилка додавання особового рахунку"
        println("[YkisLogKMP.$className.$methodName]: [API ERROR] $errorMessage")
        isHandlingResult = false
        _secretCode.value = ""
        _uiState.update { it.copy(mainLoading = false) }
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
        val mappedRole = UserRole.fromString(data.userRole)
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

            _uiState.update {
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
            _uiState.update { it.copy(mainLoading = false) }
            SnackbarManager.showMessage("Помилка авторизації прав доступу")
          }
        }
      }

      is Resource.Error -> {
        println("[YkisLogKMP.$className.$methodName]: [API ERROR] ${result.message}")
        SnackbarManager.showMessage(result.message ?: "Невірне секретне слово доступу")
      }

      is Resource.Loading -> {
        _uiState.update { it.copy(mainLoading = true) }
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
    val currentState = _uiState.value

    val finalEmail = currentState.apartment.email.takeIf { it.isNotBlank() } ?: currentState.email ?: ""
    val finalPhone = currentState.apartment.phone.trim().takeIf { it.isNotBlank() } ?: currentState.phone ?: ""

    // ОПТИМІЗАЦІЯ: Уникаємо оновлення стейту, если данные уже идентичны
    _uiState.update { state ->
      if (state.email == finalEmail && state.phone == finalPhone) state
      else state.copy(email = finalEmail, phone = finalPhone)
    }
  }



  fun onEmailChange(newValue: String) {
    _uiState.update { it.copy(email = newValue) }
  }

  fun onPhoneChange(newValue: String) {
    _uiState.update { it.copy(phone = newValue) }
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
    val currentState = _uiState.value
    val cleanEmail = email.trim()
    val cleanPhone = phone.trim()
    val currentUid = firebaseService.uid ?: ""
    if (!cleanEmail.isValidEmailKmp() && cleanEmail.isNotEmpty()) {
      SnackbarManager.showMessage("Некоректний формат Email адреси")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: [START] Надсилання нових контактів БТІ на сервер ЮКІС... (Тел: '$cleanPhone', Email: '$cleanEmail')")

    // Сразу фиксируем введенные строки в ОЗУ стейта, чтобы UI обновился мгновенно
    _uiState.update { state ->
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
          _uiState.update { it.copy(apartmentLoading = false) }
          SnackbarManager.showMessage(result.message ?: "Помилка оновлення даних")
        }

        is Resource.Loading -> {
          println("[YkisLogKMP.$className.$methodName]: [LOADING] Синхронізація анкети БТІ з сервером ЮКІС...")
          _uiState.update { it.copy(apartmentLoading = true) }
        }
      }
    }.launchIn(screenModelScope)
  }


  /**
   * [getApartment] — Завантаження детальної інформації по конкретній квартирі з біллінгу ЮКІС.
   * ИСПРАВЛЕНО НАМЕРТВО: Предохранитель ATOMIC SKIP переведен на жесткую проверку наполнения анкеты!
   * Если адрес пустой, замок пробивается, гарантируя 100% догрузку 20+ полей БТИ с сервера ЮКІС.
   */
  fun getApartment(addressId: Long = _uiState.value.addressId) {
    val methodName = "getApartment"
    if (addressId <= 0L) return

    val state = _uiState.value

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
          val currentUserRole = _uiState.value.userRole
          val isStandardUser = currentUserRole == UserRole.StandardUser

          // Прошиваем детальный наполненный объект в наш главный поток ЮКІС
          _uiState.update { currentState ->
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
            println("[YkisLogKMP.$className.$methodName]: [RESIDENT_SYNC] Профіль та токен чатів мешканця успішно синхронізовано")

            firebaseService.updateUserRoleAndPermissions(
              uid = currentUid,
              addressId = data.addressId,
              userRole = currentUserRole,
              osbbId = data.osmdId,
              displayName = data.address,
              fio = data.nanim ?: ""
            )
          } else {
            println("[YkisLogKMP.$className.$methodName]: [ADMIN_VIEW] Перегляд о/р ${data.addressId} завершено. Особистий профіль голови ОСББ захищено.")
          }
        }

        is Resource.Error -> {
          println("[YkisLogKMP.$className.$methodName]: -> КРИТИЧНА ПОМИЛКА БІЛІНГУ КТOR: ${result.message}")
          _uiState.update { it.copy(apartmentLoading = false) }
        }

        is Resource.Loading -> {
          _uiState.update { it.copy(apartmentLoading = true) }
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
      _uiState.update { state ->
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

    val currentState = _uiState.value
    // Запоминаем osbbId перед удалением для зачистки чатов
    val targetApt = currentState.apartments.find { it.addressId == addressId }
    val targetOsbbId = targetApt?.osmdId ?: 0L

    launchCatching(showLoader = true) {
      println("[YkisLogKMP.$className.$methodName]: [START] Запрос на удаление о/р $addressId для UID: $uid")

      // 1. Зачистка веток чатов в Firebase (чтобы диспетчер не слал пуши "в никуда")
      apartmentService.deleteResidentChats(
          scope = screenModelScope,
          uid = uid,
          osbbId = targetOsbbId,
          addressId = addressId
      )

      // 2. Физически удаляем квартиру на сервере биллинга Южного через Ktor
      apartmentService.deleteApartment(addressId, uid).collect { deleteResult ->
        if (deleteResult is Resource.Success) {
          println("[YkisLogKMP.$className.$methodName]: [DELETE_SERVER_OK] Квартира успішно видалена з бази Южного.")
        }
      }

      // 2. ИСПРАВЛЕНО НАМЕРТВО: Железная ручная чистка ОЗУ от удаленной квартиры!
      // Мы берем текущий список, фильтруем его, вырезая удаленный addressId, и заливаем обратно в стейт!
      _uiState.update { currentState ->
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
      val updatedList = _uiState.value.apartments
      println("[YkisLogKMP.$className.$methodName]: [UPDATE] Залишилося квартир в ОЗУ хаба после фильтрации: ${updatedList.size}")

      // 4. ПУЛЕНЕПРОБИВАЕМАЯ МАРШРУТИЗАЦИЯ
      if (updatedList.isEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [NAVIGATE] Квартир більше немає, маршрутизація на екран прив'язку БТІ")
        _uiState.update { currentState ->
          currentState.copy(
            addressId = 0L,
            address = "",
            apartment = com.ykis.ykismobkmp.domain.entity.ApartmentEntity(),
            apartmentLoading = false,
            mainLoading = false,
            apartments = emptyList() // ИСПРАВЛЕНО: Явная зачистка списка
          )
        }

        onNavigateToAddScreen()
      } else {
        // Если у жителя Южного остались другие квартиры — берем первую из оставшихся (ID 6314)
        val nextApartment = updatedList.first()
        println("[YkisLogKMP.$className.$methodName]: [SWITCH] Автоматичний перехід на наступний рахунок ID: ${nextApartment.addressId}")

        // Наш золотой метод setAddressId обновит права Firestore и скачает анкету БТИ
        setAddressId(nextApartment.addressId)
        _uiState.update { it.copy(apartmentLoading = false) }
      }
    }
  }

  fun setAddressId(addressId: Long) {
    val methodName = "setAddressId"
    val currentState = _uiState.value
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

      _uiState.update { state ->
        state.copy(
          addressId = target.addressId,
          apartment = target, // Вливаем объект, UI на экране перехватит его addressId
          address = target.address,
          osbb = finalOsbbDisplayName,
          houseId = target.houseId,
          displayName = target.address,
          osbbId = finalOsbbId,
          osmdId = finalOsbbId,
          apartmentLoading = false
        )
      }

      // Обновляем addressId в Firestore для всех ролей (включая админов при просмотре)
      screenModelScope.launch {
        firebaseService.updateUserRoleAndPermissions(
          uid = currentState.uid ?: "",
          addressId = target.addressId,
          userRole = currentState.userRole,
          osbbId = finalOsbbId,
          displayName = target.address,
          fio = target.nanim ?: ""
        )
      }
      
      if (isResident) {
        println("[YkisLogKMP.$className.$methodName]: [USER_SYNC] Обновление подписок на бейджи уведомлений квартир жильца")
      } else {
        println("[YkisLogKMP.$className.$methodName]: [ADMIN_VIEW_SYNC] Фиксация просматриваемого о/р в Firestore для админа")
      }

      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] State о/р успешно обновлено. Текущий AddressId: $addressId | OSBB: $finalOsbbDisplayName")
    } else {
      println("[YkisLogKMP.$className.$methodName]: [WARNING] Объект БТИ не найден в памяти. Принудительная установка ID и запуск Ktor-аккумуляции.")

      _uiState.update { it.copy(addressId = addressId) }

      // КАСКАДНЫЙ ПРОГРЕВ ХОЛОДНОГО СТАРТА: Если объект еще не успел дойти из СУБД, принудительно
      // посылаем Ktor-запрос за детальной анкетой БТИ, чтобы со стопроцентной гарантией наполнить вкладку!
      screenModelScope.launch {
        println("[YkisLogKMP.$className.$methodName]: [CASCADE_LOAD_COLD_START] Принудительный запуск getApartment для холодного старта.")
        getApartment(addressId = addressId)
      }
    }
  }


} // Абсолютний кінець і фінальне запечатування всього класу ApartmentScreenModel
