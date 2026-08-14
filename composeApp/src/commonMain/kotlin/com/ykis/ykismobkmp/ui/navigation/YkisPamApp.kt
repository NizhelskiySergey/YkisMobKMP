package com.ykis.ykismobkmp.ui.navigation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.SnackbarMessage
import com.ykis.ykismobkmp.core.utils.closeApplication
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Clock

private const val className = "YkisPamApp"

val LocalNavigationType = staticCompositionLocalOf { NavigationType.BOTTOM_NAVIGATION }
val LocalContentType = staticCompositionLocalOf { ContentType.SINGLE_PANE }

@OptIn(InternalVoyagerApi::class)
@Composable
fun YkisPamApp(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
) {
  val settingsModel = koinInject<SettingsScreenModel>()
  val appTheme by settingsModel.theme.collectAsState()

  YkisPAMTheme(appTheme = appTheme) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      val (navigationType, contentType) = rememberAdaptiveLayoutType(
        windowSize = windowSize,
        displayFeatures = displayFeatures
      )
      
      CompositionLocalProvider(
        LocalNavigationType provides navigationType,
        LocalContentType provides contentType
      ) {
        var lastBackPressTime by remember { mutableStateOf(0L) }
        val snackbarManager = koinInject<SnackbarManager>()
        BackHandler(enabled = true) {
          val currentTime = Clock.System.now().toEpochMilliseconds()
          if (currentTime - lastBackPressTime < 2000) {
            println("[YkisLogKMP.$className.BackHandler]: Повторне натискання зафіксовано. Вихід з системи.")
            closeApplication()
          } else {
            lastBackPressTime = currentTime
            println("[YkisLogKMP.$className.BackHandler]: Перше натискання кнопки Назад. Вивід сповіщення.")
            snackbarManager.showMessage("Натисніть ще раз для виходу з програми")
          }
        }
        LaunchedEffect(navigationType, contentType) {
          println("[YkisLogKMP.$className.YkisPamApp]: Конфігурація геометрії прийнята. Навігація=$navigationType, Контент=$contentType")
        }
        RootNavGraph(
          contentType = contentType,
          navigationType = navigationType
        )
      }
    }
  }
}
@Composable
fun rememberYkisPamAppState(
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  snackbarManager: SnackbarManager = koinInject(),
  coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(snackbarHostState, snackbarManager, coroutineScope) {
  YkisPamAppState(
    snackbarHostState = snackbarHostState,
    snackbarManager = snackbarManager,
    coroutineScope = coroutineScope
  )
}
@Stable
class YkisPamAppState(
  val snackbarHostState: SnackbarHostState,
  private val snackbarManager: SnackbarManager,
  val coroutineScope: CoroutineScope
) {
  private val logTag = "YkisPamAppState"
  init {
    coroutineScope.launch {
      println("[YkisLogKMP.$logTag.init]: Запуск кроссплатформенного слухача Snackbar повідомлень YkisMobKMP")
      snackbarManager.snackbarMessages
        .filterNotNull()
        .collect { snackbarMessage ->
          val text = try {
            when (snackbarMessage) {
              is SnackbarMessage.Resource -> {
                org.jetbrains.compose.resources.getString(snackbarMessage.resId)
              }
              is SnackbarMessage.Text -> {
                snackbarMessage.message
              }
            }
          } catch (e: Exception) {
            println("[YkisLogKMP.$logTag.init_WARN] Критическая ошибка извлечения строки Res: ${e.message}")
            "Помилка відображення сповіщення"
          }
          if (text.isNotBlank()) {
            snackbarHostState.showSnackbar(
              message = text,
              withDismissAction = true
            )
            snackbarManager.clearMessage()
            println("[YkisLogKMP.$logTag.init]: Повідомлення Snackbar успішно оброблено та видалено з черги")
          }
        }
    }
  }
}
enum class NavigationType {
  BOTTOM_NAVIGATION,
  NAVIGATION_RAIL_COMPACT,
  NAVIGATION_RAIL_EXPANDED,
  PERMANENT_NAVIGATION_DRAWER
}
enum class ContentType {
  SINGLE_PANE, DUAL_PANE
}
enum class ContentDetail {
  STANDARD_USER,
  BTI,
  FAMILY,
  UNKNOWN,
  OSBB,
  WATER_SERVICE,
  WARM_SERVICE,
  GARBAGE_SERVICE,
  WATER_METER,
  HEAT_METER,
  WATER_READINGS,
  HEAT_READINGS,
  PAYMENT_LIST,
  PAYMENT_CHOICE
}
