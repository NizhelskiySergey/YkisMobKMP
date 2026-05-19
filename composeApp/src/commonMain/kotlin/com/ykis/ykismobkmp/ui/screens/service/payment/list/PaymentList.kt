package com.ykis.ykismobkmp.ui.screens.service.payment.list
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.ykis.ykismobkmp.ui.components.EmptyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.PaymentEntity

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ JETBRAINS:
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "PaymentList"

/**
 * [PaymentList] — Кроссплатформенный списочный контейнер архива квитанций абонента ГИОЦ г. Южный.
 * Автоматически переключается на состояние пустой ленты, если за выбранный год нет транзакций.
 */
@Composable
fun PaymentList(
  modifier: Modifier = Modifier,
  paymentList: List<PaymentEntity>,
  osbb: String
) {
  if (paymentList.isEmpty()) {
    // ИСПРАВЛЕНО: Заменен Android R.string на КМР Res.string для бесшовной локализации
    EmptyListState(
      title = stringResource(Res.string.no_payment),
      subtitle = stringResource(Res.string.no_payment_year)
    )
  } else {
    LazyColumn(
      modifier = modifier.fillMaxSize(),
      contentPadding = PaddingValues(vertical = 8.dp)
    ) {
      // ИСПРАВЛЕНО: Внедрен уникальный Long-ключ recID для стабильных 60 FPS при скроллинге таблиц на Mac
      items(
        items = paymentList,
        key = { it.recID } // Наш сквозной Long ID первичного ключа таблицы paymentEntity
      ) { payment ->
        PaymentListItem(
          item = payment,
          osbb = osbb
        )
      }
    }
  }
}

