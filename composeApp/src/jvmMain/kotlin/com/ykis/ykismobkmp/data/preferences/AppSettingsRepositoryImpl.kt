package com.ykis.ykismobkmp.data.preferences // Совпадает с интерфейсом
import com.russhwolf.settings.Settings

private const val className = "AppSettingsRepositoryImpl"

/**
 * [AppSettingsRepositoryImpl] — Нативная JVM-реализация репозитория настроек для платформы Mac Desktop.
 * ИСПРАВЛЕНО НАМЕРТВО: Легаси-методы DataStore полностью стерты. Класс реализует утвержденные
 * КМР-методы getString и putString на базе синглтона Settings, устраняя ошибку компиляции членов интерфейса!
 */
class AppSettingsRepositoryImpl(
  private val settings: Settings // Инжектируем кроссплатформенный синглтон из Koin графа
) : AppSettingsRepository {

  /**
   * [getString] — Синхронное мгновенное чтение темы оформления или флага оферты ЮКИС из кэша Mac OS.
   */
  override fun getString(key: String, defaultValue: String): String {
    return try {
      val savedValue = settings.getString(key, defaultValue)
      println("[$className.getString]: [Mac Desktop] Вычитан параметр конфигурации: $key -> $savedValue")
      savedValue
    } catch (e: Exception) {
      println("[$className.getString]: [JVM_ERROR] Сбой чтения ключа $key на Mac Desktop: ${e.message}")
      defaultValue
    }
  }

  /**
   * [putString] — Моментальная синхронная запись параметров соглашений и цветовых тем в файловую систему Mac/PC.
   */
  override fun putString(key: String, value: String) {
    try {
      println("[$className.putString]: [Mac Desktop] Атомарная запись настроек на диск: $key -> $value")
      settings.putString(key = key, value = value)
    } catch (e: Exception) {
      println("[$className.putString]: [JVM_ERROR] Не удалось сохранить параметр $key в системе JVM: ${e.message}")
    }
  }
}

