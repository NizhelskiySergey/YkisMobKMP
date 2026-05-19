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
 * [YkisPAMTheme] — Центральная кроссплатформенная тема оформления Material 3.
 * ИСПРАВЛЕНО НАМЕРТВО: Конфликт дублирования геометрии Conflicting declarations: val shapes удален,
 * тема автоматически использует глобальный shapes твоего проекта.
 */
@Composable
fun YkisPAMTheme(
  appTheme: String? = ThemeValues.LIGHT_MODE.name,
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  // Определяем, какую схему использовать (Твоя оригинальная логика)
  val darkTheme = when (appTheme) {
    ThemeValues.SYSTEM_DEFAULT.name -> useDarkTheme
    ThemeValues.DARK_MODE.name -> true
    ThemeValues.LIGHT_MODE.name -> false
    else -> useDarkTheme
  }

  val colors = if (darkTheme) darkScheme else lightScheme

  // Вызываем нашу безопасную ленивую КМР-типографику из Type.kt
  val kmpTypography = rememberYkisTypography()

  SideEffect {
    println("[$tag.SideEffect]: Смена цветовой палитры. DarkTheme активна: $darkTheme")
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
