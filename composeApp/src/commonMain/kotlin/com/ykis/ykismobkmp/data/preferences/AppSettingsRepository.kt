package com.ykis.ykismobkmp.data.preferences

private const val className = "AppSettingsRepository"

/**
 * [AppSettingsRepository] — Глобальный кроссплатформенный контракт репозитория настроек ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Легаси-методы observeTheme/saveTheme полностью удалены.
 * Интерфейс переведен на универсальный синхронный КМР-стандарт getString/putString.
 */
interface AppSettingsRepository {

  /**
   * [getString] — Синхронное чтение сохраненных строковых параметров оферты, сессий или тем оформления.
   */
  fun getString(key: String, defaultValue: String): String

  /**
   * [putString] — Моментальная синхронная запись текстовых конфигураций на диск устройства.
   */
  fun putString(key: String, value: String)
}
