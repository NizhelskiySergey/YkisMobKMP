package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // КРИТИЧЕСКИЙ ИМПОРТ: Подключаем КМР inline-функцию для List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

private const val className = "ApartmentList"

/**
 * [ApartmentList] — Кроссплатформенный списочный контейнер лицевых счетов БТИ ЮКИС г. Южный.
 * Полностью типизирован под сквозной Long стандарт и оптимизирован для Mac Desktop и мобильных экранов.
 */
@Composable
fun ApartmentList(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Параметры ID переведены с Int на Long под КМР-стандарт СУБД SQLDelight 2.x
  currentAddressId: Long,
  apartmentList: List<ApartmentEntity>,
  onClick: (Long) -> Unit
) {
  // ИСПРАВЛЕНО: Нативный Log.d заменен на универсальный println() внутри LaunchedEffect
  LaunchedEffect(apartmentList) {
    println("[$className.ApartmentList]: List updated, size: ${apartmentList.size}")
  }

  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .background(color = MaterialTheme.colorScheme.surfaceContainerHighest),
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
    }

    // ИСПРАВЛЕНО: Передаем коллекцию позиционным аргументом и внедряем Long-ключ addressId
    items(
      items = apartmentList,
      key = { it.addressId } // Наш сквозной Long ID первичного ключа СУБД
    ) { apartment ->
      ApartmentListItem(
        apartment = apartment,
        onClick = { id ->
          println("[$className.ApartmentList]: Apartment clicked. ID: $id")
          // Передаем Long идентификатор в лямбду родительского контейнера
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
