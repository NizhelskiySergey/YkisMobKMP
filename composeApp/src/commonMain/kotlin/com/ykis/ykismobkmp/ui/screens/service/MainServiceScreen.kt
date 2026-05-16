package com.ykis.ykismobkmp.ui.screens.service

// Импорты общих компонентов, стейтов и моделей ЮКИС
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.screens.service.list.TotalDebtState
import org.koin.compose.koinInject

private const val tag = "MainServiceScreen"

/**
 * [MainServiceScreen] — Главный КМР-экран финансового хаба начислений и оплат ЮКИС г. Южный.
 * Обернут в контейнер Voyager Screen и адаптирован для Mac Desktop и мобильных ОС.
 */
class MainServiceScreen(
  private val contentType: ContentType,
  private val onDrawerClick: () -> Unit,
  private val navigateToWebView: (String) -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Инжектируем очищенную КМР финансовую модель экрана через Koin
    val screenModel = koinInject<ServiceScreenModel>()

    // Подписываемся на сквозное UI состояние квартир БТИ
    val baseUIState by screenModel.baseUIState.collectAsState()

    // ИСПРАВЛЕНО: collectAsStateWithLifecycle заменен универсальным КМР collectAsState()
    val totalDebtState by screenModel.totalDebtState.collectAsState()
    val contentDetail: ContentDetail = totalDebtState.serviceDetail

    if (contentType == ContentType.DUAL_PANE) {
      // ИСПРАВЛЕНО: Платформозависимый Log.d заменен универсальной функцией println()
      println("[$tag.Tablet]: [RECOMPOSE] CurrentDetail: $contentDetail | ShowDetail: ${totalDebtState.showDetail}")

      // Широкоформатная двухпанельная верстка для Mac Desktop (Хаб слева, детализация справа)
      Row(modifier = Modifier.fillMaxSize()) {
        // Левая панель: Сводный баланс и список служб биллинга (45% ширины)
        Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
          ServiceListScreen(
            baseUIState = baseUIState,
            onDrawerClick = onDrawerClick,
            totalDebtState = totalDebtState,
            getTotalServiceDebt = { params ->
              println("[$tag.Tablet]: [GET_DEBT] Триггер запроса баланса для о/р ${params.addressId}")
              screenModel.getTotalServiceDebt(params)
            },
            setContentDetail = { content ->
              println("[$tag.Tablet]: [CLICK_EVENT] Выбрана служба: $content")
              screenModel.setContentDetail(content)
            }
          )
        }

        VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Правая панель: Инвойсы Xpay или детальные тарифы ГИОЦ (55% ширины)
        Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
          if (contentDetail != ContentDetail.UNKNOWN) {
            ServiceDetailScreen(
              modifier = Modifier.background(Color.Transparent),
              contentDetail = contentDetail,
              baseUIState = baseUIState,
              totalDebtState = totalDebtState,
              navigateToWebView = navigateToWebView
            )
          } else {
            // КМР-заглушка правого холста Material 3, если ни одна ЖКХ-служба еще не выбрана мышкой
            Column(
              modifier = Modifier.fillMaxSize().padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Оберіть комунальну послугу для перегляду деталей",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            }
          }
        }
      }
    } else {
      // Мобильный режим смартфона (Одноэкранное каскадное переключение)
      SinglePanelService(
        contentDetail = contentDetail,
        baseUIState = baseUIState,
        onDrawerClick = onDrawerClick,
        totalDebtState = totalDebtState,
        screenModel = screenModel,
        navigateToWebView = navigateToWebView
      )
    }
  }
}

/**
 * [SinglePanelService] — Stateless-верстка финансового хаба для мобильных дисплеев.
 */
@Composable
fun SinglePanelService(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  onDrawerClick: () -> Unit,
  totalDebtState: TotalDebtState,
  screenModel: ServiceScreenModel,
  navigateToWebView: (String) -> Unit
) {
  Crossfade(
    targetState = totalDebtState.showDetail,
    label = "SinglePanelServiceCrossfade"
  ) { showDetail ->
    if (showDetail) {
      // ИСПРАВЛЕНО: Нативный Android BackHandler заменен на КМР BackHandler библиотеки Voyager
      cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow.let {
        cafe.adriel.voyager.navigator.BackHandler(enabled = true) {
          println("[$tag.Mobile]: Нажата кнопка Назад, закрытие инвойса")
          screenModel.closeContentDetail()
        }
      }

      ServiceDetailScreen(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentDetail = contentDetail,
        baseUIState = baseUIState,
        totalDebtState = totalDebtState,
        navigateToWebView = navigateToWebView
      )
    } else {
      ServiceListScreen(
        baseUIState = baseUIState,
        onDrawerClick = onDrawerClick,
        totalDebtState = totalDebtState,
        getTotalServiceDebt = { params -> screenModel.getTotalServiceDebt(params) },
        setContentDetail = { content -> screenModel.setContentDetail(content) }
      )
    }
  }
}

// Заглушка экрана списков начислений для успешной сборки (Пришли её оригинальный файл следующим шагом)
@Composable
fun ServiceListScreen(
  baseUIState: BaseUIState,
  onDrawerClick: () -> Unit,
  totalDebtState: TotalDebtState,
  getTotalServiceDebt: (ServiceParams) -> Unit,
  setContentDetail: (ContentDetail) -> Unit
) {
  Box(Modifier.fillMaxSize()) { Text("Сводний баланс ГІОЦ") }
}

