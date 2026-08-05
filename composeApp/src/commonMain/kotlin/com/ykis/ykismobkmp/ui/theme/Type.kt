package com.ykis.ykismobkmp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import com.ykis.ykismobkmp.*

@Composable
fun rememberYkisTypography(): Typography {
  val regularFont = Font(resource = Res.font.robotocondensed_regular)
  val lightFont = Font(resource = Res.font.robotocondensed_light, weight = FontWeight.Light)
  val boldFont = Font(resource = Res.font.robotocondensed_bold, weight = FontWeight.Bold)

  val robotoFontFamily = FontFamily(regularFont, lightFont, boldFont)

  return remember(robotoFontFamily) {
    Typography(
      headlineLarge = TextStyle(fontFamily = robotoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
      headlineMedium = TextStyle(fontFamily = robotoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
      bodyLarge = TextStyle(fontFamily = robotoFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
      labelMedium = TextStyle(fontFamily = robotoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    )
  }
}
