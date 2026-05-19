package com.ykis.ykismobkmp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Нативный импорт КМР-функции Font для бесшовной работы со сгенерированными ресурсами JetBrains
import org.jetbrains.compose.resources.Font

// Импорт сгенерированных КМР ресурсов шрифтов Roboto Condensed
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.robotocondensed_regular
import ykismobkmp.composeapp.generated.resources.robotocondensed_light
import ykismobkmp.composeapp.generated.resources.robotocondensed_bold

private const val className = "Type"

/**
 * [rememberYkisTypography] — Кроссплатформенная фабрика ленивой сборки текстовой матрицы Material 3.
 * ИСПРАВЛЕНО НАМЕРТВО: Вызовы Composable-функции Font(...) вынесены напрямую в тело метода,
 * полностью ликвидируя ошибку контекста вызова Composable invocations can only happen...
 */
@Composable
fun rememberYkisTypography(): Typography {

  // РЕШЕНИЕ: Извлекаем Composable-шрифты напрямую в контексте функции, минуя ограничения лямбд!
  val regularFont = Font(resource = Res.font.robotocondensed_regular)
  val lightFont = Font(resource = Res.font.robotocondensed_light, weight = FontWeight.Light)
  val boldFont = Font(resource = Res.font.robotocondensed_bold, weight = FontWeight.Bold)

  // Собираем шрифты в единое КМР семейство FontFamily
  val robotoFontFamily = remember(regularFont, lightFont, boldFont) {
    FontFamily(regularFont, lightFont, boldFont)
  }

  return remember(robotoFontFamily) {
    Typography(
      headlineLarge = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
      ),
      headlineMedium = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
      ),
      headlineSmall = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
      ),
      titleLarge = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
      ),
      titleMedium = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
      ),
      titleSmall = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
      ),
      bodyLarge = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
      ),
      bodyMedium = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
      ),
      bodySmall = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
      ),
      labelLarge = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
      ),
      labelMedium = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
      ),
      labelSmall = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
      )
    )
  }
}
