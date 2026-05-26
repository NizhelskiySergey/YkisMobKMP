package com.ykis.ykismobkmp.ui.screens.settings

import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val tag = "SettingsScreenModel"
private const val THEME_KEY = "theme_key"
class SettingsScreenModel(
  private val settings: Settings,
  private val clearDatabase: () -> Flow<Resource<Unit>>,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _theme = MutableStateFlow("system")
  val theme: StateFlow<String> = _theme.asStateFlow()

  val displayName: String get() = "Абонент ЮКІС (Офлайн)"
  val photoUrl: String get() = ""
  val email: String get() = "yuzhne.user@ykis.com"

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
  fun signOut(onSuccess: () -> Unit) {
    val methodName = "signOut"
    if (_loading.value) return
    _loading.value = true

    println("[YkisLogKMP.$tag.$methodName]: [START_OFFLINE] Запуск локального логаута абонента г. Южного...")

    screenModelScope.launch(NonCancellable) {
      try {
        try {
          settings.putString(key = "agreement_accepted", value = "false")
          println("[YkisLogKMP.$tag.$methodName]: Прапор угоди успішно скинуто в false.")
        } catch (e: Exception) {
          println("[YkisLogKMP.$tag.$methodName]: [ERR] Сбой сброса флага согласия в Settings: ${e.message}")
        }
        try {
          withTimeout(2000L) {
            println("[YkisLogKMP.$tag.$methodName]: Запрос к Use Case ClearDatabase на очистку таблиц SQLite...")
            clearDatabase()
              .take(1)
              .collect { result ->
                if (result is Resource.Success) {
                  println("[YkisLogKMP.$tag.$methodName]: [SUCCESS_DB] Локальні таблиці СУБД очищено успішно.")
                }
              }
          }
        } catch (e: Exception) {
          println("[YkisLogKMP.$tag.$methodName]: [TIMEOUT_OR_ERR] Очищення СУБД завершено або перевищило ліміт: ${e.message}")
        }

      } catch (e: Exception) {
        println("[YkisLogKMP.$tag.$methodName]: [FATAL] Сбой закрытия сессии: ${e.message}")
      } finally {
        println("[YkisLogKMP.$tag.$methodName]: Процедура логаута завершена. Сброс лоадера и вызов onSuccess.")
        _loading.value = false
        onSuccess()
      }
    }
  }

  /**
   * [revokeAccess] — Безвозвратное удаление коммунального профиля абонента ИС ЮКИС.
   */
  fun revokeAccess(onSuccess: () -> Unit) {
    val methodName = "revokeAccess"
    if (_loading.value) return
    _loading.value = true

    println("[YkisLogKMP.$tag.$methodName]: [START_OFFLINE] Запущено локальное удаление профиля ЮКИС.")

    screenModelScope.launch(NonCancellable) {
      try {
        try {
          settings.putString(key = "agreement_accepted", value = "false")

          withTimeout(2500L) {
            // ИСПРАВЛЕНО НАМЕРТВО: Добавлен оператор .take(1) для безопасного разрыва зависшего Flow!
            clearDatabase()
              .take(1)
              .collect { dbResult ->
                if (dbResult is Resource.Success) {
                  println("[YkisLogKMP.$tag.$methodName]: [SUCCESS_DB] Локальна база даних очищена.")
                }
              }
          }
        } catch (e: Exception) {
          println("[YkisLogKMP.$tag.$methodName]: [ERR] Локальное стирание базы пропущено: ${e.message}")
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$tag.$methodName]: [FATAL] Краш удаления профиля: ${e.message}")
      } finally {
        println("[YkisLogKMP.$tag.$methodName]: Процедура деструкции аккаунта завершена. Сброс лоадера.")
        _loading.value = false
        onSuccess()
      }
    }
  }
}
