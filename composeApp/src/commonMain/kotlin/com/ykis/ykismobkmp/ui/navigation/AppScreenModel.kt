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

  private val _updateConfig = MutableStateFlow<com.ykis.ykismobkmp.domain.entity.AppUpdateConfig?>(null)
  val updateConfig: StateFlow<com.ykis.ykismobkmp.domain.entity.AppUpdateConfig?> = _updateConfig.asStateFlow()

  var cachedTermsText by mutableStateOf("")
    private set

  init {
    evaluateStartDestination()
  }

  fun evaluateStartDestination() {
    screenModelScope.launch {
      println("[YkisLogKMP.$className.evaluateStartDestination]: >>> ЗАПУСК ПЕРЕВІРКИ (v.${com.ykis.ykismobkmp.AppConfig.APP_VERSION}) <<<")
      
      // Фонова перевірка оновлень
      launch {
          println("[YkisLogKMP.$className]: Запит конфігурації оновлень...")
          val config = firebaseService.fetchAppUpdateConfig()
          if (config != null) {
              println("[YkisLogKMP.$className]: Отримано з БД: latest=${config.latestVersion}, current=${com.ykis.ykismobkmp.AppConfig.APP_VERSION}")
              if (config.latestVersion.isNotBlank() && config.latestVersion != com.ykis.ykismobkmp.AppConfig.APP_VERSION) {
                  println("[YkisLogKMP.$className]: УВАГА! Доступна нова версія!")
                  _updateConfig.value = config
              } else {
                  println("[YkisLogKMP.$className]: Версія актуальна.")
              }
          } else {
              println("[YkisLogKMP.$className]: Документ конфігурації не знайдено або помилка.")
          }
      }

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
      // Збільшено до 50 спроб (10 секунд) для повільних пристроїв
      while (firebaseService.uid.isBlank() && attempts < 50) {
          if (attempts % 10 == 0) {
              println("[YkisLogKMP.$className]: Очікування сесії... Спроба $attempts. Current user: ${firebaseService.currentUser?.email ?: "null"}")
          }
          delay(200)
          attempts++
      }

      val finalUid = firebaseService.uid
      if (finalUid.isBlank()) {
          println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Сесія НЕ знайдена. Прямий перехід на SignIn.")
          _startState.value = AppStartState.SignIn
          return@launch
      }
      println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] UID знайдено: $finalUid")

      // 2.5 ШАГ №2.5: Перевірка верифікації пошти
      println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2.5] Перевірка статусу пошти...")
      firebaseService.reloadFirebaseUser()
      
      val user = firebaseService.currentUser
      val userEmail = user?.email ?: ""
      val isVerified = user?.isEmailVerified ?: true 
      
      println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2.5] User: $userEmail, Verified: $isVerified")
      
      // ИСПРАВЛЕНО: Якщо пошти немає (вхід по телефону) або вона вже верифікована — пропускаємо
      if (userEmail.isNotBlank() && isVerified == false) {
          println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2.5] Email не підтверджений. Йдемо на VerifyEmail.")
          _startState.value = AppStartState.VerifyEmail
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

      // ДОДАНО: Примусова реєстрація Web Push токена при кожному старті
      screenModelScope.launch {
          println("[YkisLogKMP.$className]: Фонова перевірка Web Push токена...")
          firebaseService.addFcmToken()
      }

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

  fun dismissUpdateBanner() {
    _updateConfig.value = null
  }
}
