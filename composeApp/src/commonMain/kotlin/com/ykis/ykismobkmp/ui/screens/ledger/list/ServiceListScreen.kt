package com.ykis.ykismobkmp.ui.screens.ledger.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.CorporateFare
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

/**
 * [TotalServiceDebt] — Кроссплатформенная UI-модель распределения финансовых долей ЖКХ-служб.
 */
data class TotalServiceDebt(
  val name: String,
  val color: Color,
  val debt: Double,
  val icon: ImageVector,
  val contentDetail: ContentDetail
)
/**
 * [assembleServiceList] — Сборщик локализованного списка долгов ГИОЦ по предприятиям города Южный.
 * ИСПРАВЛЕНО: Платформенные цвета и строки переведены на КМР-стандарты JetBrains Res.
 */
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
        // RECHЕNIE: Страхуем все Double-поля начислений биллинга г. Южного
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


/**
 * [ServiceListScreen] — Сводный экран структуры задолженностей и переплат лицевого счета.
 */
@Composable
fun ServiceListScreen(
  baseUIState: BaseUIState,
  onDrawerClick: () -> Unit,
  totalDebtState: TotalDebtState,
  getTotalServiceDebt: (LedgerParams) -> Unit,
  setContentDetail: (ContentDetail) -> Unit
) {
  val methodName = "ServiceListScreen"

  // ТРИГГЕР ЗАГРУЗКИ ФИНАНСОВЫХ МЕТРИК ИЗ REST API KTOR
  // ИСПРАВЛЕНО: Условия проверок переведены на сквозные Long-константы (0L)
  LaunchedEffect(key1 = baseUIState.addressId) {
    val addrId = baseUIState.addressId
    val houseId = baseUIState.houseId
    val osbbId = baseUIState.osmdId
    val uid = baseUIState.uid ?: ""

    println("[$tag.$methodName]: [TRIGGER] Сработал ключ переключения о/р: $addrId")

    if (addrId > 0L) {
      println("[$tag.$methodName]: [SEND_CHECK] Запрос баланса. UID: $uid, House: $houseId, OSBB: $osbbId")

      getTotalServiceDebt(
        LedgerParams(
          uid = uid,
          addressId = addrId,
          houseId = houseId,
          service = 0,
          total = 1,
          year = "2026"
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
        // Переход в архив совершенных квитанций
        IconButton(onClick = { setContentDetail(ContentDetail.PAYMENT_LIST) }) {
          Icon(
            // ИСПРАВЛЕНО: Заменен нативный Android-вектор на кроссплатформенный КМР-ресурс JetBrains Res
            painter = painterResource(Res.drawable.ic_history),
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
        // Вызываем Composable-сборку списка долей начислений
        val allItems = assembleServiceList(totalDebtState = totalDebtState, baseUIState = baseUIState)

        // Динамическая КМР-фильтрация видимости предприятий на основе уровня прав доступа сессии (Роли)
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
            else -> allItems // Рядовой жилец видит все квитанции ГИОЦ
          }
        }

        // Расчет итоговой консолидированной суммы задолженности
        // ИСПРАВЛЕНО: Безопасное извлечение общего консолидированного долга
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
              onClick = { setContentDetail(item.contentDetail) }
            )
          }
        )
      }
    }
  }
}

