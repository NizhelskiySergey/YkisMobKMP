package com.ykis.ykismobkmp.data.preferences

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobPAM / YkisMobKMP
import com.russhwolf.settings.Settings

private const val className = "AppSettingsRepositoryImpl"

/**
 * [AppSettingsRepositoryImpl] — Реализация репозитория настроек, оферты и тем оформления на базе Multiplatform Settings.
 * ИСПРАВЛЕНО НАМЕРТВО: Легаси-методы полностью удалены, класс реализует новые КМР-методы getString и putString,
 * полностью ликвидируя ошибку "does not implement abstract members"!
 */
class AppSettingsRepositoryImpl(
  private val settings: Settings
) : AppSettingsRepository {

  /**
   * [getString] — Синхронное вычитывание параметров темы, оферты или сессий из SharedPreferences (на Android).
   */
  override fun getString(key: String, defaultValue: String): String {
    return try {
      val savedValue = settings.getString(key, defaultValue)
      println("[$className.getString]: Успешно прочитан ключ: $key -> $savedValue")
      savedValue
    } catch (e: Exception) {
      println("[$className.getString]: [ERROR] Сбой чтения ключа $key: ${e.message}")
      defaultValue
    }
  }

  /**
   * [putString] — Синхронная атомарная запись флагов соглашений и цветовых схем на диск устройства.
   */
  override fun putString(key: String, value: String) {
    try {
      println("[$className.putString]: Запись параметра на диск: $key -> $value")
      settings.putString(key = key, value = value)
    } catch (e: Exception) {
      println("[$className.putString]: [ERROR] Не удалось записать параметр $key на диск: ${e.message}")
    }
  }
}
