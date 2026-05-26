package com.ykis.ykismobkmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

// ПЕРЕЧИСЛЕНИЕ ДЛЯ КОНФИГУРАЦИЙ ТЕМЫ ЮКИС г. ЮЖНЫЙ
enum class ThemeValues {
  LIGHT_MODE,
  DARK_MODE,
  SYSTEM_DEFAULT
}

// Палитра Material 3 расчетного центра ЮКИС г. Южный
private val lightScheme = lightColorScheme(
  primary = Color(0xFF0061A4),
  background = Color(0xFFFEFBFF),
  surface = Color(0xFFFEFBFF)
)

private val darkScheme = darkColorScheme(
  primary = Color(0xFF9ECAFF),
  background = Color(0xFF1A1C1E),
  surface = Color(0xFF1A1C1E)
)

private const val tag = "YkisPAMTheme"

/**
 * [YkisPAMTheme] — Глобальний кросплатформовий конвеєр кольорових палітр Material 3.
 * ИСПРАВЛЕНО НАМЕРТВО: Строковый анализатор приведен к единомуlowercase КМР-стандарту биллинга ЮКІС ("light"/"dark"/"system").
 * Теперь регистры букв полностью совпадают с DataStore, и палитра переключается на лету мгновенно!
 */
@Composable
fun YkisPAMTheme(
  appTheme: String? = "light", // Дефолтное значение строчным текстом
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  // ИСПРАВЛЕНО НАМЕРТВО: Сверяем прилетающие строки с реальными ключами кэша DataStore/Settings!
  // Это полностью устраняет падение в ветку else и гарантирует мгновенную рекомпозицию палитры.
  val darkTheme = when (appTheme?.lowercase()) {
    "system" -> useDarkTheme
    "dark"   -> true
    "light"  -> false
    else     -> useDarkTheme // Безопасный фоллбэк на системные часы Android/iOS
  }

  val colors = if (darkTheme) darkScheme else lightScheme

  // Вызываем нашу безопасную ленивую КМР-типографику из Type.kt
  val kmpTypography = rememberYkisTypography()

  SideEffect {
    println("[$tag.SideEffect]: Зміна кольорової палітри ІС ЮКІС. Активний appTheme: \"$appTheme\" -> Темна схема: $darkTheme")
  }

  // Вызываем expect-компонент для прорисовки цвета баров под капотом каждой ОС
  PlatformSystemBarsAppearance(isDarkScrim = darkTheme)

  MaterialTheme(
    colorScheme = colors,
    content = content,
    typography = kmpTypography,
    shapes = shapes // Нативно подхватывает открытую переменную shapes из твоего Shapes.kt!
  )
}


/**
 * [PlatformSystemBarsAppearance] — Кроссплатформенный expect-компонент изменения инсетов статус-бара.
 */
@Composable
expect fun PlatformSystemBarsAppearance(isDarkScrim: Boolean)
