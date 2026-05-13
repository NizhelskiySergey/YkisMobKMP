package com.ykis.ykismobkmp.ui.screens.appartment


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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val className = "ApartmentListItemKt"

@Composable
fun ApartmentListItem(
  modifier: Modifier = Modifier,
  apartment: ApartmentEntity,
  onClick: (Int) -> Unit = {},
  currentAddressId: Int
) {
  // 1. Анимация рамки выделения
  val isSelected = apartment.addressId == currentAddressId

  val borderWidth by animateDpAsState(
    targetValue = if (isSelected) 3.dp else 0.dp, // В KMP лучше использовать 0.dp вместо отрицательных значений
    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow),
    label = "borderWidthAnim"
  )

  // 2. Логирование согласно правилу [Класс.Метод]
  LaunchedEffect(isSelected) {
    if (isSelected) {
      Log.d("YkisLog", "[$className.ApartmentListItem]: Address ${apartment.addressId} is SELECTED")
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .clickable {
        Log.d("YkisLog", "[$className.ApartmentListItem]: Click on ${apartment.addressId}")
        onClick(apartment.addressId)
      }
      .border(
        width = borderWidth,
        // Используем основной цвет темы, если extendedColor еще не инициализирован для KMP
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.extraSmall
      )
      .padding(vertical = 8.dp), // Увеличил кликабельную зону для Mac/Desktop
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      modifier = Modifier.padding(horizontal = 12.dp),
      imageVector = Icons.Default.Home,
      contentDescription = null,
      tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
      text = apartment.address,
      style = MaterialTheme.typography.bodyLarge,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Preview
@Composable
private fun PreviewApartmentListItem() {
  YkisPAMTheme {
    Column {
      ApartmentListItem(
        apartment = ApartmentEntity(
          address = "Хіміків 6/64",
          addressId = 23
        ),
        currentAddressId = 23
      )
      ApartmentListItem(
        apartment = ApartmentEntity(
          address = "Будівельників 10/1",
          addressId = 24
        ),
        currentAddressId = 23
      )
    }
  }
}

