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
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

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
        houses.asSequence()
          .filter { it.house.contains(query, ignoreCase = true) }
          .map { ApartmentEntity(address = it.house, addressId = it.houseId) }
          .toList()
      }
      ListMode.APARTMENTS -> {
        val source = if (userRole != UserRole.StandardUser && userRole != UserRole.OsbbUser) drApts else apartments
        source.filter {
          it.address.contains(query, ignoreCase = true) ||
            (it.nanim.contains(query, ignoreCase = true)) ||
            it.addressId.toString().contains(query)
        }
      }
      ListMode.RAIONS -> emptyList()
    }
  }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun onSearchQueryChanged(newQuery: String) {
    _searchQuery.value = newQuery
  }

  fun onRaionSelected(raion: RaionEntity) {
    val methodName = "onRaionSelected"
    val raionIdLong = raion.raionId
    _uiState.update { it.copy(selectedRaionId = raionIdLong, listMode = ListMode.HOUSES) }
    screenModelScope.launch {
      apartmentService.getHouseList(raionIdLong).collect { result ->
        if (result is Resource.Success) {
          _drawerHouses.value = result.data ?: emptyList()
          _uiState.update { it.copy(isLoading = false) }
        } else if (result is Resource.Loading) {
            _uiState.update { it.copy(isLoading = true) }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
      }
    }
  }

  fun goBackLevel() {
    _uiState.update { state ->
      val newMode = when (state.listMode) {
        ListMode.APARTMENTS -> ListMode.HOUSES
        ListMode.HOUSES -> {
          _drawerHouses.value = emptyList()
          ListMode.RAIONS
        }
        ListMode.RAIONS -> ListMode.RAIONS
      }
      state.copy(listMode = newMode)
    }
  }

  fun onHouseSelected(houseId: Long) {
    _uiState.update { it.copy(selectedHouseId = houseId, listMode = ListMode.APARTMENTS) }
    screenModelScope.launch {
      apartmentService.getOsbbApartmentsList(houseId, true).collect { result ->
        if (result is Resource.Success) {
          _drawerApartments.value = result.data ?: emptyList()
          _uiState.update { it.copy(isLoading = false) }
        } else if (result is Resource.Loading) {
            _uiState.update { it.copy(isLoading = true) }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
      }
    }
  }

  fun onSecretCodeChanged(newCode: String) {
    _secretCode.value = newCode
  }

  fun observeUserProfile() {
    val methodName = "observeUserProfile"
    val actualUid = firebaseService.uid ?: return
    observeJob?.cancel()

    observeJob = screenModelScope.launch {
      _uiState.update { it.copy(mainLoading = true) }
      try {
        val user = firebaseService.getUserProfile()
        val currentUserRole = UserRole.fromString(user.userRole)

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

        val officialOrgName = if (!user.osbb.isNullOrBlank()) {
            user.osbb
        } else {
            when (currentUserRole) {
                UserRole.VodokanalUser -> getString(Res.string.vodokanal)
                UserRole.YtkeUser      -> getString(Res.string.ytke)
                UserRole.TboUser       -> getString(Res.string.yzhtrans)
                else -> "ОСББ"
            }
        }

        _uiState.update {
          it.copy(
            uid = user.uid,
            userRole = currentUserRole,
            osbbId = currentOsbbId,
            osmdId = currentOsbbId,
            osbb = officialOrgName,
            displayName = user.name ?: "",
            fio = user.fio
          )
        }

        firebaseService.addFcmToken()

        when (currentUserRole) {
          UserRole.StandardUser -> {
            apartmentService.getApartmentList(user.uid).collect { result ->
              handleStandardUserResult(result, user.uid, currentUserRole)
            }
          }
          UserRole.OsbbUser -> {
            apartmentService.getOsbbApartmentsList(currentOsbbId).collect { result ->
              handleOsbbAdminResult(result, user.uid, currentUserRole, currentOsbbId, user.osbb)
            }
          }
          else -> {
            apartmentService.getRaionList(user.uid).collect { result ->
              handleOrganizationResult(result, user.uid, currentUserRole, currentOsbbId, officialOrgName)
            }
          }
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(mainLoading = false) }
      }
    }
  }

  private suspend fun handleOsbbAdminResult(
    result: Resource<List<ApartmentEntity>>,
    uid: String,
    role: UserRole,
    osbbId: Long,
    currentOsbbName: String?
  ) {
    if (result is Resource.Success) {
      val incoming = result.data ?: emptyList()
      _uiState.update { state ->
        val combined = if (state.apartments.isEmpty()) incoming else (incoming + state.apartments).distinctBy { it.addressId }
        if (combined.isNotEmpty()) {
          val target = combined.find { it.addressId == state.addressId } ?: combined.first()
          
          val rawOsbbFromDb = target.osbb
          val finalOsbbName = if (rawOsbbFromDb.isNullOrBlank() || rawOsbbFromDb == "0") {
              currentOsbbName ?: "ОСББ"
          } else {
              if (rawOsbbFromDb.startsWith("ОСББ", ignoreCase = true)) rawOsbbFromDb else "ОСББ \"$rawOsbbFromDb\""
          }

          state.copy(
            apartments = combined,
            apartment = target,
            isApartmentsLoaded = true,
            listMode = ListMode.APARTMENTS,
            addressId = target.addressId,
            address = target.address,
            osbbId = target.osmdId,
            osbb = finalOsbbName,
            nanim = target.nanim ?: "Власник не вказаний",
            fio = target.nanim ?: "",
            mainLoading = false
          )
        } else {
          state.copy(mainLoading = false, isApartmentsLoaded = true)
        }
      }
      
      val updatedName = _uiState.value.osbb
      if (updatedName.isNotBlank() && updatedName != "ОСББ" && updatedName != currentOsbbName) {
          screenModelScope.launch {
              firebaseService.updateUserRoleAndPermissions(
                  uid = uid,
                  addressId = 0L,
                  userRole = role,
                  osbbId = osbbId,
                  osbb = updatedName
              )
          }
      }
    }
  }

  private suspend fun handleStandardUserResult(
    result: Resource<List<ApartmentEntity>>,
    uid: String,
    role: UserRole,
    forcedAddressId: Long = 0L
  ) {
    if (result is Resource.Success) {
      val incoming = result.data ?: emptyList()
      _uiState.update { state ->
        val combined = if (state.apartments.isEmpty()) incoming else (incoming + state.apartments).distinctBy { it.addressId }
        if (combined.isNotEmpty()) {
          val target = when {
            forcedAddressId != 0L -> combined.find { it.addressId == forcedAddressId } ?: combined.first()
            state.addressId == 0L -> combined.first()
            else -> combined.find { it.addressId == state.addressId } ?: combined.first()
          }
          
          val rawOsbb = target.osbb
          val finalOsbbName = if (rawOsbb.isNullOrBlank() || rawOsbb == "0") "Мій ОСББ" else rawOsbb

          state.copy(
            apartments = combined,
            apartment = target,
            isApartmentsLoaded = true,
            addressId = target.addressId,
            address = target.address,
            osbbId = target.osmdId,
            osmdId = target.osmdId,
            osbb = finalOsbbName,
            nanim = target.nanim ?: "Власник не вказаний",
            fio = target.nanim ?: "",
            mainLoading = false
          )
        } else {
          state.copy(mainLoading = false, isApartmentsLoaded = true)
        }
      }
      
      // СИНХРОНИЗАЦИЯ С FIREBASE
      val currentState = _uiState.value
      val target = currentState.apartment
      if (target.addressId != 0L) {
          screenModelScope.launch {
              firebaseService.updateUserRoleAndPermissions(
                  uid = uid,
                  addressId = target.addressId,
                  userRole = role,
                  osbbId = target.osmdId,
                  displayName = target.address,
                  fio = target.nanim ?: "",
                  osbb = currentState.osbb
              )
          }
      }
    }
  }

  fun setAddressId(addressId: Long) {
    val currentState = _uiState.value
    val isResident = currentState.userRole == UserRole.StandardUser
    val target = currentState.apartments.find { it.addressId == addressId }
      ?: _drawerApartments.value.find { it.addressId == addressId }

    if (target != null) {
      _uiState.update { state ->
        val finalOsbbName = if (isResident) {
             val raw = target.osbb
             if (raw.isNullOrBlank() || raw == "0") "Мій ОСББ" else raw
        } else {
             state.osbb
        }

        state.copy(
          addressId = target.addressId,
          apartment = target,
          address = target.address,
          nanim = target.nanim,
          fio = target.nanim ?: "",
          osbb = finalOsbbName,
          osbbId = if (isResident) target.osmdId else state.osbbId,
          osmdId = if (isResident) target.osmdId else state.osmdId,
          apartmentLoading = false
        )
      }

      screenModelScope.launch {
        firebaseService.updateUserRoleAndPermissions(
          uid = currentState.uid ?: "",
          addressId = target.addressId,
          userRole = currentState.userRole,
          osbbId = if (isResident) target.osmdId else currentState.osbbId,
          displayName = target.address,
          fio = target.nanim ?: "",
          osbb = if (isResident) target.osbb else currentState.osbb
        )
      }
    }
  }

  private suspend fun handleOrganizationResult(
    result: Resource<List<RaionEntity>>,
    uid: String,
    role: UserRole,
    osbbId: Long,
    orgName: String
  ) {
    _uiState.update { state ->
      if (result is Resource.Success) {
        state.copy(
          raions = result.data ?: emptyList(),
          listMode = ListMode.RAIONS,
          mainLoading = false,
          osbb = orgName
        )
      } else {
        state.copy(mainLoading = false)
      }
    }
  }

  fun clearAllData() {
    observeJob?.cancel()
    _uiState.value = com.ykis.ykismobkmp.ui.BaseUIState(mainLoading = false)
    _drawerHouses.value = emptyList()
    _drawerApartments.value = emptyList()
  }

  fun addApartment() {
    val input = _secretCode.value.trim()
    if (input.isEmpty()) return
    val uid = firebaseService.uid ?: return
    val email = firebaseService.email ?: ""
    _secretCode.value = ""

    if (input.all { it.isDigit() }) {
      apartmentService.addApartment(input, uid, email).onEach { result ->
        handleApartmentResult(uid, result)
      }.launchIn(screenModelScope)
    } else {
      apartmentService.verifyAdminCode(input, uid).onEach { result ->
        handleAdminResult(result)
      }.launchIn(screenModelScope)
    }
  }

  private suspend fun handleApartmentResult(uid: String, result: Resource<GetSimpleResponse>) {
    when (result) {
      is Resource.Loading -> _uiState.update { it.copy(mainLoading = true) }
      is Resource.Success -> {
        val data = result.data ?: return
        val newAddressId = data.addressId ?: 0L
        val newOsbbId = data.osbbId ?: 0L
        val finalOsbbName = data.osbb ?: "Мій ОСББ"
        
        _uiState.update { state ->
          state.copy(
            addressId = newAddressId,
            address = data.address ?: "",
            nanim = data.nanim,
            fio = data.nanim ?: "",
            osbb = finalOsbbName,
            osbbId = newOsbbId,
            userRole = UserRole.StandardUser,
            mainLoading = false
          )
        }
        
        firebaseService.updateUserRoleAndPermissions(
          uid = uid,
          addressId = newAddressId,
          userRole = UserRole.StandardUser,
          osbbId = newOsbbId,
          displayName = data.address ?: "",
          fio = data.nanim ?: "",
          osbb = finalOsbbName
        )

        apartmentService.initResidentChats(
          scope = screenModelScope,
          uid = uid,
          osbbId = newOsbbId,
          addressId = newAddressId,
          addressText = data.address ?: "",
          nanim = data.nanim ?: ""
        )
        
        observeUserProfile()
        SnackbarManager.showMessage("Рахунок успішно прив'язаний")
      }
      is Resource.Error -> {
        _uiState.update { it.copy(mainLoading = false) }
        SnackbarManager.showMessage(result.message ?: "Помилка")
      }
    }
  }

  private fun handleAdminResult(result: Resource<GetSimpleResponse>) {
    when (result) {
      is Resource.Success -> {
        val data = result.data ?: return
        val mappedRole = UserRole.fromString(data.userRole)
        val newOsbbId = when (mappedRole) {
            UserRole.VodokanalUser -> 9999L
            UserRole.YtkeUser -> 9998L
            UserRole.TboUser -> 9997L
            else -> data.osbbId ?: 0L
        }
        
        screenModelScope.launch {
          firebaseService.updateUserRoleAndPermissions(
            uid = uid,
            addressId = 0L,
            userRole = mappedRole,
            osbbId = newOsbbId,
            displayName = data.osbb ?: "ОСББ",
            osbb = data.osbb ?: "ОСББ"
          )
          observeUserProfile()
          SnackbarManager.showMessage("Авторизація адміністратора успішна")
        }
      }
      is Resource.Error -> SnackbarManager.showMessage(result.message ?: "Невірний код")
      is Resource.Loading -> _uiState.update { it.copy(mainLoading = true) }
    }
  }

  fun getApartment(addressId: Long = _uiState.value.addressId) {
    if (addressId <= 0L) return
    apartmentService.getApartment(uid, addressId).onEach { result ->
      when (result) {
        is Resource.Success -> {
          val data = result.data ?: ApartmentEntity()
          _uiState.update { it.copy(apartment = data, apartmentLoading = false) }
        }
        is Resource.Error -> _uiState.update { it.copy(apartmentLoading = false) }
        is Resource.Loading -> _uiState.update { it.copy(apartmentLoading = true) }
      }
    }.launchIn(screenModelScope)
  }

  fun deleteApartmentFromProfile(addressId: Long, onNavigateToAddScreen: () -> Unit) {
    launchCatching(showLoader = true) {
      apartmentService.deleteApartment(addressId, uid).collect { result ->
        if (result is Resource.Success) {
           _uiState.update { state ->
              val newList = state.apartments.filter { it.addressId != addressId }
              if (newList.isEmpty()) {
                  onNavigateToAddScreen()
                  state.copy(apartments = emptyList(), addressId = 0L)
              } else {
                  state.copy(apartments = newList)
              }
           }
           if (_uiState.value.addressId != 0L) observeUserProfile()
        }
      }
    }
  }

  fun onUpdateBti(phone: String, email: String) {
    val currentState = _uiState.value
    apartmentService.updateBti(uid, currentState.addressId, phone, email).onEach { result ->
      if (result is Resource.Success) {
          SnackbarManager.showMessage("Дані оновлено")
          getApartment(currentState.addressId)
      }
    }.launchIn(screenModelScope)
  }

  fun initialContactState() {
    val currentState = _uiState.value
    _uiState.update { it.copy(
        email = currentState.apartment.email.takeIf { it.isNotBlank() } ?: currentState.email ?: "",
        phone = currentState.apartment.phone.takeIf { it.isNotBlank() } ?: currentState.phone ?: ""
    )}
  }

  fun onEmailChange(newValue: String) { _uiState.update { it.copy(email = newValue) } }
  fun onPhoneChange(newValue: String) { _uiState.update { it.copy(phone = newValue) } }
  
  private fun String.isValidEmailKmp(): Boolean = this.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$".toRegex())
}
