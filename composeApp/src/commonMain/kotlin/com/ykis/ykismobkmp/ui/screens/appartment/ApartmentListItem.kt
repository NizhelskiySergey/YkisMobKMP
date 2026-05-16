package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * [ApartmentListItem] — Кроссплатформенный элемент списка квартир/лицевых счетов ЮКИС г. Южный.
 * Полностью типизирован под сквозной Long стандарт и оптимизирован для Mac Desktop и мобильных экранов.
 */
@Composable
fun ApartmentListItem(
  modifier: Modifier = Modifier,
  apartment: ApartmentEntity,
  // ИСПРАВЛЕНО: Параметры ID переведены с Int на Long под КМР-стандарт СУБД SQLDelight
  onClick: (Long) -> Unit = {},
  currentAddressId: Long
) {
  // 1. Анимация рамки выделения активного лицевого счета
  val isSelected = apartment.addressId == currentAddressId

  val borderWidth by animateDpAsState(
    targetValue = if (isSelected) 2.dp else 0.dp,
    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow),
    label = "borderWidthAnim"
  )

  // 2. ИСПРАВЛЕНО: Платформозависимый логгер заменен универсальной функцией println() общего кода Котлина
  LaunchedEffect(isSelected) {
    if (isSelected) {
      println("[$className.ApartmentListItem]: Address ${apartment.addressId} is SELECTED")
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .clickable {
        println("[$className.ApartmentListItem]: Click on ${apartment.addressId}")
        // Передаем Long идентификатор в лямбду родительского контейнера
        onClick(apartment.addressId)
      }
      .border(
        width = borderWidth,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp) // Улучшен визуал скругления рамки Material 3
      )
      .padding(vertical = 12.dp), // Сохранена увеличенная зона клика (Удобно для мышки на Mac/Desktop)
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

