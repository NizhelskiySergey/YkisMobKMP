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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

enum class ListMode { RAIONS, HOUSES, APARTMENTS }

class ApartmentScreenModel(
  val firebaseService: FirebaseService,
  private val apartmentService: ApartmentService,
  logService: LogService,
) : BaseScreenModel(logService) {
  private val className = "ApartmentScreenModel"

  val uid get() = firebaseService.uid
  val email get() = firebaseService.email

  private var observeJob: Job? = null
  private val _secretCode = MutableStateFlow("")
  val secretCode: StateFlow<String> = _secretCode.asStateFlow()

  private val _drawerHouses = MutableStateFlow<List<HouseEntity>>(emptyList())
  val drawerHouses = _drawerHouses.asStateFlow()

  private val _drawerApartments = MutableStateFlow<List<ApartmentEntity>>(emptyList())
  val drawerApartments = _drawerApartments.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()

  init {
    _uiState.update { it.copy(mainLoading = false) }
  }

  private val _apartmentFilterData = _uiState.map {
    Triple(it.listMode, it.userRole, it.apartments)
  }.distinctUntilChanged()

  val filteredApartments: StateFlow<List<ApartmentEntity>> = combine(
    _searchQuery,
    _apartmentFilterData,
    _drawerHouses,
    _drawerApartments
  ) { query, filterData, _, drApts ->
    val (listMode, userRole, apartments) = filterData
    if (query.isEmpty() || listMode != ListMode.APARTMENTS) return@combine emptyList()
    val source = if (userRole != UserRole.StandardUser && userRole != UserRole.OsbbUser) {
        if (drApts.isNotEmpty()) drApts else apartments
    } else apartments
    source.filter {
      it.address.contains(query, ignoreCase = true) ||
        (it.nanim?.contains(query, ignoreCase = true) == true) ||
        it.addressId.toString().contains(query)
    }
  }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun onSearchQueryChanged(newQuery: String) { _searchQuery.value = newQuery }

  fun onRaionSelected(raion: RaionEntity) {
    _uiState.update { it.copy(selectedRaionId = raion.raionId, listMode = ListMode.HOUSES) }
    screenModelScope.launch {
      apartmentService.getHouseList(raion.raionId).collect { result ->
        if (result is Resource.Success) {
          _drawerHouses.value = result.data ?: emptyList()
          _uiState.update { it.copy(isLoading = false) }
        } else if (result is Resource.Loading) _uiState.update { it.copy(isLoading = true) }
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
        } else if (result is Resource.Loading) _uiState.update { it.copy(isLoading = true) }
      }
    }
  }

  fun onSecretCodeChanged(newCode: String) { _secretCode.value = newCode }

  fun observeUserProfile() {
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

        val officialOrgName = if (!user.osbb.isNullOrBlank()) user.osbb else {
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
          println("[YkisLogKMP.$className]: Error: ${e.message}")
      } finally {
          _uiState.update { it.copy(mainLoading = false) }
      }
    }
  }

  private suspend fun handleOsbbAdminResult(result: Resource<List<ApartmentEntity>>, uid: String, role: UserRole, osbbId: Long, currentOsbbName: String?) {
    if (result is Resource.Success) {
      val incoming = result.data ?: emptyList()
      _uiState.update { state ->
        if (incoming.isNotEmpty()) {
          val target = combinedFind(incoming, state.addressId)
          val finalOsbbName = formatOsbbName(target, currentOsbbName)
          state.copy(apartments = incoming, apartment = target, isApartmentsLoaded = true, listMode = ListMode.APARTMENTS,
            addressId = target.addressId, address = target.address, osbbId = target.osmdId, osbb = finalOsbbName, mainLoading = false)
        } else state.copy(mainLoading = false, isApartmentsLoaded = true)
      }
    }
  }

  private suspend fun handleStandardUserResult(result: Resource<List<ApartmentEntity>>, uid: String, role: UserRole) {
    if (result is Resource.Loading) return
    val incoming = result.data ?: emptyList()
    _uiState.update { state ->
      if (incoming.isNotEmpty()) {
        val target = if (state.addressId == 0L) incoming.first() else (incoming.find { it.addressId == state.addressId } ?: incoming.first())
        val finalOsbbName = if (target.osbb.isNullOrBlank() || target.osbb == "0") "Мій ОСББ" else target.osbb!!
        
        if (state.addressId == 0L) {
            screenModelScope.launch {
                firebaseService.updateUserRoleAndPermissions(
                    uid = uid,
                    addressId = target.addressId,
                    userRole = role,
                    osbbId = target.osmdId,
                    displayName = target.address,
                    fio = target.nanim ?: "",
                    osbb = target.osbb
                )
            }
        }

        state.copy(apartments = incoming, apartment = target, isApartmentsLoaded = true, addressId = target.addressId,
          address = target.address, osbbId = target.osmdId, osmdId = target.osmdId, osbb = finalOsbbName, mainLoading = false, apartmentLoading = false)
      } else state.copy(mainLoading = false, apartmentLoading = false, isApartmentsLoaded = true)
    }
  }

  private fun combinedFind(list: List<ApartmentEntity>, id: Long) = list.find { it.addressId == id } ?: list.first()

  private fun formatOsbbName(apt: ApartmentEntity, current: String?): String {
      val raw = apt.osbb
      return if (raw.isNullOrBlank() || raw == "0") current ?: "ОСББ"
      else if (raw.startsWith("ОСББ", true)) raw else "ОСББ \"$raw\""
  }

  fun setAddressId(addressId: Long) {
    val currentState = _uiState.value
    val isResident = currentState.userRole == UserRole.StandardUser
    val target = currentState.apartments.find { it.addressId == addressId } ?: _drawerApartments.value.find { it.addressId == addressId }
    if (target != null) {
      _uiState.update { state ->
        val finalOsbbName = if (isResident) (if (target.osbb.isNullOrBlank() || target.osbb == "0") "Мій ОСББ" else target.osbb!!) else state.osbb
        state.copy(addressId = target.addressId, apartment = target, address = target.address, nanim = target.nanim, fio = target.nanim ?: "",
          osbb = finalOsbbName, osbbId = if (isResident) target.osmdId else state.osbbId, osmdId = if (isResident) target.osmdId else state.osmdId, apartmentLoading = false)
      }
      
      screenModelScope.launch {
        try {
            firebaseService.updateUserRoleAndPermissions(
                uid = currentState.uid ?: "",
                addressId = target.addressId,
                userRole = currentState.userRole,
                osbbId = if (isResident) target.osmdId else currentState.osbbId,
                displayName = target.address,
                fio = target.nanim ?: "",
                osbb = if (isResident) target.osbb else currentState.osbb
            )
        } catch (e: Exception) { }
      }
    }
  }

  fun getApartment(addressId: Long = _uiState.value.addressId) {
    if (addressId <= 0L) return
    apartmentService.getApartment(uid ?: "", addressId).onEach { result ->
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
      apartmentService.deleteApartment(addressId, uid ?: "").collect { result ->
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
    apartmentService.updateBti(uid ?: "", currentState.addressId, phone, email).onEach { result ->
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

  private suspend fun handleOrganizationResult(result: Resource<List<RaionEntity>>, uid: String, role: UserRole, osbbId: Long, orgName: String) {
    _uiState.update { state ->
      if (result is Resource.Success) state.copy(raions = result.data ?: emptyList(), listMode = ListMode.RAIONS, mainLoading = false, osbb = orgName)
      else state.copy(mainLoading = false)
    }
  }

  fun clearAllData() {
    observeJob?.cancel()
    _uiState.value = com.ykis.ykismobkmp.ui.BaseUIState(mainLoading = false)
  }

  fun addApartment() {
    val input = _secretCode.value.trim()
    if (input.isEmpty()) return
    val uid = firebaseService.uid ?: return
    val email = firebaseService.email ?: ""
    _secretCode.value = ""
    _uiState.update { it.copy(mainLoading = true) }
    if (input.all { it.isDigit() }) {
      apartmentService.addApartment(input, uid, email).onEach { result ->
        if (result is Resource.Success) {
            SnackbarManager.showMessage("Рахунок успішно прив'язаний")
            screenModelScope.launch {
                observeUserProfile()
                delay(3000)
                if (_uiState.value.mainLoading) _uiState.update { it.copy(mainLoading = false, isApartmentsLoaded = true) }
            }
        } else if (result is Resource.Error) {
            _uiState.update { it.copy(mainLoading = false) }
            SnackbarManager.showMessage(result.message ?: "Помилка")
        }
      }.launchIn(screenModelScope)
    }
  }

  fun onEmailChange(newValue: String) { _uiState.update { it.copy(email = newValue) } }
  fun onPhoneChange(newValue: String) { _uiState.update { it.copy(phone = newValue) } }
}
