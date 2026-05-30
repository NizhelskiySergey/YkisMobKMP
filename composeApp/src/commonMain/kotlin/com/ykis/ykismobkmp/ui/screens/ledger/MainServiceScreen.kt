package com.ykis.ykismobkmp.ui.screens.ledger
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
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.list.ServiceListScreen
import org.koin.compose.koinInject
private const val tag = "MainServiceScreen"

/**
 * [MainServiceScreen] — Главный КМР-экран финансового хаба начислений и оплат ЮКИС г. Южный.
 * ИСПРАВЛЕНО НАМЕРТВО: Сигнатура конструктора приведена к сквозному стандарту адаптивного Хаба!
 */
class MainServiceScreen(
  private val baseUIState: BaseUIState, // ДОБАВЛЕНО: Принимаем снимок состояния БТИ
  private val navigationType: NavigationType, // ДОБАВЛЕНО: Принимаем тип навигации для DefaultAppBar
  private val onDrawerClick: () -> Unit = {},
  private val navigateToWebView: (String) -> Unit = {}
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val adaptiveContentType = LocalContentType.current

    // 1. Инжектируем финансовую модель для обработки инвойсов и балансов ГИОЦ
    val ledgerScreenModel = koinInject<LedgerScreenModel>()

    // Извлекаем BaseUIState напрямую из его легитимного КМР-источника — ApartmentScreenModel
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()

    // ИСПРАВЛЕНО НАМЕРТВО: Поле приведено к правильному имени apartmentUiState,
    // полностью уничтожая ошибку Unresolved reference!
    val currentLiveState by apartmentScreenModel.apartmentUiState.collectAsState()

    // 2. Подписываемся на финансовое состояние задолженностей и тарифов ЮКИС
    val totalDebtState by ledgerScreenModel.totalDebtState.collectAsState()
    val contentDetail: ContentDetail = totalDebtState.serviceDetail

    if (adaptiveContentType == ContentType.DUAL_PANE) {
      // Трассировка рантайма по правилу [Класс.Метод] через КМР-команду println()
      println("[$tag.Tablet]: [RECOMPOSE] CurrentDetail: $contentDetail | ShowDetail: ${totalDebtState.showDetail}")

      // Широкоформатная двухпанельная верстка для Mac Desktop (Хаб слева, детализация справа)
      Row(modifier = Modifier.fillMaxSize()) {
        // Левая панель: Сводный баланс и список служб биллинга (45% ширины дисплея)
        Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
          ServiceListScreen(
            baseUIState = currentLiveState, // Передаем живой стейт БТИ квартиры
            onDrawerClick = onDrawerClick,
            totalDebtState = totalDebtState,
            getTotalServiceDebt = { params ->
              println("[$tag.Tablet]: [GET_DEBT] Trigger запроса баланса ГИОЦ для о/р Long: ${params.addressId}")
              ledgerScreenModel.getTotalServiceDebt(params.uid, params.addressId, params.year, params.service, params.total)
            },
            setContentDetail = { content ->
              println("[$tag.Tablet]: [CLICK_EVENT] Переключение финансовой вкладки: $content")
              ledgerScreenModel.setContentDetail(content)
            }
          )
        }

        VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Правая панель: Инвойсы Xpay или детальные ведомости ГИОЦ (55% ширины)
        Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
          if (contentDetail != ContentDetail.UNKNOWN) {
            ServiceDetailScreen(
              modifier = Modifier.background(Color.Transparent),
              contentDetail = contentDetail,
              baseUIState = currentLiveState,
              totalDebtState = totalDebtState,
              navigateToWebView = navigateToWebView
            )
          } else {
            // КМР-заглушка правого холста Material 3, если ни одна ЖКХ-служба еще не выбрана мышкой на Mac
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
      // Мобильный режим смартфона (Одноэкранное каскадное переключение через Crossfade)
      SinglePanelService(
        contentDetail = contentDetail,
        baseUIState = currentLiveState,
        onDrawerClick = onDrawerClick,
        totalDebtState = totalDebtState,
        screenModel = ledgerScreenModel,
        navigateToWebView = navigateToWebView
      )
    }
  }
}

/**
 * [SinglePanelService] — Stateless-верстка финансового хаба для мобильных дисплеев жителей г. Южный.
 */
@OptIn(InternalVoyagerApi::class)
@Composable
fun SinglePanelService(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  onDrawerClick: () -> Unit,
  totalDebtState: TotalDebtState,
  screenModel: LedgerScreenModel,
  navigateToWebView: (String) -> Unit
) {
  Crossfade(
    targetState = totalDebtState.showDetail,
    label = "SinglePanelServiceCrossfade"
  ) { showDetail ->
    if (showDetail) {
      // ИСПРАВЛЕНО: Перехватчик аппаратной кнопки "Назад" КМР-движка Voyager (без лишних let-вложений)
      BackHandler(enabled = true) {
        println("[$tag.Mobile.BackHandler]: Системне перехоплення кнопки Назад. Закриття детального інвойсу.")
        screenModel.closeContentDetail()
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
        getTotalServiceDebt = { params -> screenModel.getTotalServiceDebt(params.uid, params.addressId, params.year, params.service, params.total) },
        setContentDetail = { content -> screenModel.setContentDetail(content) }
      )
    }
  }
}





