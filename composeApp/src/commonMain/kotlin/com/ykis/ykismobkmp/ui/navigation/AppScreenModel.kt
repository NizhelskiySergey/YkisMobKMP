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
      println("[YkisLogKMP.$className.evaluateStartDestination]: >>> ЗАПУСК ПЕРЕВІРКИ (v.1.0.6) <<<")
      
      // 0. ТЕХНІЧНА ПАУЗА ДЛЯ WEB (щоб Firebase встиг прокинутись)
      if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
          delay(1000)
      }

      // 1. ШАГ №1: Перевірка ліцензійної угоди (Оферти ГІОЦ)
      val isTermsAccepted = appCache.getBoolean(key = TERMS_ACCEPTED_KEY, defaultValue = false)

      if (!isTermsAccepted) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 1] Оферта не прийнята.")
        val isSuccess = firebaseService.fetchConfiguration()
        cachedTermsText = firebaseService.agreementText
        _startState.value = AppStartState.TermsAndConditions
        return@launch
      }

      // 2. ШАГ №2: Проверка сессии Firebase
      println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Перевірка Auth стейту...")
      
      // ИСПРАВЛЕНО: Ждем появления UID до 4 секунд
      var attempts = 0
      while (firebaseService.uid.isBlank() && attempts < 20) {
          delay(200)
          attempts++
          if (attempts % 5 == 0) {
              println("[YkisLogKMP.$className.evaluateStartDestination]: Очікування Firebase... (${attempts*200}ms)")
          }
      }

      val finalUid = firebaseService.uid
      if (finalUid.isBlank()) {
          println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Сесія НЕ знайдена за 4 сек. Перехід на SignIn.")
          _startState.value = AppStartState.SignIn
          return@launch
      }

      println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Сесія відновлена: ${finalUid.takeLast(5)}")


      // Если локальная сессия есть, пробуем обновить её по сети (для выявления удаленных аккаунтов)
      println("[YkisLogKMP.$className.evaluateStartDestination]: Спроба оновлення сесії...")
      firebaseService.reloadFirebaseUser() 
      
      // Теперь проверяем статус СНОВА после попытки обновления.
      // Если аккаунт был удален в консоли, firebaseService.isUserAuthenticatedInFirebase станет false.
      val isStillAuthenticated = firebaseService.isUserAuthenticatedInFirebase && firebaseService.uid.isNotBlank()

      println("[YkisLogKMP.$className.evaluateStartDestination]: Перевірка після reload. Статус: $isStillAuthenticated")

      if (!isStillAuthenticated) {
        println("[YkisLogKMP.$className.evaluateStartDestination]: [ШАГ 2] Аккаунт видалено або сесію анульовано сервером. Наказ на SignIn.")
        firebaseService.signOut()
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
      println("[YkisLogKMP.$className.evaluateStartDestination]: [SESSION_OK] Завантаження профілю...")
      
      // ИСПРАВЛЕНО: Перед переходом принудительно получаем свежий профиль из Firestore
      val profile = firebaseService.getUserProfile()
      println("[YkisLogKMP.$className.evaluateStartDestination]: Профіль отримано. Роль: ${profile.userRole}")
      
      apartmentScreenModel.observeUserProfile()

      // Ждем загрузки стейта, где роль совпадает с полученной из профиля
      val finalUIState = apartmentScreenModel.uiState.first { state ->
        (state.userRole.getSerialName() == profile.userRole && !state.mainLoading) || firebaseService.uid.isBlank()
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



