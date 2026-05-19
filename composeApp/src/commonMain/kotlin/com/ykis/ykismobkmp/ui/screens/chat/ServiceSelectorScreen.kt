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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.service.TotalDebtState
import com.ykis.ykismobkmp.ui.screens.service.list.TotalServiceDebt
import com.ykis.ykismobkmp.ui.screens.service.list.assembleServiceList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.services

// Временные КМР-заглушки вспомогательных ЖКХ-классов, пока не присланы сорцы моделей служб

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ СТРОК JETBRAINS
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "ServiceSelectorScreen"

/**
 * [ServiceSelectorScreen] — Кроссплатформенный экран выбора коммунальных служб и ОСМД г. Южного.
 * ИСПРАВЛЕНО: Объявлен как класс Screen Voyager, ChatViewModel заменен на ChatScreenModel.
 */
class ServiceSelectorScreen(
  private val baseUIState: BaseUIState,
  private val onServiceClick: (TotalServiceDebt) -> Unit,
  private val onDrawerClicked: () -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val adaptiveNavigationType = LocalNavigationType.current

    // Кроссплатформенная инжекция Koin ScreenModel вместо Android ViewModel
    val chatScreenModel = koinInject<ChatScreenModel>()

    ServiceSelectorContent(
      baseUIState = baseUIState,
      chatScreenModel = chatScreenModel,
      onServiceClick = onServiceClick,
      onDrawerClicked = onDrawerClicked,
      navigationType = adaptiveNavigationType
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

  // ИСПРАВЛЕНО: Платформенные подписки Lifecycle заменены на нативные КМР .collectAsState()
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val isForwardingMode by chatScreenModel.isForwardingMode.collectAsState()
  val selectedService by chatScreenModel.selectedService.collectAsState()

  Column(modifier = modifier.fillMaxSize()) {
    DefaultAppBar(
      title = stringResource(Res.string.services), // ИСПРАВЛЕНО: Перевод на JetBrains Res строку
      subtitle = "Оберіть службу",
      onDrawerClick = onDrawerClicked,
      navigationType = navigationType
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        modifier = Modifier
          .width(IntrinsicSize.Max)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val residentServices = assembleServiceList(
          totalDebtState = TotalDebtState(),
          baseUIState = baseUIState
        )

        residentServices.forEach { service ->
          // СЧИТАЕМ СУММАРНЫЙ БЕЙДЖ НЕПРОЧИТАННЫХ (По всем КМР Long квартирам жильца для этой службы)
          val totalCount = baseUIState.apartments.sumOf { apt ->
            val chatId = when (service.contentDetail) {
              // ИСПРАВЛЕНО: Перевод ИД в интерполяцию строк без принудительных кастов к Int под SQLDelight
              ContentDetail.OSBB -> "OSBB_${apt.osmdId}_${apt.addressId}_${baseUIState.uid}"
              ContentDetail.WATER_SERVICE -> "WATER_SERVICE_9999_${apt.addressId}_${baseUIState.uid}"
              ContentDetail.WARM_SERVICE -> "WARM_SERVICE_9998_${apt.addressId}_${baseUIState.uid}"
              ContentDetail.GARBAGE_SERVICE -> "GARBAGE_SERVICE_9997_${apt.addressId}_${baseUIState.uid}"
              else -> "${service.contentDetail.name}_${apt.addressId}_${baseUIState.uid}"
            }
            unreadCounts[chatId] ?: 0
          }

          Box(modifier = Modifier.fillMaxWidth()) {
            Button(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
              onClick = {
                if (isForwardingMode) {
                  // ИСПРАВЛЕНО: Log.d заменен на println в формате [Класс.Метод]
                  println("[$tag.$methodName]: [FORWARD_TO_SERVICE] Пересылка сообщения в службу: ${service.contentDetail}")
                  chatScreenModel.confirmForwardToService(service.contentDetail, baseUIState)
                } else {
                  println("[$tag.$methodName]: [SELECT_SERVICE] Выбрана ветка чата: ${service.name}")
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

            // Отображение суммарного бейджа непрочитанных уведомлений
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

