package com.ykis.ykismobkmp.ui.screens.appartment


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val className = "ApartmentListKt"

@Composable
fun ApartmentList(
  modifier: Modifier = Modifier,
  currentAddressId: Int,
  apartmentList: List<ApartmentEntity>,
  onClick: (Int) -> Unit
) {
  // Логирование согласно правилу [Класс.Метод]
  LaunchedEffect(apartmentList) {
    Log.d("YkisLog", "[$className.ApartmentList]: List updated, size: ${apartmentList.size}")
  }

  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .background(color = MaterialTheme.colorScheme.surfaceContainerHighest),
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
    }

    items(
      items = apartmentList,
      key = { it.addressId }
    ) { apartment ->
      ApartmentListItem(
        apartment = apartment,
        onClick = { id ->
          Log.d("YkisLog", "[$className.ApartmentList]: Apartment clicked. ID: $id")
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

@Preview
@Composable
private fun PreviewApartmentList() {
  YkisPAMTheme {
    ApartmentList(
      currentAddressId = 12,
      apartmentList = listOf(
        ApartmentEntity(addressId = 12, address = "Новобілярська 28-1/25"),
        ApartmentEntity(addressId = 1, address = "Хіміків 6/12")
      ),
      onClick = {}
    )
  }
}

