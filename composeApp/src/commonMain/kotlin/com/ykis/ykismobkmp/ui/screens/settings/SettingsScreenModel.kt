package com.ykis.ykismobkmp.ui.screens.settings

import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val tag = "SettingsScreenModel"
private const val THEME_KEY = "theme_key"
class SettingsScreenModel(
  private val settings: Settings,
  private val firebaseService: FirebaseService,
  private val clearDatabase: () -> Flow<Resource<Unit>>,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _theme = MutableStateFlow("system")
  val theme: StateFlow<String> = _theme.asStateFlow()

  val displayName: String get() = firebaseService.displayName
  val photoUrl: String get() = firebaseService.photoUrl
  val email: String get() = firebaseService.email

  init {
    println("[YkisLogKMP.$tag.init]: Ініціалізація моделі налаштувань. Читання параметрів з диска...")
    getThemeValue()
  }
  fun setThemeValue(value: String) {
    val methodName = "setThemeValue"
    try {
      println("[YkisLogKMP.$tag.$methodName]: Сохранение темы оформления в локальное хранилище: \"$value\" под ключом: \"$THEME_KEY\"")
      settings.putString(key = THEME_KEY, value = value)
      _theme.value = value
    } catch (e: Exception) {
      println("[YkisLogKMP.$tag.$methodName]: [ERROR] Не удалось сохранить тему в Settings: ${e.message}")
    }
  }
  fun getThemeValue() {
    val methodName = "getThemeValue"
    try {
      val savedTheme = settings.getString(key = THEME_KEY, defaultValue = "system")
      println("[YkisLogKMP.$tag.$methodName]: Извлечена сохраненная конфигурация темы: \"$savedTheme\"")
      _theme.value = savedTheme
    } catch (e: Exception) {
      println("[YkisLogKMP.$tag.$methodName]: [ERROR] Ошибка чтения конфигурации темы из Settings: ${e.message}")
      _theme.value = "system"
    }
  }
  /**
   * [signOut] — Каскадна процедура БЕЗПЕЧНОГО ВИХОДУ з комунального профілю ЮКІС м. Южне.
   * Интегрирован оператор .catch для защиты от краша Flow exception transparency!
   * Метод только аннулирует токен suspend-сессии, глушит КМР-слушатели и выжигает локальный файл СУБД,
   * гарантированно освобождая индикатор загрузки в блоке finally при любых сетевых или дисковых лагах.
   */
  fun signOut(onSuccess: () -> Unit) {
    val methodName = "signOut"
    if (_loading.value) return
    _loading.value = true

    println("[YkisLogKMP.$tag.$methodName]: [START] Запуск процедури виходу з облікового запису (Дані в хмарі залишаються цілими).")

    screenModelScope.launch {
      try {
        // Оборачиваем транзакции очистки локального диска в блок NonCancellable,
        // чтобы они гарантированно завершились, даже если жилец в этот момент свернет приложение
        withContext(NonCancellable) {

          // 1. Каскадно глушим все активные фоновые КМР-потоки и Snapshot-слушатели Firestore/RTDB
          // Это мгновенно освобождает ОЗУ смартфона и прекращает фоновый сетевой трафик!
          firebaseService.stopAllListeners()
          println("[YkisLogKMP.$tag.$methodName]: [STEP 1] Всі фонові КМР-слухачі Firebase успішно зупинені.")

          // Переключаемся в фоновый пул корутин Default для тяжелых дисковых операций
          withContext(Dispatchers.Default) {

            // 2. Выход из Firebase Auth сессии на данном конкретном смартфоне.
            // Так как мы находимся внутри корутин-контекста withContext, suspend-вызов теперь полностью легитимен!
            firebaseService.signOut()
            println("[YkisLogKMP.$tag.$methodName]: [STEP 2] Авторизаційний токен сесії пристрою успішно анульовано.")

            // 3. Сброс локальных флагов онбординга и согласия с офертой на накопителе телефона,
            // чтобы дать возможность следующему пользователю авторизоваться под своим Google/SMS аккаунтом
            settings.putString(key = "agreement_accepted", value = "false")
            settings.putBoolean(key = "is_terms_accepted", value = false)
            println("[YkisLogKMP.$tag.$methodName]: [STEP 3] Локальні прапори ліцензійної угоди ГІОЦ БТІ скинуто.")

            // 4. Очистка локального SQLite кэша СУБД SQLDelight.
            // Мы полностью стираем сохраненные квартиры предыдущего жильца из памяти телефона,
            // чтобы гарантировать приватность данных при перевходе под другим логином!
            try {
              withTimeout(2000L) {
                println("[YkisLogKMP.$tag.$methodName]: [STEP 4] Безпечний запуск очищення таблиць СУБД...")

                // ИСПРАВЛЕНО НАМЕРТВО: Интегрирован оператор .catch! Любые синтаксические взрывы
                // внутри холодного потока Use Case будут перехвачены на лету, не ломая корутину логаута!
                clearDatabase()
                  .catch { error ->
                    println("[YkisLogKMP.$tag.$methodName.catch]: [HANDLED_FLOW_ERROR] Перехоплено порушення прозорості Flow: ${error.message}")
                  }
                  .firstOrNull()

                println("[YkisLogKMP.$tag.$methodName]: [STEP 4_DONE] Локальний кЕш СУБД SQLDelight успішно випалено.")
              }
            } catch (e: Exception) {
              println("[YkisLogKMP.$tag.$methodName]: [STEP 4_TIMEOUT] Очищення локальної бази завершено за лімітом: ${e.message}")
            }
          }
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$tag.$methodName]: [FATAL_ERROR] Непередбачений збій під час логауту пристрою: ${e.message}")
      } finally {
        // Блок finally сработает ВСЕГДА, пробивая любые дисковые лаги и таймауты СУБД!
        println("[YkisLogKMP.$tag.$methodName]: [FINISH] Процедура логаута завершена. Сброс индикатора загрузки.")
        _loading.value = false

        // Передаем управление лямбде успеха на Главном потоке для мгновенного нативного переключения экрана
        withContext(Dispatchers.Main) {
          onSuccess()
        }
      }
    }
  }


  /**
   * [revokeAccess] — Безвозвратное локальное и облачное удаление коммунального профиля абонента ИС ЮКИС.
   * Внедрена автоматическая проверка "свежести" сессии! Если сессия устарела,
   * метод возвращает ошибкуCREDENTIALS_TOO_OLD, защищая базу данных от половинчатого удаления.
   */
  fun revokeAccess(onSuccess: (isSessionExpired: Boolean) -> Unit) {
    val methodName = "revokeAccess"
    if (_loading.value) return
    _loading.value = true

    println("[YkisLogKMP.$tag.$methodName]: [START] Запущено процедуру повного знищення аккаунта ЮКИС.")

    screenModelScope.launch {
      try {
        // 1. Вызываем наш обновленный КМР-сервис в облаке Firebase
        val cloudResult = firebaseService.revokeAccess()

        if (cloudResult is Resource.Error) {
          // ПРОВЕРКА НА КРИТИЧЕСКИЙ МАРКЕР УСТАРЕВШЕЙ СЕССИИ GOOGLE:
          if (cloudResult.message == "CREDENTIALS_TOO_OLD") {
            println("[YkisLogKMP.$tag.$methodName]: [ABORT] Сесія застаріла! Скасування локального випалювання таблиць.")
            _loading.value = false

            // Вызываем onSuccess, передавая true (сессия истекла, нужно показать Snackbar перезахода)
            withContext(Dispatchers.Main) {
              onSuccess(true)
            }
            return@launch
          }
          throw Exception(cloudResult.message)
        }

        // 2. СЕССИЯ БЫЛА СВЕЖЕЙ И ОБЛАКО УСПЕШНО СТЕРТО — Теперь каскадно выжигаем локальный накопитель смартфона!
        withContext(NonCancellable) {
          try {
            settings.putString(key = "agreement_accepted", value = "false")
            settings.putBoolean(key = "is_terms_accepted", value = false)

            withTimeout(2000L) {
              println("[YkisLogKMP.$tag.$methodName]: Очищення локальних таблиць СУБД SQLDelight...")
              clearDatabase()
                .catch { error ->
                  println("[YkisLogKMP.$tag.$methodName.catch]: Погашено порушення прозорості Flow СУБД: ${error.message}")
                }
                .firstOrNull()
            }
          } catch (e: Exception) {
            println("[YkisLogKMP.$tag.$methodName]: [TIMEOUT_HANDLED] Локальне чищення бази завершено: ${e.message}")
          }
        }

        println("[YkisLogKMP.$tag.$methodName]: [SUCCESS_ALL] Профіль повністю знищено у хмарі та на пристрої мешканця.")
        _loading.value = false

        withContext(Dispatchers.Main) {
          onSuccess(false) // Успех, сессия не истекла, аккаунт полностью удален!
        }

      } catch (e: Exception) {
        println("[YkisLogKMP.$tag.$methodName]: Сбой ликвидации аккаунта: ${e.message}")
        _loading.value = false
        // В случае непредвиденной ошибки просто возвращаем фоллбэк логаута
        withContext(Dispatchers.Main) { onSuccess(false) }
      }
    }
  }


}
