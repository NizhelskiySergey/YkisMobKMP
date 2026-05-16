package com.ykis.ykismobkmp.ui.navigation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.UserRole

// ИМПОРТ КРОСС ПЛАТФОРМЕННОГО КЭША ИЗ MULTIPLATFORM SETTINGS:
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel

private const val className = "AppScreenModel"
private const val TERMS_ACCEPTED_KEY = "ykis_terms_accepted_key"

/**
 * [AppScreenModel] — Диспетчер холодного старта приложения ЮКИС г. Южное.
 * ИСПРАВЛЕНО: Флаг принятия оферты Terms вычитывается нативно из кроссплатформенного кэша Settings.
 */
class AppScreenModel(
  private val firebaseService: FirebaseService,
  private val apartmentScreenModel: ApartmentScreenModel,
  private val appCache: Settings // Внедряем общий КМР-кэш (Settings) через Koin
) : ScreenModel {

  private val _startState = MutableStateFlow<AppStartState>(AppStartState.Loading)
  val startState: StateFlow<AppStartState> = _startState.asStateFlow()

  init {
    evaluateStartDestination()
  }

  private fun evaluateStartDestination() {
    screenModelScope.launch {
      // 1. ИСПРАВЛЕНО: Считываем флаг из общего кэша. Если ключа нет — возвращаем false (не принял)
      val isTermsAccepted = appCache.getBoolean(key = TERMS_ACCEPTED_KEY, defaultValue = false)

      if (!isTermsAccepted) {
        println("[$className]: Користувач ще не прийняв умови. Перехід на TermsAndConditions")
        _startState.value = AppStartState.TermsAndConditions
        return@launch
      }

      // 2. ПРОВЕРКА АВТОРИЗАЦИИ (Firebase Auth KMP)
      val currentUid = firebaseService.auth
      if (currentUid.isNullOrBlank()) {
        _startState.value = AppStartState.SignIn
        return@launch
      }

      // Извлекаем текущий стейт жилого фонда
      val baseUIState = apartmentScreenModel.uiState.value

      // 3. РАЗВЕТВЛЕНИЕ НА ОСНОВЕ РОЛИ И КВАРТИР БТИ
      if (baseUIState.userRole == UserRole.StandardUser) {
        if (baseUIState.apartments.isEmpty()) {
          _startState.value = AppStartState.AddApartment
        } else {
          _startState.value = AppStartState.InfoApartment
        }
      } else {
        val isAdminRegistered = baseUIState.osbbId != 0L
        if (!isAdminRegistered) {
          _startState.value = AppStartState.AddApartment
        } else {
          _startState.value = AppStartState.UserList
        }
      }
    }
  }

  /**
   * Метод сохранения флага в кэш при тапе по кнопке "Принять" на экране TermsAndConditionScreen
   */
  fun acceptTermsAndConditions(onSuccess: () -> Unit) {
    screenModelScope.launch {
      appCache.putBoolean(key = TERMS_ACCEPTED_KEY, value = true)
      println("[$className]: Клієнт успішно підтвердив умови оферти ЮКИС. Флаг запісовано в кЕш.")
      evaluateStartDestination() // Пересчитываем стейт для автоматического перехода на SignIn
      onSuccess()
    }
  }
}
