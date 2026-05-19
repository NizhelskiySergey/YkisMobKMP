package com.ykis.ykismobkmp.ui.screens.service.payment.list
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.ColumnLabelTextWithTextAndIcon
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

@Composable fun ColumnLabelTextWithTextAndIcon(modifier: Modifier = Modifier, labelText: String, valueText: String, imageVector: androidx.compose.ui.graphics.vector.ImageVector) { Column(modifier) { Row { Icon(imageVector, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(labelText) }; Text(valueText, fontWeight = FontWeight.Medium) } }
@Composable fun LabelTextWithText(modifier: Modifier = Modifier, labelText: String, valueText: String) { Row(modifier) { Text(labelText); Text(valueText, fontWeight = FontWeight.Medium) } }

private const val className = "PaymentListItem"

/**
 * [PaymentListItem] — Кроссплатформенный элемент строки архива квитанций абонента ГИОЦ г. Южный.
 * ИСПРАВЛЕНО: Платформозависимые иконки заменены встроенными КМР-векторами, аннотация Preview переведена на Skiko-стандарт.
 */
@Composable
fun PaymentListItem(
  modifier: Modifier = Modifier,
  item: PaymentEntity,
  osbb: String
) {
  // Безопасный КМР-парсинг даты расчетного центра (формат ГГГГ-ММ-ДД или ДД.ММ.ГГГГ)
  val formattedDate = rememberFormattedDateKmp(item.data)

  BaseCard(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      ColumnLabelTextWithTextAndIcon(
        modifier = Modifier.weight(1f),
        labelText = stringResource(Res.string.date_colon),
        valueText = formattedDate,
        imageVector = Icons.Default.DateRange
      )
      ColumnLabelTextWithTextAndIcon(
        modifier = Modifier.weight(1f),
        labelText = stringResource(Res.string.point_of_sale),
        valueText = item.kassa,
        imageVector = Icons.Default.ReceiptLong // ИСПРАВЛЕНО: Заменена отсутствующая PointOfSale
      )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

    // Блок начислений квартплаты и ремонтного фонда ОСМД г. Южного
    if (item.remont != 0.0 || item.kvartplata != 0.0) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.HomeWork, // ИСПРАВЛЕНО: Заменена отсутствующая CorporateFare
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = osbb,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.height(IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically
      ) {
        VerticalDivider(
          thickness = 2.dp,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(vertical = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          if (item.kvartplata != 0.0) {
            LabelTextWithText(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
              labelText = stringResource(Res.string.kvartplata_colon),
              valueText = item.kvartplata.formatMoneyStringKmp()
            )
          }
          if (item.remont != 0.0) {
            LabelTextWithText(
              modifier = Modifier.padding( 8.dp, vertical = 1.dp),
              labelText = stringResource(Res.string.rfond_colon),
              valueText = item.remont.formatMoneyStringKmp()
            )
          }
        }
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
    }

    // Начисления Водоканала г. Южный
    if (item.voda != 0.0) {
      ColumnLabelTextWithTextAndIcon(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.vodokanal_colon),
        valueText = item.voda.formatMoneyStringKmp(),
        imageVector = Icons.Default.Water
      )
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
    }

    // Начисления теплосети ЮТКЕ
    if (item.otoplenie != 0.0) {
      ColumnLabelTextWithTextAndIcon(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.ytke_colon),
        valueText = item.otoplenie.formatMoneyStringKmp(),
        imageVector = Icons.Default.LocalFireDepartment // ИСПРАВЛЕНО: Заменена отсутствующая HotTub
      )
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
    }

    // Начисления Южтранса (вывоз мусора ТБО)
    if (item.tbo != 0.0) {
      ColumnLabelTextWithTextAndIcon(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.yzhtrans_colon),
        valueText = item.tbo.formatMoneyStringKmp(),
        imageVector = Icons.Default.LocalShipping // ИСПРАВЛЕНО: Заменена отсутствующая Commute
      )
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
    }

    // Итоговая суммарная строка квитанции расчетного центра
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(Res.string.summary),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "${item.summa.formatMoneyStringKmp()} ${stringResource(Res.string.uah)}",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black
      )
    }
  }
}

/**
 * [formatMoneyStringKmp] — Кроссплатформенное форматирование вещественных сумм начислений ЖКХ.
 */
fun Double.formatMoneyStringKmp(): String {
  val rounded = (this * 100.0).toLong()
  val mainPart = rounded / 100
  val kopecks = rounded % 100
  val kopecksStr = if (kopecks < 10) "0$kopecks" else "$kopecks"
  return "$mainPart.$kopecksStr"
}

/**
 * [rememberFormattedDateKmp] — Безопасный КМР-парсер строк дат биллинга без привлечения тяжелых Java-библиотек.
 */
@Composable
fun rememberFormattedDateKmp(rawDate: String): String {
  return remember(rawDate) {
    try {
      if (rawDate.contains("-")) {
        val parts = rawDate.split("-")
        if (parts.size >= 3) "${parts[2]}.${parts[1]}.${parts[0]}" else rawDate
      } else {
        rawDate
      }
    } catch (e: Exception) {
      rawDate
    }
  }
}

/**
 * ИСПРАВЛЕНО: Аннотация Preview переведена на кроссплатформенный КМР-стандарт JetBrains.
 */
@Preview
@Composable
private fun PreviewPaymentListItem() {
  YkisPAMTheme {
    PaymentListItem(
      item = PaymentEntity(
        voda = 146.35,
        otoplenie = 124.88,
        tbo = 64.00,
        remont = 46.2,
        kvartplata = 322.60,
        summa = 704.03
      ),
      osbb = "Кондомінімум 16"
    )
  }
}
