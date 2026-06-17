package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val className = "AppScreenModel"
private const val TERMS_ACCEPTED_KEY = "is_terms_accepted"

class AppScreenModel(
  val firebaseService: FirebaseService,
  private val apartmentScreenModel: ApartmentScreenModel,
  private val appCache: Settings
) : ScreenModel {

  private val _startState = MutableStateFlow<AppStartState>(AppStartState.Loading)
  val startState: StateFlow<AppStartState> = _startState.asStateFlow()

  var cachedTermsText by mutableStateOf("")
    private set

  init {
    evaluateStartDestination()
  }

  fun evaluateStartDestination() {
    screenModelScope.launch {
      println("[YkisLogKMP.$className.evaluateStartDestination]: >>> ЗАПУСК ПЕРЕВІРКИ (v.1.0.7) <<<")
      
      if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
          delay(1000)
      }

      // 1. ШАГ №1: Оферта
      val isTermsAccepted = appCache.getBoolean(key = TERMS_ACCEPTED_KEY, defaultValue = false)
      if (!isTermsAccepted) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 1] Оферта не прийнята.")
        firebaseService.fetchConfiguration()
        cachedTermsText = firebaseService.agreementText
        _startState.value = AppStartState.TermsAndConditions
        return@launch
      }

      // 2. ШАГ №2: Auth
      var attempts = 0
      while (firebaseService.uid.isBlank() && attempts < 20) {
          delay(200)
          attempts++
      }

      val finalUid = firebaseService.uid
      if (finalUid.isBlank()) {
          println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Сесія НЕ знайдена. SignIn.")
          _startState.value = AppStartState.SignIn
          return@launch
      }

      // 3. ШАГ №3: Профиль и Роль
      println("[YkisLogKMP.$className.evaluateStartDestination]: [SESSION_OK] Завантаження профілю...")
      val profile = firebaseService.getUserProfile()
      println("[YkisLogKMP.$className.evaluateStartDestination]: Профіль отримано. Роль: ${profile.userRole}")
      
      apartmentScreenModel.observeUserProfile()

      // ИСПРАВЛЕНО: Гнучке порівняння ролей для усунення зависання на лоадері.
      val finalUIState = apartmentScreenModel.uiState.first { state ->
        val stateRole = state.userRole.getSerialName()
        val profileRole = profile.userRole
        
        val isRoleMatched = stateRole.equals(profileRole, ignoreCase = true) || 
                          state.userRole.name.equals(profileRole, ignoreCase = true)

        (isRoleMatched && (!state.mainLoading || state.isApartmentsLoaded)) || firebaseService.uid.isBlank()
      }

      if (firebaseService.uid.isBlank()) {
        _startState.value = AppStartState.SignIn
        return@launch
      }

      val currentRole = finalUIState.userRole
      println("[YkisLogKMP.$className.evaluateStartDestination]: [NAV_RESOLVE] Стейт запечатано! Роль: $currentRole")

      // ШАГ №4: Навигация
      if (currentRole == UserRole.StandardUser) {
        if (finalUIState.apartments.isEmpty()) {
          _startState.value = AppStartState.AddApartment
        } else {
          _startState.value = AppStartState.InfoApartment
        }
      } else {
        if (finalUIState.osbbId == 0L) _startState.value = AppStartState.AddApartment 
        else _startState.value = AppStartState.UserList
      }
    }
  }

  fun acceptTermsAndConditions(onSuccess: () -> Unit) {
    screenModelScope.launch {
      appCache.putBoolean(key = TERMS_ACCEPTED_KEY, value = true)
      evaluateStartDestination()
      onSuccess()
    }
  }
}
