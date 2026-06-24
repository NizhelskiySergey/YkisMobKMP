package com.ykis.ykismobkmp.ui.screens.settings

import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.restartApp
import com.ykis.ykismobkmp.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val tag = "SettingsScreenModel"
private const val THEME_KEY = "theme_key"
private const val APP_LANGUAGE_KEY = "app_language"

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

  private val _language = MutableStateFlow(settings.getString(APP_LANGUAGE_KEY, "uk"))
  val language: StateFlow<String> = _language.asStateFlow()

  val displayName: String get() = firebaseService.displayName
  val photoUrl: String get() = firebaseService.photoUrl
  val email: String get() = firebaseService.email

  init {
    val currentLang = settings.getString(APP_LANGUAGE_KEY, "uk")
    println("[YkisLogKMP.$tag.init]: Ініціалізація налаштувань. Мова: $currentLang, Тема: ${settings.getString(THEME_KEY, "system")}")
    getThemeValue()
  }

  fun setLanguageValue(value: String) {
    val methodName = "setLanguageValue"
    try {
      println("[YkisLogKMP.$tag.$methodName]: Збереження мови: \"$value\"")
      settings.putString(key = APP_LANGUAGE_KEY, value = value)
      _language.value = value
      
      // КРИТИЧНО ДЛЯ WEB: Перезавантаження для оновлення ресурсів Compose
      if (getPlatform().name.contains("Web", true)) {
          println("[YkisLogKMP.$tag.$methodName]: Web-платформа виявлена. Запуск перезавантаження...")
          restartApp()
      }
    } catch (e: Exception) {
      println("[YkisLogKMP.$tag.$methodName]: [ERROR] Не вдалося зберегти мову: ${e.message}")
    }
  }

  fun setThemeValue(value: String) {
    val methodName = "setThemeValue"
    try {
      settings.putString(key = THEME_KEY, value = value)
      _theme.value = value
    } catch (e: Exception) {
      println("[YkisLogKMP.$tag.$methodName]: [ERROR] Не удалось сохранить тему: ${e.message}")
    }
  }

  fun getThemeValue() {
    val methodName = "getThemeValue"
    try {
      val savedTheme = settings.getString(key = THEME_KEY, defaultValue = "system")
      _theme.value = savedTheme
    } catch (e: Exception) {
      _theme.value = "system"
    }
  }

  fun signOut(onSuccess: () -> Unit) {
    if (_loading.value) return
    _loading.value = true
    screenModelScope.launch {
      try {
        withContext(NonCancellable) {
          firebaseService.stopAllListeners()
          withContext(Dispatchers.Default) {
            firebaseService.signOut()
            settings.putString(key = "agreement_accepted", value = "false")
            settings.putBoolean(key = "is_terms_accepted", value = false)
            try {
              withTimeout(2000L) {
                clearDatabase().catch { }.firstOrNull()
              }
            } catch (e: Exception) { }
          }
        }
      } catch (e: Exception) { } finally {
        _loading.value = false
        withContext(Dispatchers.Main) { onSuccess() }
      }
    }
  }

  fun revokeAccess(onSuccess: (isSessionExpired: Boolean) -> Unit) {
    if (_loading.value) return
    _loading.value = true
    screenModelScope.launch {
      try {
        val cloudResult = firebaseService.revokeAccess()
        if (cloudResult is Resource.Error && cloudResult.message == "CREDENTIALS_TOO_OLD") {
            _loading.value = false
            withContext(Dispatchers.Main) { onSuccess(true) }
            return@launch
        }
        withContext(NonCancellable) {
          settings.putString(key = "agreement_accepted", value = "false")
          settings.putBoolean(key = "is_terms_accepted", value = false)
          withTimeout(2000L) { clearDatabase().catch { }.firstOrNull() }
        }
        _loading.value = false
        withContext(Dispatchers.Main) { onSuccess(false) }
      } catch (e: Exception) {
        _loading.value = false
        withContext(Dispatchers.Main) { onSuccess(false) }
      }
    }
  }
}
