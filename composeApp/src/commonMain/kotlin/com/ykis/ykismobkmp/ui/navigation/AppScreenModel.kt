package com.ykis.ykismobkmp.ui.navigation
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val className = "AppScreenModel"
private const val TERMS_ACCEPTED_KEY = "ykis_terms_accepted_key"

/**
 * [AppScreenModel] — Диспетчер холодного старта приложения ЮКИС г. Южное.
 * ИСПРАВЛЕНО: Исправлен ошибочный вызов isNullOrBlank() на объекте пользователя FirebaseUser.
 */
class AppScreenModel(
  private val firebaseService: FirebaseService,
  private val apartmentScreenModel: ApartmentScreenModel,
  private val appCache: Settings // Внедряем общий КМР-кэш (Settings) через Koin мост
) : ScreenModel {

  private val _startState = MutableStateFlow<AppStartState>(AppStartState.Loading)
  val startState: StateFlow<AppStartState> = _startState.asStateFlow()

  init {
    evaluateStartDestination()
  }

  /**
   * [evaluateStartDestination] — Вычисление стартового экрана на основе флагов оферты, авторизации и ЖКХ-ролей.
   * Логирование рантайма согласно правилу [Класс.Метод].
   */
  private fun evaluateStartDestination() {
    screenModelScope.launch {
      // 1. Считываем флаг принятия оферты из общего кроссплатформенного кэша Settings
      val isTermsAccepted = appCache.getBoolean(key = TERMS_ACCEPTED_KEY, defaultValue = false)

      if (!isTermsAccepted) {
        println("[$className.evaluateStartDestination]: Користувач ще не прийняв умови оферти. Перехід на TermsAndConditions")
        _startState.value = AppStartState.TermsAndConditions
        return@launch
      }

      // 2. ПРОВЕРКА АВТОРИЗАЦИИ (Firebase Auth KMP)
      // ИСПРАВЛЕНО: Заменен ложный строковый вызов на атомарную КМР-проверку существования сессии GitLive
      val hasActiveUser = firebaseService.isUserAuthenticatedInFirebase
      if (!hasActiveUser) {
        println("[$className.evaluateStartDestination]: Сесія відсутня. Перенаправлення на вікно входу SignIn")
        _startState.value = AppStartState.SignIn
        return@launch
      }

      // Извлекаем текущий стейт жилого фонда БТИ из ApartmentScreenModel
      val baseUIState = apartmentScreenModel.uiState.value

      // 3. АДАПТИВНОЕ РАЗВЕТВЛЕНИЕ НА ОСНОВЕ РОЛИ И ПРИВЯЗАННЫХ КВАРТИР ЮКИС
      if (baseUIState.userRole == UserRole.StandardUser) {
        if (baseUIState.apartments.isEmpty()) {
          println("[$className.evaluateStartDestination]: Житель не має прив'язаних квартир. Перехід на AddApartment")
          _startState.value = AppStartState.AddApartment
        } else {
          println("[$className.evaluateStartDestination]: Перехід на головний ЖКХ-модуль биллинга InfoApartment")
          _startState.value = AppStartState.InfoApartment
        }
      } else {
        // Логика администратора / диспетчера коммунальных служб Южного
        val isAdminRegistered = baseUIState.osbbId != 0L
        if (!isAdminRegistered) {
          println("[$className.evaluateStartDestination]: Адмін не закріплений за підприємством. Перехід на AddApartment")
          _startState.value = AppStartState.AddApartment
        } else {
          println("[$className.evaluateStartDestination]: Авторизовано керівника/диспетчера. Перехід до списку чатів UserList")
          _startState.value = AppStartState.UserList
        }
      }
    }
  }

  /**
   * [acceptTermsAndConditions] — Метод сохранения флага оферты при тапе по кнопке "Принять".
   */
  fun acceptTermsAndConditions(onSuccess: () -> Unit) {
    screenModelScope.launch {
      appCache.putBoolean(key = TERMS_ACCEPTED_KEY, value = true)
      println("[$className.acceptTermsAndConditions]: Клієнт успішно підтвердив умови оферти ЮКИС. Флаг запісовано в кЕш.")
      evaluateStartDestination() // Пересчитываем стейт для автоматического перехода на SignIn
      onSuccess()
    }
  }
}

