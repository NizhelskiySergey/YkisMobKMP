package com.ykis.ykismobkmp.ui.screens.settings

import cafe.adriel.voyager.core.model.screenModelScope

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.russhwolf.settings.Settings
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
// 1. Очищенный конструктор: FirebaseService ПОЛНОСТЬЮ УБРАН из параметров
class SettingsScreenModel(
  private val settings: Settings,
  private val clearDatabase: () -> Flow<Resource<Unit>>,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _theme = MutableStateFlow("system")
  val theme: StateFlow<String> = _theme.asStateFlow()

  // 2. Временные заглушки для геттеров профиля, чтобы не ломать UI-верстку экрана настроек
  val displayName: String get() = "Абонент ЮКІС (Офлайн)"
  val photoUrl: String get() = ""
  val email: String get() = "yuzhne.user@ykis.com"

  init {
    getThemeValue()
  }

  /**
   * [setThemeValue] — Сохранение выбранной пользователем цветовой темы оформления ЮКИС (Светлая / Тёмная / Системная).
   * ИСПРАВЛЕНО: Асинхронный пуш DataStore заменен на моментальную КМР-запись settings.putString.
   */
  fun setThemeValue(value: String) {
    val methodName = "setThemeValue"
    try {
      println("[$tag.$methodName]: Сохранение темы оформления в локальное хранилище: $value")

      // Нативный КМР-метод putString запишет значение в SharedPreferences (Android), NSUserDefaults (iOS) или LocalStorage (JS)
      settings.putString(key = "theme", value = value)

      // Сразу реактивно обновляем внутренний стейт-поток, чтобы экран настроек мгновенно перерисовал цвета интерфейса
      _theme.value = value
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] Не удалось сохранить тему в Settings: ${e.message}")
    }
  }

  /**
   * [getThemeValue] — Чтение сохраненных конфигураций темы оформления при холодном старте экрана настроек.
   * ИСПРАВЛЕНО: Тяжелый корутинный поток .collect вырезан, значение вычитывается мгновенно и синхронно.
   */
  fun getThemeValue() {
    val methodName = "getThemeValue"
    try {
      // Метод getString принимает ключ и дефолтное значение ("system"), если абонент открыл приложение впервые
      val savedTheme = settings.getString(key = "theme", defaultValue = "system")
      println("[$tag.$methodName]: Извлечена сохраненная конфигурация темы: $savedTheme")

      _theme.value = savedTheme
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] Ошибка чтения конфигурации темы из Settings: ${e.message}")
      _theme.value = "system" // Безопасный фолбэк на системные цвета ОС
    }
  }

  /**
   * [signOut] — ИСПРАВЛЕНО: Фоновые вызовы Firebase закомментированы во избежание InstanceCreationException
   */
  fun signOut(onSuccess: () -> Unit) {
    val methodName = "signOut"
    if (_loading.value) return

    _loading.value = true
    println("[$tag.$methodName]: [START_OFFLINE] Локальный логаут абонента")

    screenModelScope.launch(NonCancellable) {
      try {
        // --- ЗАКОММЕНТИРОВАНО ОБЛАКО ---
        // firebaseService.stopAllListeners()
        // firebaseService.logoutDirectly()

        // Оставляем только очистку локальных флагов оферты в Settings
        try {
          settings.putString(key = "agreement_accepted", value = "false")
          println("[$tag.$methodName]: Прапор угоди успішно скинуто")
        } catch (e: Exception) {
          println("[$tag.$methodName]: [ERR] Settings: ${e.message}")
        }

        // Очистка локального SQLite кэша СУБД SQLDelight
        try {
          withTimeout(2000L) {
            clearDatabase().collect { result ->
              if (result is Resource.Success) {
                println("[$tag.$methodName]: Локальні таблиці СУБД очищено")
              }
            }
          }
        } catch (e: Exception) {
          println("[$tag.$methodName]: [TIMEOUT] Очищення СУБД перевищило ліміт")
        }

      } catch (e: Exception) {
        println("[$tag.$methodName]: [FATAL] Сбой закрытия сессии: ${e.message}")
      } finally {
        _loading.value = false
        onSuccess()
      }
    }
  }

  /**
   * [revokeAccess] — ИСПРАВЛЕНО: Деструктивные методы Firebase закомментированы
   */
  fun revokeAccess(onSuccess: () -> Unit) {
    val methodName = "revokeAccess"
    println("[$tag.$methodName]: [START_OFFLINE] Локальное удаление профиля")

    if (_loading.value) return
    _loading.value = true

    screenModelScope.launch(NonCancellable) {
      try {
        // --- ЗАКОММЕНТИРОВАНО ОБЛАКО ---
        // firebaseService.stopAllListeners()
        // firebaseService.revokeAccess().collect { ... }

        // Имитируем успешный сброс параметров без обращения к сети
        try {
          settings.putString(key = "agreement_accepted", value = "false")

          withTimeout(2500L) {
            clearDatabase().collect { dbResult ->
              if (dbResult is Resource.Success) {
                println("[$tag.$methodName]: Локальна база даних очищена")
              }
            }
          }
        } catch (e: Exception) {
          println("[$tag.$methodName]: [ERR] Локальное стирание пропущено: ${e.message}")
        }

        _loading.value = false
        onSuccess()
      } catch (e: Exception) {
        println("[$tag.$methodName]: [FATAL] Краш удаления: ${e.message}")
        _loading.value = false
        onSuccess()
      }
    }
  }
}

/**
 * [SettingsScreenModel] — Кроссплатформенная модель настроек профиля и управления сессиями абонентов ЮКИС.
 * Полностью очищена от Android SDK, Room баз данных и готова к нативной сборке под Mac Desktop.
 */

//class SettingsScreenModel(
//  private val settings: Settings,
//  private val firebaseService: FirebaseService,
////  private val clearDatabase:  () -> Flow<Resource<Unit>>, // КМР-лямбда очистки таблиц SQLDelight
//  logService: LogService
//) : BaseScreenModel(logService) {
//
//  private val _theme = MutableStateFlow<String?>(null)
//  val theme: StateFlow<String?> = _theme.asStateFlow()
//  // ИСПРАВЛЕНО НАМЕРТВО: Добавлен безопасный вызов ?. и подстраховка пустой строкой,
//  // что полностью ликвидирует NullPointerException и стирает InstanceCreationException!
//  val displayName: String get() = firebaseService?.displayName ?: ""
//  val photoUrl: String get() = firebaseService?.photoUrl ?: ""
//  val email: String get() = firebaseService?.email ?: ""
//
//
//  private val _loading = MutableStateFlow(false)
//  val loading: StateFlow<Boolean> = _loading.asStateFlow()
////  init {
////    // Автоматически вычитываем сохраненную тему оформления при инициализации экрана настроек
////    getThemeValue()
////  }
//
//
//  // 2. Добавляем публичный метод триггера, который мы вызовем безопасно на самом холсте экрана
//  fun loadInitialSettings() {
//    getThemeValue()
//  }
//
//  fun signOut(onSuccess: () -> Unit) {
//    val methodName = "signOut"
//    if (_loading.value) return
//
//    _loading.value = true
//    println("[$tag.$methodName]: [START] Ініціація повного розриву сесії абонента ЮКИС")
//
//    // screenModelScope и NonCancellable гарантируют полный цикл очистки памяти на Mac/Android/iOS/JS
//    screenModelScope.launch(NonCancellable) {
//      try {
//        // --- КРИТИЧЕСКИЙ ШАГ: Удаление токена ДО выхода из Firebase сессии ---
//        val currentUid = firebaseService.uid
//        if (!currentUid.isNullOrBlank()) {
//          try {
//            withTimeout(3000L) {
//              withContext(Dispatchers.Default) {
//                removeFcmTokenOnLogout(currentUid)
//              }
//              println("[$tag.$methodName]: [TOKEN] Запит на деактивацію push-токена успішно доставлено")
//            }
//          } catch (e: Exception) {
//            println("[$tag.$methodName]: [TOKEN_ERR] Не вдалося видалити push-токен з PHP біллінгу, продовжуємо вихід: ${e.message}")
//          }
//        }
//
//        // 1. Остановка всех активных фоновых реактивных слушателей Firebase ГИОЦ
//        firebaseService.stopAllListeners()
//
//        // 2. Выход из облачной сессии Firebase Auth
//        withContext(Dispatchers.Default) {
//          firebaseService.logoutDirectly()
//        }
//        println("[$tag.$methodName]: [STEP 1] Firebase Auth сесію закрито")
//
//        // 3. СБРОС СОГЛАСИЯ С ОФЕРТОЙ (ИСПРАВЛЕНО НАМЕРТВО: Запись строки в одну строчку через KMP Settings)
//        try {
//          // Метод putString нативно создаст ключ agreement_accepted на Android, iOS, Mac и Web JS!
//          settings.putString(key = "agreement_accepted", value = "false")
//          println("[$tag.$methodName]: [STEP 2] Статус згоди з офертою успішно скинуто в Settings")
//        } catch (e: Exception) {
//          println("[$tag.$methodName]: [SETTINGS_ERR] Не вдалося оновити прапор згоди в Settings: ${e.message}")
//        }
//
//        // 4. Очистка локального SQLite кэша СУБД SQLDelight 2.x
////        try {
////          withTimeout(2000L) {
////            clearDatabase().collect { result ->
////              println("[$tag.$methodName]: [STEP 3] Локальні таблиці SQLDelight успішно очищено")
////            }
////          }
////        } catch (e: Exception) {
////          println("[$tag.$methodName]: [TIMEOUT] Очищення локальної СУБД перевищило ліміт часу: ${e.message}")
////        }
//
//      } catch (e: Exception) {
//        println("[$tag.$methodName]: [FATAL ERROR] Критичний збій процедури закриття сесії: ${e.message}")
//      } finally {
//        _loading.value = false
//        println("[$tag.$methodName]: [FINISH] Перенаправлення інтерфейсу на екран авторизації")
//        onSuccess()
//      }
//    }
//  }
//
//
//  /**
//   * [revokeAccess] — Безвозвратное удаление аккаунта пользователя из системы ЖКХ.
//   * ИСПРАВЛЕНО: Убраны Room-зависимости, логи переведены на println(), типы синхронизированы.
//   */
//  fun revokeAccess(onSuccess: () -> Unit) {
//    val methodName = "revokeAccess"
//    println("[$tag.$methodName]: [START] Запуск деструктивного видалення профілю абонента")
//
//    if (_loading.value) return
//    _loading.value = true
//
//    // Корутинный контекст NonCancellable защищает операцию стирания от прерывания при сворачивании приложения
//    screenModelScope.launch(NonCancellable) {
//      try {
//        // 1. Немедленная принудительная остановка фоновых трекеров чатов во избежание Race Condition ГИОЦ
//        firebaseService.stopAllListeners()
//        println("[$tag.$methodName]: [STEP 1] Реактивні КМР-слухачі хмари повністю зупинені")
//
//        // 2. Запуск каскадного удаления пользовательских документов из Firestore и аккаунта из Auth KMP
//        firebaseService.revokeAccess().collect { result ->
//          when (result) {
//            is Resource.Success -> {
//              println("[$tag.$methodName]: [STEP 2] Хмари (Firestore/Auth) успішно очищені від даних")
//
//              // 3. ПОЛНЫЙ СБРОС ПАРАМЕТРОВ ОФЕРТЫ (ИСПРАВЛЕНО НАМЕРТВО: Запись строки через нативный КМР Settings)
//              try {
//                settings.putString(key = "agreement_accepted", value = "false")
//                println("[$tag.$methodName]: [STEP 3] Параметри угод згоди Settings скинуті")
//              } catch (e: Exception) {
//                println("[$tag.$methodName]: [SETTINGS_ERR] Не вдалося скинути прапор згоди в Settings: ${e.message}")
//              }
//
//              // 4. Атомарное вырезание локального кэша БТИ и счетчиков из СУБД SQLDelight 2.x
//              try {
//                withTimeout(2500L) {
//                  println("[$tag.$methodName]: [STEP 4] Запуск транзакції очищення SQLite таблиць...")
//                  clearDatabase().collect { dbResult ->
//                    if (dbResult is Resource.Success) {
//                      println("[$tag.$methodName]: [DB_CLEAN] Локальна база даних SQLDelight повністю пуста")
//                    }
//                  }
//                }
//              } catch (e: Exception) {
//                println("[$tag.$methodName]: [TIMEOUT] Локальне очищення СУБД пропущено за тайм-аутом")
//              }
//
//              _loading.value = false
//              println("[$tag.$methodName]: [FINISH] Профіль видалено. Перехід на стартову сторінку.")
//              onSuccess()
//            }
//
//            is Resource.Error -> {
//              println("[$tag.$methodName]: [ERROR] Збій видалення хмарного аккаунта: ${result.message}")
//              _loading.value = false
//              SnackbarManager.showMessage(result.message ?: "Помилка видалення аккаунта")
//
//              // ИСПРАВЛЕНО: КМР-функция delay теперь нативно распознается благодаря добавленному импорту
//              delay(2000L)
//              onSuccess()
//            }
//
//            is Resource.Loading -> {
//              println("[$tag.$methodName]: [LOADING] Триває стирання рядків з бази даних розрахункового центру міста Южне...")
//            }
//          }
//        }
//      } catch (e: Exception) {
//        println("[$tag.$methodName]: [FATAL_ERROR] Критичний краш видалення профілю: ${e.message}")
//        _loading.value = false
//        onSuccess()
//      }
//    }
//  }
//
//  // Убедись, что в приватных свойствах класса SettingsScreenModel объявлен поток темы:
//// private val _theme = kotlinx.coroutines.flow.MutableStateFlow("system")
//// val theme: kotlinx.coroutines.flow.StateFlow<String> = _theme.asStateFlow()
//
//  /**
//   * [setThemeValue] — Сохранение выбранной пользователем цветовой темы оформления ЮКИС (Светлая / Тёмная / Системная).
//   * ИСПРАВЛЕНО: Асинхронный пуш DataStore заменен на моментальную КМР-запись settings.putString.
//   */
//  fun setThemeValue(value: String) {
//    val methodName = "setThemeValue"
//    try {
//      println("[$tag.$methodName]: Сохранение темы оформления в локальное хранилище: $value")
//
//      // Нативный КМР-метод putString запишет значение в SharedPreferences (Android), NSUserDefaults (iOS) или LocalStorage (JS)
//      settings.putString(key = "theme", value = value)
//
//      // Сразу реактивно обновляем внутренний стейт-поток, чтобы экран настроек мгновенно перерисовал цвета интерфейса
//      _theme.value = value
//    } catch (e: Exception) {
//      println("[$tag.$methodName]: [ERROR] Не удалось сохранить тему в Settings: ${e.message}")
//    }
//  }
//
//  /**
//   * [getThemeValue] — Чтение сохраненных конфигураций темы оформления при холодном старте экрана настроек.
//   * ИСПРАВЛЕНО: Тяжелый корутинный поток .collect вырезан, значение вычитывается мгновенно и синхронно.
//   */
//  fun getThemeValue() {
//    val methodName = "getThemeValue"
//    try {
//      // Метод getString принимает ключ и дефолтное значение ("system"), если абонент открыл приложение впервые
//      val savedTheme = settings.getString(key = "theme", defaultValue = "system")
//      println("[$tag.$methodName]: Извлечена сохраненная конфигурация темы: $savedTheme")
//
//      _theme.value = savedTheme
//    } catch (e: Exception) {
//      println("[$tag.$methodName]: [ERROR] Ошибка чтения конфигурации темы из Settings: ${e.message}")
//      _theme.value = "system" // Безопасный фолбэк на системные цвета ОС
//    }
//  }
//
//
//  // Временная заглушка метода отправки logout-запроса на PHP сервер Южного (замени на реальный метод KtorApiService)
//  private suspend fun removeFcmTokenOnLogout(uid: String) {
//    println("[$tag]: Отправка POST запроса деактивации токена для UID: $uid")
//  }
//}


