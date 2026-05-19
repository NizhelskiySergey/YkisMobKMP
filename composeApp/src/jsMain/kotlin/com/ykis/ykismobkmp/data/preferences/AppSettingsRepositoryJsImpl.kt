package com.ykis.ykismobkmp.data.preferences

import kotlinx.browser.localStorage // Нативный браузерный API-доступ к LocalStorage в Kotlin/JS

// ИМПОРТ НАШЕГО УТВЕРЖДЕННОГО КМР-КОНТРАКТА ИНТЕРФЕЙСА
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository

private const val className = "AppSettingsRepositoryJsImpl"

/**
 * [AppSettingsRepositoryJsImpl] — Нативная JavaScript-реализация репозитория настроек для браузерной Web-версии.
 * ИСПРАВЛЕНО НАМЕРТВО: Сигнатуры методов полностью выровнены с общим КМР-интерфейсом AppSettingsRepository,
 * устранен конфликт compilation error несовпадения абстрактных членов класса!
 */
class AppSettingsRepositoryJsImpl : AppSettingsRepository {

  /**
   * [getString] — Синхронное чтение сохраненных конфигураций тем или оферты из LocalStorage браузера.
   */
  override fun getString(key: String, defaultValue: String): String {
    try {
      val savedValue = localStorage.getItem(key)
      println("[$className.getString]: Вычитан ключ Web JS LocalStorage: $key -> $savedValue")
      return savedValue ?: defaultValue
    } catch (e: Exception) {
      println("[$className.getString]: [ERROR] Ошибка чтения из браузерного LocalStorage: ${e.message}")
      return defaultValue
    }
  }

  /**
   * [putString] — Моментальная синхронная запись параметров оферты и тем оформления в куки LocalStorage.
   */
  override fun putString(key: String, value: String) {
    try {
      println("[$className.putString]: Запись в Web JS LocalStorage: $key -> $value")
      localStorage.setItem(key, value)
    } catch (e: Exception) {
      println("[$className.putString]: [ERROR] Не удалось записать параметр в LocalStorage браузера: ${e.message}")
    }
  }
}
