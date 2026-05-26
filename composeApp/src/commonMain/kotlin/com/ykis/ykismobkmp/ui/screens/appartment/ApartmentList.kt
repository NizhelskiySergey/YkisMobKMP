package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

private const val className = "ApartmentList"

/**
 * [ApartmentList] — Кросплатформенний списочний контейнер особових рахунків БТІ ЮКІС м. Южне.
 * ІСПРАВЛЕНО: Шлях пакету приведено до єдиного доменного стандарту (apartment),
 * префікси логування вирівняні під сквозний корпоративний еталон [YkisLogKMP]. Повна заміна.
 */
@Composable
fun ApartmentList(
  modifier: Modifier = Modifier,
  currentAddressId: Long,
  apartmentList: List<ApartmentEntity>,
  onClick: (Long) -> Unit
) {
  // Логування оновлення списку рахунків у реальному часі
  LaunchedEffect(apartmentList) {
    println("[YkisLogKMP.$className]: Список рахунків оновлено, розмір: ${apartmentList.size}")
  }

  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .background(color = MaterialTheme.colorScheme.surfaceContainerHighest),
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
    }

    // Передаємо колекцію позиційним аргументом та впроваджуємо Long-ключ addressId для оптимізації рендеру Skiko
    items(
      items = apartmentList,
      key = { it.addressId }
    ) { apartment ->
      ApartmentListItem(
        apartment = apartment,
        onClick = { id ->
          println("[YkisLogKMP.$className]: Клік по рахунку в списку. ID: $id")
          onClick(id)
        },
        currentAddressId = currentAddressId
      )
    }

    item {
      Spacer(modifier = Modifier.height(4.dp))
    }
  }
}
