package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

private const val className = "ApartmentListItem"

/**
 * [ApartmentListItem] — Кросплатформенний елемент списку квартир/особових рахунків ЮКІС м. Южне.
 * ІСПРАВЛЕНО: Шлях пакету приведено до єдиного доменного стандарту (apartment),
 * префікси логування вирівняні під сквозний корпоративний еталон [YkisLogKMP]. Повна замена.
 */
@Composable
fun ApartmentListItem(
  modifier: Modifier = Modifier,
  apartment: ApartmentEntity,
  onClick: (Long) -> Unit = {},
  currentAddressId: Long
) {
  // 1. Анімація рамки виділення активного особового рахунку БТІ
  val isSelected = apartment.addressId == currentAddressId

  val borderWidth by animateDpAsState(
    targetValue = if (isSelected) 2.dp else 0.dp,
    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow),
    label = "borderWidthAnim"
  )

  // 2. Логування стану активації о/р у реальному часі
  LaunchedEffect(isSelected) {
    if (isSelected) {
      println("[YkisLogKMP.$className]: Рахунок ${apartment.addressId} обрано як АКТИВНИЙ")
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .clickable {
        println("[YkisLogKMP.$className]: Клік по рахунку ID: ${apartment.addressId}")
        // Передаємо Long ідентифікатор у лямбду батьківського Drawer контейнера
        onClick(apartment.addressId)
      }
      .border(
        width = borderWidth,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp) // Покращений візуал заокруглення рамки Material 3
      )
      .padding(vertical = 12.dp), // Збільшена зона кліку (Зручно для мишки на Mac/Desktop)
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      modifier = Modifier.padding(horizontal = 16.dp),
      imageVector = Icons.Default.Home,
      contentDescription = null,
      tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
      text = apartment.address.takeIf { it.isNotEmpty() } ?: "Особовий рахунок № ${apartment.addressId}",
      style = MaterialTheme.typography.bodyLarge,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}
