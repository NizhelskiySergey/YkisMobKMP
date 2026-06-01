package com.ykis.ykismobkmp.ui.screens.chat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.ledger.TotalDebtState
import com.ykis.ykismobkmp.ui.screens.ledger.list.TotalServiceDebt
import com.ykis.ykismobkmp.ui.screens.ledger.list.assembleServiceList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.services

private const val tag = "ServiceSelectorScreen"

class ServiceSelectorScreen(
  private val baseUIState: BaseUIState,
  private val onServiceClick: (TotalServiceDebt) -> Unit,
  private val onDrawerClicked: () -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val adaptiveNavigationType = LocalContentType.current

    val chatScreenModel = koinInject<ChatScreenModel>()

    ServiceSelectorContent(
      baseUIState = baseUIState,
      chatScreenModel = chatScreenModel,
      onServiceClick = onServiceClick,
      onDrawerClicked = onDrawerClicked,
      navigationType = NavigationType.BOTTOM_NAVIGATION
    )
  }
}

/**
 * [ServiceSelectorContent] — Декларативная Stateless-верстка выбора предприятий ГИОЦ г. Южного.
 */
@Composable
fun ServiceSelectorContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  chatScreenModel: ChatScreenModel,
  onServiceClick: (TotalServiceDebt) -> Unit,
  onDrawerClicked: () -> Unit,
  navigationType: NavigationType
) {
  val methodName = "ServiceSelectorContent"

  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val isForwardingMode by chatScreenModel.isForwardingMode.collectAsState()

  // Локальный чистый КМР-список коммунальных служб города Южного для чат-контура
  val residentServices = remember(baseUIState.osbb) {
    val list = mutableListOf<TotalServiceDebt>()
    if (baseUIState.osmdId != 0L || baseUIState.osbbId != 0L) {
      list.add(
        TotalServiceDebt(
          name = baseUIState.osbb.takeIf { it.isNotEmpty() } ?: "Мій ОСББ",
          color = Color.Unspecified,
          debt = 0.0,
          icon = Icons.Default.CorporateFare,
          contentDetail = ContentDetail.OSBB
        )
      )
    }
    list.addAll(
      listOf(
        TotalServiceDebt(
          name = "Водоканал",
          color = Color(0xFF2196F3),
          debt = 0.0,
          icon = Icons.Default.WaterDrop,
          contentDetail = ContentDetail.WATER_SERVICE
        ),
        TotalServiceDebt(
          name = "ЮТКЕ",
          color = Color(0xFFFF5722),
          debt = 0.0,
          icon = Icons.Default.HotTub,
          contentDetail = ContentDetail.WARM_SERVICE
        ),
        TotalServiceDebt(
          name = "Южтранс",
          color = Color(0xFF4CAF50),
          debt = 0.0,
          icon = Icons.Default.Commute,
          contentDetail = ContentDetail.GARBAGE_SERVICE
        )
      )
    )
    list
  }

  Column(modifier = modifier.fillMaxSize()) {
    DefaultAppBar(
      title = stringResource(Res.string.services),
      subtitle = "Оберіть службу чату",
      onDrawerClick = onDrawerClicked,
      canNavigateBack = false,
      navigationType = navigationType
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        modifier = Modifier
          .width(IntrinsicSize.Max)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        residentServices.forEach { service ->
          // СЧИТАЕМ СУММАРНЫЙ БЕЙДЖ НЕПРОЧИТАННЫХ (По всем КМР Long квартирам жильца для этой службы)
          val totalCount = remember(unreadCounts, baseUIState.apartments, service.contentDetail) {
            baseUIState.apartments.sumOf { apt ->
              // ИСПРАВЛЕНО: Ссылка на несуществующий osbbId удалена, оставлен легитимный osmdId
              val chatId = when (service.contentDetail) {
                ContentDetail.OSBB -> "OSBB_${apt.osmdId}_${apt.addressId}_${baseUIState.uid}"
                ContentDetail.WATER_SERVICE -> "WATER_SERVICE_9999_${apt.addressId}_${baseUIState.uid}"
                ContentDetail.WARM_SERVICE  -> "WARM_SERVICE_9998_${apt.addressId}_${baseUIState.uid}"
                ContentDetail.GARBAGE_SERVICE -> "GARBAGE_SERVICE_9997_${apt.addressId}_${baseUIState.uid}"
                else -> "${service.contentDetail.name}_${apt.addressId}_${baseUIState.uid}"
              }
              unreadCounts[chatId] ?: 0
            }
          }

          Box(modifier = Modifier.fillMaxWidth()) {
            Button(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
              onClick = {
                if (isForwardingMode) {
                  println("[$tag.$methodName]: [FORWARD_TO_SERVICE] Пересилання повідомлення до служби: ${service.contentDetail}")
                  chatScreenModel.confirmForwardToService(service.contentDetail, baseUIState)
                } else {
                  println("[$tag.$methodName]: [SELECT_SERVICE] Обрано лінію чату: ${service.name}")
                  chatScreenModel.setSelectedService(service)
                  onServiceClick(service)
                }
              }
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = service.icon,
                  contentDescription = null,
                  modifier = Modifier.size(24.dp)
                )
                Text(
                  modifier = Modifier.weight(1f),
                  text = service.name,
                  textAlign = TextAlign.Start,
                  style = MaterialTheme.typography.titleMedium
                )
              }
            }

            // Отображение суммарного бейджа непрочитанных уведомлений на смартфоне
            if (totalCount > 0 && !isForwardingMode) {
              Surface(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .offset(x = 6.dp, y = (-6).dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                tonalElevation = 4.dp
              ) {
                Text(
                  text = if (totalCount > 9) "9+" else totalCount.toString(),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.onError,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}




