package com.ykis.ykismobkmp.ui.screens.settings

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val tag = "SettingsScreenModel"

/**
 * [SettingsScreenModel] — Кроссплатформенная модель настроек профиля и управления сессиями абонентов ЮКИС.
 * Полностью очищена от Android SDK, Room баз данных и готова к нативной сборке под Mac Desktop.
 */
class SettingsScreenModel(
  private val dataStore: AppSettingsRepository, // Твой КМР-репозиторий на базе DataStore / Settings
  private val clearDatabase: suspend () -> Flow<Resource<Unit>>, // КМР-лямбда очистки таблиц SQLDelight
  private val firebaseService: FirebaseService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _theme = MutableStateFlow<String?>(null)
  val theme: StateFlow<String?> = _theme.asStateFlow()

  val displayName get() = firebaseService.displayName
  val photoUrl get() = firebaseService.photoUrl
  val email get() = firebaseService.email

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  init {
    // Автоматически вычитываем сохраненную тему оформления при инициализации экрана настроек
    getThemeValue()
  }

  /**
   * [signOut] — Безопасный выход из учетной записи биллинга ЮКИС.
   * ИСПРАВЛЕНО: Удален Android Dispatchers.Main.immediate, Room изменен на SQLDelight.
   */
  fun signOut(onSuccess: () -> Unit) {
    val methodName = "signOut"
    if (_loading.value) return

    _loading.value = true
    println("[$tag.$methodName]: [START] Инициация разрыва сессии абонента")

    // ИСПРАВЛЕНО: screenModelScope и NonCancellable гарантируют полный цикл очистки при уходе с экрана настроек
    screenModelScope.launch(NonCancellable) {
      try {
        // --- КРИТИЧЕСКИЙ ШАГ: Удаление токена ДО выхода из Firebase сессии ---
        val currentUid = firebaseService.uid
        if (!currentUid.isNullOrBlank()) {
          try {
            // Используем сжатый КМР-таймаут с пулом потоков Default
            withTimeout(3000) {
              withContext(Dispatchers.Default) {
                // Вызываем сетевой метод удаления FCM-токена из базы расчетного центра г. Южный
                removeFcmTokenOnLogout(currentUid)
              }
              println("[$tag.$methodName]: [TOKEN] Запрос на деактивацию push-токена успешно доставлен")
            }
          } catch (e: Exception) {
            println("[$tag.$methodName]: [TOKEN_ERR] Не удалось удалить push-токен из PHP биллинга, продолжаем выход: ${e.message}")
          }
        }

        // 1. Остановка всех активных фоновых реактивных слушателей Firebase
        firebaseService.stopAllListeners()

        // 2. Выход из облачной сессии Firebase Auth
        withContext(Dispatchers.Default) {
          firebaseService.logoutDirectly()
        }
        println("[$tag.$methodName]: [STEP 1] Firebase Auth сессия закрыта")

        // 3. Сброс согласия с офертой (DataStore / Multiplatform Settings)
        dataStore.putThemeStrings(key = "agreement_accepted", value = "false")

        // 4. Очистка локального SQLite кэша СУБД SQLDelight 2.x
        try {
          withTimeout(2000) {
            clearDatabase().collect { result ->
              if (result is Resource.Success) {
                println("[$tag.$methodName]: [STEP 2] Локальные таблицы SQLDelight успешно очищены")
              }
            }
          }
        } catch (e: Exception) {
          println("[$tag.$methodName]: [TIMEOUT] Очистка локальной СУБД превысила лимит времени: ${e.message}")
        }

      } catch (e: Exception) {
        println("[$tag.$methodName]: [FATAL ERROR] Критический сбой процедуры закрытия сессии: ${e.message}")
      } finally {
        _loading.value = false
        println("[$tag.$methodName]: [FINISH] Перенаправление интерфейса на экран входа")
        onSuccess()
      }
    }
  }

  /**
   * [revokeAccess] — Безвозвратное удаление аккаунта пользователя из системы ЖКХ.
   * ИСПРАВЛЕНО: Убраны Room-зависимости, логи переведены на println(), типы синхронизированы.
   */
  fun revokeAccess(onSuccess: () -> Unit) {
    val methodName = "revokeAccess"
    println("[$tag.$methodName]: [START] Запуск деструктивного удаления профиля")

    if (_loading.value) return
    _loading.value = true

    screenModelScope.launch(NonCancellable) {
      try {
        // 1. Немедленная принудительная остановка фоновых трекеров чатов во избежание Race Condition
        firebaseService.stopAllListeners()
        println("[$tag.$methodName]: [STEP 1] Реактивные КМР-слушатели облака остановлены")

        // 2. Запуск каскадного удаления пользовательских документов из Firestore и аккаунта из Auth
        firebaseService.revokeAccess().collect { result ->
          when (result) {
            is Resource.Success -> {
              println("[$tag.$methodName]: [STEP 2] Облачные хранилища (Firestore/Auth) успешно очищены")

              // 3. Полный сброс параметров конфигурации DataStore локального диска Mac/Android
              dataStore.putThemeStrings(key = "agreement_accepted", value = "false")
              println("[$tag.$methodName]: [STEP 3] Параметры соглашений DataStore сброшены")

              // 4. Атомарное вырезание локального кэша БТИ и счетчиков из SQLDelight
              try {
                withTimeout(2500) {
                  println("[$tag.$methodName]: [STEP 4] Запуск транзакции очистки SQLite...")
                  clearDatabase().collect { dbResult ->
                    if (dbResult is Resource.Success) {
                      println("[$tag.$methodName]: [DB_CLEAN] Локальная база данных SQLDelight пуста")
                    }
                  }
                }
              } catch (e: Exception) {
                println("[$tag.$methodName]: [TIMEOUT] Локальная очистка СУБД пропущена по тайм-ауту")
              }

              _loading.value = false
              println("[$tag.$methodName]: [FINISH] Профиль удален. Уход на стартовую страницу.")
              onSuccess()
            }

            is Resource.Error -> {
              println("[$tag.$methodName]: [ERROR] Сбой удаления облачного аккаунта: ${result.message}")
              _loading.value = false
              SnackbarManager.showMessage(result.message ?: "Помилка видалення аккаунта")

              // При сбое структуры облака все равно выводим интерфейс через паузу, страхуя от зависания рантайма
              delay(2000)
              onSuccess()
            }

            is Resource.Loading -> {
              println("[$tag.$methodName]: [LOADING] Идет стирание строк из базы данных расчетного центра...")
            }
          }
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: [FATAL_ERROR] Критический краш удаления: ${e.message}")
        _loading.value = false
        onSuccess()
      }
    }
  }

  fun setThemeValue(value: String) {
    screenModelScope.launch {
      dataStore.putThemeStrings(key = "theme", value = value)
    }
  }

  fun getThemeValue() {
    screenModelScope.launch {
      dataStore.getThemeStrings(key = "theme").collect { value ->
        _theme.value = value
      }
    }
  }

  // Временная заглушка метода отправки logout-запроса на PHP сервер Южного (замени на реальный метод KtorApiService)
  private suspend fun removeFcmTokenOnLogout(uid: String) {
    println("[$tag]: Отправка POST запроса деактивации токена для UID: $uid")
  }
}


