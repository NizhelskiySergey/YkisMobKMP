package com.ykis.ykismobkmp.data.preferences // Совпадает с интерфейсом
import com.russhwolf.settings.Settings

private const val className = "AppSettingsRepositoryImpl"

/**
 * [AppSettingsRepositoryImpl] — Нативная Apple-реализация репозитория настроек для iOS-устройств.
 * ИСПРАВЛЕНО НАМЕРТВО: Легаси-методы DataStore полностью стерты. Класс реализует утвержденные
 * КМР-методы getString и putString на базе синглтона Settings (NSUserDefaults)!
 */
class AppSettingsRepositoryImpl(
  private val settings: Settings // Инжектируем кроссплатформенный синглтон из Koin графа
) : AppSettingsRepository {

  /**
   * [getString] — Синхронное мгновенное чтение темы оформления или оферты ЮКИС из NSUserDefaults iOS.
   */
  override fun getString(key: String, defaultValue: String): String {
    return try {
      val savedValue = settings.getString(key, defaultValue)
      println("[$className.getString]: [iOS] Вычитан параметр из NSUserDefaults: $key -> $savedValue")
      savedValue
    } catch (e: Exception) {
      println("[$className.getString]: [iOS_ERROR] Сбой чтения ключа $key: ${e.message}")
      defaultValue
    }
  }

  /**
   * [putString] — Моментальная синхронная запись флагов соглашений ЖКХ в память Apple-устройства.
   */
  override fun putString(key: String, value: String) {
    try {
      println("[$className.putString]: [iOS] Атомарная запись в NSUserDefaults: $key -> $value")
      settings.putString(key = key, value = value)
    } catch (e: Exception) {
      println("[$className.putString]: [iOS_ERROR] Не удалось сохранить параметр $key в iOS: ${e.message}")
    }
  }
}
