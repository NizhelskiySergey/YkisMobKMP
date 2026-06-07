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
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.list.ServiceListScreen
import org.koin.compose.koinInject
private const val tag = "MainServiceScreen"


class MainServiceScreen(
  private val baseUIState: BaseUIState, // Принимаем снимок состояния БТИ
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

    // Вычитываем живой стейт БТИ квартиры Южного
    val currentLiveState by apartmentScreenModel.uiState.collectAsState()

    // 2. Подписываемся на единый финансовый стейт задолженностей и тарифов ЮКИС
    val ledgerUIState by ledgerScreenModel.uiState.collectAsState()
    val contentDetail: ContentDetail = ledgerUIState.serviceDetail

    // Строго разделяем геометрию экранов на уровне корня верстки Хаба!
    if (adaptiveContentType == ContentType.DUAL_PANE) {
      println("[$tag.Tablet]: [RECOMPOSE] Двопанельний режим. Служба: $contentDetail")

      // Широкоформатная двухпанельная верстка для Mac Desktop / Планшетов
      Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
          ServiceListScreen(
            baseUIState = currentLiveState,
            onDrawerClick = onDrawerClick,
            ledgerUIState = ledgerUIState,
            getTotalServiceDebt = { params ->
              ledgerScreenModel.getTotalServiceDebt(params.uid, params.addressId, params.houseId, params.year, params.service, params.total)
            },
            setContentDetail = { content ->
              ledgerScreenModel.setContentDetail(content)
            }
          )
        }

        VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
          if (contentDetail != ContentDetail.UNKNOWN) {
            ServiceDetailScreen(
              modifier = Modifier.background(Color.Transparent),
              contentDetail = contentDetail,
              baseUIState = currentLiveState,
              ledgerUIState = ledgerUIState,
              screenModel = ledgerScreenModel,
              navigateToWebView = navigateToWebView
            )
          } else {
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
      SinglePanelService(
        modifier = Modifier.fillMaxSize(),
        contentDetail = contentDetail,
        baseUIState = currentLiveState,
        onDrawerClick = onDrawerClick,
        ledgerUIState = ledgerUIState,
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
  ledgerUIState: BaseUIState,
  screenModel: LedgerScreenModel,
  navigateToWebView: (String) -> Unit
) {
  // ИСПРАВЛЕНО НАМЕРТВО: Вычитываем живой, реактивный стейт прямо из сквозной модели!
  // Полностью уничтожен клин Snapshot-копий в ОЗУ смартфона!
  val liveDebtState by screenModel.uiState.collectAsState()

  Crossfade(
    targetState = liveDebtState.showDetail, // Отслеживаем живой Stateless-флаг
    label = "SinglePanelServiceCrossfade"
  ) { isDetailVisible ->
    if (isDetailVisible) {
      // Перехватчик аппаратной кнопки "Назад" смартфона
      BackHandler(enabled = true) {
        println("[$tag.Mobile.BackHandler]: Системне перехоплення кнопки Назад ЖКГ.")
        screenModel.closeContentDetail()
      }


      ServiceDetailScreen(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
        contentDetail = liveDebtState.serviceDetail,
        baseUIState = baseUIState,
        ledgerUIState = liveDebtState,
        screenModel = screenModel,
        navigateToWebView = navigateToWebView
      )
    } else {
      ServiceListScreen(
        baseUIState = baseUIState,
        onDrawerClick = onDrawerClick,
        ledgerUIState = liveDebtState,
        getTotalServiceDebt = { params ->
          screenModel.getTotalServiceDebt(params.uid, params.addressId, params.houseId, params.year, params.service, params.total)
        },
        setContentDetail = { content ->
          screenModel.setContentDetail(content)
        }
      )
    }
  }
}







