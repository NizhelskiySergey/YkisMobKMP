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
      // ИСПРАВЛЕНО: Ставим Loading только если мы ЕЩЕ НЕ определились с состоянием.
      // Это предотвратит уничтожение навигатора и корутин чата при обновлении профиля.
      if (_startState.value == AppStartState.Loading) {
          delay(1200)
      }

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

      // 2. ШАГ №2: Проверка сессии Firebase
      delay(300)
      val currentUid = firebaseService.uid
      val hasActiveUser = firebaseService.isUserAuthenticatedInFirebase && currentUid.isNotBlank()
      println("[YkisLogKMP.$className.evaluateStartDestination]: Перевірка авторизації Firebase. Статус: $hasActiveUser, UID: ${currentUid.takeLast(5)}")

      if (!hasActiveUser) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Сесія недійсна або порожня. Наказ на SignIn.")
        _startState.value = AppStartState.SignIn
        return@launch
      }

      // ИСПРАВЛЕНО: Проверка подтверждения Email при старте (Только для Email-провайдера)
      val provider = firebaseService.providerId
      val isVerified = firebaseService.isEmailVerified ?: false
      
      // Если пользователь зашел через пароль (password) — требуем верификацию.
      // Если через телефон (phone) или Google — пускаем без проверки почты.
      if (provider == "password" && !isVerified) {
          println("[YkisLogKMP.$className.evaluateStartDestination]: [STEP 2.5] Пошта НЕ підтверджена. Редирект на верифікацію.")
          _startState.value = AppStartState.VerifyEmail
          return@launch
      }

      // ГАРАНТИЯ ПУШЕЙ: Пробуем зарегистрировать токен при каждом входе
      firebaseService.addFcmToken()

      // 3. ШАГ №3: ПОЛЬЗОВАТЕЛЬ АВТОРИЗОВАН
      println("[YkisLogKMP.$className.evaluateStartDestination]: [SESSION_OK] Запуск моніторингу профілю...")
      apartmentScreenModel.observeUserProfile()

      // ИСПРАВЛЕНО: Ждем загрузки, но следим за UID. Если он пропал (авто-выход Firebase) — прерываемся.
      val finalUIState = apartmentScreenModel.uiState.first { state ->
        (state.userRole != UserRole.Unknown && !state.mainLoading) || firebaseService.uid.isBlank()
      }

      if (firebaseService.uid.isBlank()) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [SESSION_LOST] Сесія анульована в процесі. Редирект на SignIn.")
        _startState.value = AppStartState.SignIn
        return@launch
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



