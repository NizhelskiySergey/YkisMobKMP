package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel
import com.ykis.ykismobkmp.domain.services.UserRole
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.terms_condition
import ykismobkmp.composeapp.generated.resources.terms_condition_accept
import ykismobkmp.composeapp.generated.resources.terms_condition_down

private const val className = "TermsAndConditionScreen"

/**
 * [TermsAndConditionScreen] — Стартовий екран ліцензійної угоди (оферти ГІОЦ) біллінгу м. Южне.
 */
class TermsAndConditionScreen(private val termsText: String) : Screen {

  override val key: cafe.adriel.voyager.core.screen.ScreenKey = "TermsAndConditionScreen_Static"

  @Composable
  override fun Content() {
    val appStartModel = koinInject<AppScreenModel>()
    var isLoading by remember { mutableStateOf(false) }

    TermsAndConditionContent(
      termsText = termsText,
      isLoading = isLoading,
      onAccept = {
        isLoading = true
        println("[YkisLogKMP.$className.Content.onAccept]: Користувач підтвердив згоду. Фіксація в КМР-кЕш...")

        appStartModel.acceptTermsAndConditions {
          println("[YkisLogKMP.$className.Content.onAccept]: Лямбда успіху. Передано на реактивний розподіл.")
          // Мы не сбрасываем isLoading в false, так как экран должен смениться
        }
      }
    )
  }
}

/**
 * [TermsAndConditionContent] — Декларативна чиста верстка екрана ліцензії Material 3.
 */
@Composable
fun TermsAndConditionContent(
  termsText: String,
  isLoading: Boolean,
  onAccept: () -> Unit
) {
  val scrollState = rememberScrollState()

  val isScrollFinished = remember(scrollState.value, scrollState.maxValue) {
    scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = stringResource(Res.string.terms_condition),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    Box(modifier = Modifier.weight(1f)) {
      Text(
        text = parseMarkdown(termsText, MaterialTheme.colorScheme.primary),
        modifier = Modifier.verticalScroll(scrollState),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      onClick = {
        if (!isLoading) {
            println("[YkisLogKMP.$className.TermsAndConditionContent]: [EVENT] Клік по кнопці фіксації оферти")
            onAccept()
        }
      },
      enabled = isScrollFinished && !isLoading,
      shape = RoundedCornerShape(12.dp)
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = Color.White,
          strokeWidth = 2.dp
        )
      } else {
        Text(
          text = if (isScrollFinished) stringResource(Res.string.terms_condition_accept) else stringResource(Res.string.terms_condition_down),
          style = MaterialTheme.typography.titleMedium
        )
      }
    }
  }
}

/**
 * Дублюємо парсер Markdown для екрана угоди
 */
private fun parseMarkdown(text: String, accentColor: Color): AnnotatedString {
    val cleanText = text
        .replace("\\n", "\n")
        .replace(" #", "\n#")
        .replace(" *", "\n*")
        .replace("\r", "")
        // Якщо прийшов сирий JSON масив як строка, спробуємо його очистити від зайвого
        .replace("[", "").replace("]", "").replace("\"", "")

    return buildAnnotatedString {
        val lines = cleanText.split("\n")
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                append("\n")
                return@forEachIndexed
            }
            when {
                line.startsWith("#") -> {
                    val level = line.takeWhile { it == '#' }.length
                    val headerContent = line.replace("#", "").trim()
                    val fontSize = if (level == 1) 22.sp else 18.sp
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = accentColor)) {
                        appendInlineFormatted(headerContent)
                    }
                    append("\n")
                }
                line.startsWith("*") -> {
                    append("  • ")
                    appendInlineFormatted(line.removePrefix("*").trim())
                }
                else -> {
                    appendInlineFormatted(rawLine)
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(text: String) {
    val parts = text.split("**")
    parts.forEachIndexed { pIndex, part ->
        if (pIndex % 2 != 0) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(part)
            }
        } else {
            append(part)
        }
    }
}


