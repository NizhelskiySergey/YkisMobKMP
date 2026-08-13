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
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerParams
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ykis.ykismobkmp.*
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
  baseUIState: BaseUIState,
  ledgerUIState: BaseUIState
): List<TotalServiceDebt> {
  val serviceList = mutableListOf<TotalServiceDebt>()

  // ИСПРАВЛЕНО: Проверяем наличие ОСББ по базовому стейту квартиры (baseUIState), 
  // а долги берем из финансового стейта (ledgerUIState)
  if (baseUIState.osmdId != 0L) {
    serviceList.add(
      TotalServiceDebt(
        name = baseUIState.osbb.ifBlank { "ОСББ" },
        color = MaterialTheme.colorScheme.primary,
        debt = ledgerUIState.totalDebt.dolg4,
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
        debt = ledgerUIState.totalDebt.dolg1,
        icon = Icons.Default.WaterDrop,
        contentDetail = ContentDetail.WATER_SERVICE
      ),
      TotalServiceDebt(
        name = stringResource(Res.string.ytke_short),
        color = Color(0xFFFF5722),
        debt = ledgerUIState.totalDebt.dolg2,
        icon = Icons.Default.HotTub,
        contentDetail = ContentDetail.WARM_SERVICE
      ),
      TotalServiceDebt(
        name = stringResource(Res.string.yzhtrans),
        color = Color(0xFF4CAF50),
        debt = ledgerUIState.totalDebt.dolg3,
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
  ledgerUIState: BaseUIState,
  getTotalServiceDebt: (LedgerParams) -> Unit,
  getFastpayTokenByOsbb: (String, Long) -> Unit,
  setContentDetail: (ContentDetail) -> Unit
) {
  val methodName = "ServiceListScreen"
  val uriHandler = LocalUriHandler.current

  // Извлекаем токен Водоканала (9999) для быстрой оплаты из круга
  val vodokanalToken = remember(ledgerUIState.fastpayTokens) {
    ledgerUIState.fastpayTokens.find { it.osbbId == Constants.WATER_SERVICE_ID }?.token
  }

  // ИСПРАВЛЕНО НАМЕРТВО: Фиксация на один стартовый запрос сводного тотала!
  LaunchedEffect(key1 = baseUIState.addressId) {
    val addrId = baseUIState.addressId
    val houseId = baseUIState.houseId
    val uid = baseUIState.uid ?: ""

    println("[YkisLogKMP.$tag.$methodName]: Запрос баланса для о/р: $addrId")

    if (addrId > 0L) {
      // 1. Запит загальних боргів
      getTotalServiceDebt(
        LedgerParams(
          uid = uid,
          addressId = addrId,
          houseId = houseId,
          service = 0.toByte(),
          total = 1.toByte(),
          year = "2026"
        )
      )
      // 2. Примусове завантаження токена для Водоканалу (9999) для кнопки оплати
      if (baseUIState.userRole == UserRole.StandardUser) {
          getFastpayTokenByOsbb(uid, Constants.WATER_SERVICE_ID)
      }
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
      canNavigateBack = false
    )

    Crossfade(
      modifier = Modifier.fillMaxSize(),
      animationSpec = tween(durationMillis = 300),
      targetState = ledgerUIState.isLoading,
      label = "finance_loading"
    ) { isLoading ->
      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
        val allItems = assembleServiceList(baseUIState = baseUIState, ledgerUIState = ledgerUIState)
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
            ledgerUIState.totalDebt.dolg
          } else {
            filteredItems.sumOf { it.debt }
          }
        }
        println("[YkisLogKMP.$tag.$methodName]: Роль: ${baseUIState.userRole}. Видимых служб: ${filteredItems.size}")
        ServiceListStateless(
          modifier = Modifier.fillMaxSize(),
          items = filteredItems,
          debts = { it.debt },
          colors = { it.color },
          total = displayTotal,
          circleLabel = stringResource(Res.string.summary),
          isPayEnabled = !vodokanalToken.isNullOrBlank(),
          onPayClick = if (baseUIState.userRole == UserRole.StandardUser) {
            {
              if (!vodokanalToken.isNullOrBlank()) {
                  val personalAccount = baseUIState.addressId.toString()
                  val jsonParams = "{\"token\":\"$vodokanalToken\",\"personalAccount\":\"$personalAccount\"}"
                  val encodedParams = jsonParams.replace("{", "%7B").replace("}", "%7D").replace("\"", "%22")
                  val url = "https://next.privat24.ua/payments/form/$encodedParams"
                  try {
                    println("[YkisLogKMP.$tag]: Запуск быстрой оплаты Водоканала из главного экрана")
                    uriHandler.openUri(url)
                  } catch (e: Exception) {
                    println("[YkisLogKMP.${tag}_ERROR]: Не удалось открыть ссылку оплаты: ${e.message}")
                  }
              }
            }
          } else null,
          rows = { item ->
            ServiceRow(
              color = item.color,
              title = item.name,
              debt = item.debt,
              icon = item.icon,
              onClick = {
                setContentDetail(item.contentDetail)
              }
            )
          }
        )
      }
    }
  }
}





