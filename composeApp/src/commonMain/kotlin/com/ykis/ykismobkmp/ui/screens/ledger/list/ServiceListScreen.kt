package com.ykis.ykismobkmp.ui.screens.ledger.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerParams
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.ledger.TotalDebtState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "ServiceListScreen"

data class TotalServiceDebt(
  val name: String,
  val color: Color,
  val debt: Double,
  val icon: ImageVector,
  val contentDetail: ContentDetail
)

@Composable
fun assembleServiceList(
  totalDebtState: TotalDebtState,
  baseUIState: BaseUIState
): List<TotalServiceDebt> {
  val serviceList = mutableListOf<TotalServiceDebt>()

  if (baseUIState.osmdId != 0L) {
    serviceList.add(
      TotalServiceDebt(
        name = baseUIState.osbb.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.my_osbb),
        color = MaterialTheme.colorScheme.primary,
        debt = totalDebtState.totalDebt.dolg4 ?: 0.0,
        icon = Icons.Default.CorporateFare,
        contentDetail = ContentDetail.OSBB
      )
    )
  }
  serviceList.addAll(
    listOf(
      TotalServiceDebt(
        name = stringResource(Res.string.vodokanal),
        color = Color(0xFF2196F3),
        debt = totalDebtState.totalDebt.dolg1 ?: 0.0,
        icon = Icons.Default.Water,
        contentDetail = ContentDetail.WATER_SERVICE
      ),
      TotalServiceDebt(
        name = stringResource(Res.string.ytke_short),
        color = Color(0xFFFF5722),
        debt = totalDebtState.totalDebt.dolg2 ?: 0.0,
        icon = Icons.Default.HotTub,
        contentDetail = ContentDetail.WARM_SERVICE
      ),
      TotalServiceDebt(
        name = stringResource(Res.string.yzhtrans),
        color = Color(0xFF4CAF50),
        debt = totalDebtState.totalDebt.dolg3 ?: 0.0,
        icon = Icons.Default.Commute,
        contentDetail = ContentDetail.GARBAGE_SERVICE
      )
    )
  )
  return serviceList
}

@Composable
fun ServiceListScreen(
  baseUIState: BaseUIState,
  onDrawerClick: () -> Unit,
  totalDebtState: TotalDebtState,
  getTotalServiceDebt: (LedgerParams) -> Unit,
  setContentDetail: (ContentDetail) -> Unit
) {
  val methodName = "ServiceListScreen"

  // Извлекаем текущий выбранный подмодуль из общего стейта задолженностей
  val currentDetail = totalDebtState.serviceDetail

  // ИСПРАВЛЕНО НАМЕРТВО: Добавлен второй триггерный ключ currentDetail!
  // Теперь при клике мышкой по любой ЖКХ-службе Южного LaunchedEffect мгновенно перезапустится,
  // высчитает правильный код service и отправит точечный запрос в Ktor-сеть без зависаний!
  // ИСПРАВЛЕНО НАМЕРТВО: Все числовые коды приведены к типу Byte через .toByte()!
  // Ошибка 'Argument type mismatch: actual type is Int, but Byte was expected' полностью уничтожена.
  LaunchedEffect(key1 = baseUIState.addressId, key2 = currentDetail) {
    val addrId = baseUIState.addressId
    val houseId = baseUIState.houseId
    val osbbId = baseUIState.osmdId
    val uid = baseUIState.uid ?: ""

    // Вычисляем код услуги сразу в типе Byte
    val targetServiceCode: Byte = when (currentDetail) {
      ContentDetail.WATER_SERVICE -> 1.toByte()
      ContentDetail.WARM_SERVICE  -> 2.toByte()
      ContentDetail.GARBAGE_SERVICE -> 3.toByte()
      ContentDetail.OSBB            -> 4.toByte()
      else -> 0.toByte()
    }

    println("[$tag.$methodName]: [TRIGGER] Смена фокуса подмодуля. ServiceCode: $targetServiceCode, Найдено о/р: $addrId")

    if (addrId > 0L) {
      println("[$tag.$methodName]: [SEND_CHECK] Запрос тарифов. UID: $uid, Service: $targetServiceCode, Total: 1")
      getTotalServiceDebt(
        LedgerParams(
          uid = uid,
          addressId = addrId,
          houseId = houseId,
          year = "2026",
          service = targetServiceCode, // Теперь типы идеально совпадают!
          total = 1.toByte(),

        )
      )
    }
  }


  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
  ) {
    DefaultAppBar(
      title = stringResource(Res.string.accrued),
      subtitle = baseUIState.address,
      onDrawerClick = onDrawerClick,
      canNavigateBack = false,
      actionButton = {
        IconButton(onClick = { setContentDetail(ContentDetail.PAYMENT_LIST) }) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = "Історія платіжок",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    )

    Crossfade(
      modifier = Modifier.fillMaxSize(),
      animationSpec = tween(durationMillis = 300),
      targetState = totalDebtState.isLoading,
      label = "finance_loading"
    ) { isLoading ->
      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
        val allItems = assembleServiceList(totalDebtState = totalDebtState, baseUIState = baseUIState)
        val filteredItems = remember(allItems, baseUIState.userRole) {
          when (baseUIState.userRole) {
            UserRole.VodokanalUser -> allItems.filter { it.contentDetail == ContentDetail.WATER_SERVICE }
            UserRole.YtkeUser      -> allItems.filter { it.contentDetail == ContentDetail.WARM_SERVICE }
            UserRole.TboUser       -> allItems.filter { it.contentDetail == ContentDetail.GARBAGE_SERVICE }
            UserRole.OsbbUser      -> allItems.filter {
              it.contentDetail != ContentDetail.WATER_SERVICE &&
                it.contentDetail != ContentDetail.WARM_SERVICE &&
                it.contentDetail != ContentDetail.GARBAGE_SERVICE
            }
            else -> allItems
          }
        }
        val displayTotal = remember(filteredItems, baseUIState.userRole) {
          if (baseUIState.userRole == UserRole.StandardUser) {
            totalDebtState.totalDebt.dolg ?: 0.0
          } else {
            filteredItems.sumOf { it.debt }
          }
        }
        println("[$tag.$methodName]: [DISPLAY] Роль аккаунта: ${baseUIState.userRole}. Видимых служб в хабе: ${filteredItems.size}")
        ServiceListStateless(
          modifier = Modifier.fillMaxSize(),
          items = filteredItems,
          debts = { it.debt },
          colors = { it.color },
          total = displayTotal,
          circleLabel = stringResource(Res.string.summary),
          rows = { item ->
            ServiceRow(
              color = item.color,
              title = item.name,
              debt = item.debt,
              icon = item.icon,
              onClick = {
                // Клик нативно меняет контентное состояние, что триггерит наш LaunchedEffect
                setContentDetail(item.contentDetail)
              }
            )
          }
        )
      }
    }
  }
}



