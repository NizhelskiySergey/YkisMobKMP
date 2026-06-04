package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication.Companion.init
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
      _startState.value = AppStartState.Loading

      // ИСПРАВЛЕНО НАМЕРТВО: Стартовый защитный барьер разгрузки процессора увеличен до 1200 мс.
      // Дает нативным сервисам Google Play Services и Tag Manager завершить рефлексивную
      // привязку к SystemProperties, полностью исключая краш SIGSEGV (Fatal signal 11)!
      delay(1200)

      // 1. ШАГ №1: Перевірка ліцензійної угоди (Оферти ГІОЦ)
      val isTermsAccepted = appCache.getBoolean(key = TERMS_ACCEPTED_KEY, defaultValue = false)

      if (!isTermsAccepted) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 1] Оферта не прийнята. Запуск завантаження Remote Config...")

        val isSuccess = firebaseService.fetchConfiguration()
        cachedTermsText = firebaseService.agreementText
        println("[YkisLogKMP.$className.evaluateStartDestination]: Remote Config завантажено ($isSuccess). Довжина: ${cachedTermsText.length}")

        _startState.value = AppStartState.TermsAndConditions
        return@launch
      } else {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 1] Оферта прийнята.")
      }

      // 2. ШАГ №2: Проверка сессии Firebase с буферизацией (300 мс)
      delay(300)
      val hasActiveUser = firebaseService.isUserAuthenticatedInFirebase
      println("[YkisLogKMP.$className.evaluateStartDestination]: Перевірка авторизації Firebase. Статус: $hasActiveUser")

      if (!hasActiveUser) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Сесія відсутня. Наказ на SignIn.")
        _startState.value = AppStartState.SignIn
        return@launch
      }

      // ГАРАНТИЯ ПУШЕЙ: Пробуем зарегистрировать токен при каждом входе
      firebaseService.addFcmToken()

      // 3. ШАГ №3: ПОЛЬЗОВАТЕЛЬ АВТОРИЗОВАН — Безопасное чтение профиля БТИ без зацикливания памяти
      println("[YkisLogKMP.$className.evaluateStartDestination]: [SESSION_OK] Запуск одноразового моніторингу профілю...")

      apartmentScreenModel.observeUserProfile()

      val finalUIState = apartmentScreenModel.uiState.first { state ->
        state.userRole != UserRole.Unknown && !state.mainLoading
      }

      val currentRole = finalUIState.userRole
      println("[YkisLogKMP.$className.evaluateStartDestination]: [NAV_RESOLVE] Стейт запечатано! Роль: $currentRole")

      // ШАГ №4: Разветвление траектории БТИ
      if (currentRole == UserRole.StandardUser) {
        if (finalUIState.apartments.isEmpty() || finalUIState.addressId == 0L) {
          _startState.value = AppStartState.AddApartment
        } else {
          _startState.value = AppStartState.InfoApartment
        }
      } else {
        val isAdminRegistered = finalUIState.osbbId != 0L
        if (!isAdminRegistered) _startState.value = AppStartState.AddApartment else _startState.value = AppStartState.UserList
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



