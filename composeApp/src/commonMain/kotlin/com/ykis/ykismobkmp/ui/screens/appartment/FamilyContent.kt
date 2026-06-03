package com.ykis.ykismobkmp.ui.screens.appartment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*
private const val className = "FamilyContent"

@Composable
private fun FlatCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
  ) {
    Column(modifier = Modifier.padding(12.dp)) { content() }
  }
}

@Composable
fun FamilyContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  viewModel: ApartmentScreenModel // Удерживаем КМР-синхронизацию вкладок Хаба
) {
  // Внедряем нашу чистую кроссплатформенную модель экрана семьи
  val familyScreenModel = koinInject<FamilyListScreenModel>()
  val familyUIState by familyScreenModel.uiState.collectAsState()

  LaunchedEffect(baseUIState.addressId) {
    // Первичный Long-идентификатор ГИОЦ передается напрямую в КМР-метод без кастинга типов
    if (baseUIState.addressId != 0L) {
      println("[$className.invoke]: Запуск КМР UseCase отримання складу сім'ї для о/р Long: ${baseUIState.addressId}")
      familyScreenModel.getFamilyList(baseUIState.uid ?: "", baseUIState.addressId)
    }
  }

  Box(
    modifier = modifier
      .padding(horizontal = 8.dp)
      .fillMaxSize()
  ) {
    // Контент списочной ленты
    AnimatedVisibility(
      visible = !familyUIState.mainLoading,
      enter = fadeIn(tween(300)),
      exit = fadeOut(tween(300))
    ) {
      FamilyList(
        familyList = familyUIState.familyList,
        modifier = Modifier.fillMaxSize()
      )
    }
    if (familyUIState.mainLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}

@Composable
fun FamilyList(
  familyList: List<FamilyEntity>,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(vertical = 12.dp)
  ) {
    items(
      items = familyList,
      key = { it.recId } // Наш сквозной первичный Long-ключ таблицы SQLDelight
    ) { person ->
      FamilyListItem(
        person = person,
        modifier = Modifier.fillMaxWidth()
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )
    }
  }
}

@Composable
fun FamilyListItem(
  modifier: Modifier = Modifier,
  person: FamilyEntity,
) {
  // Нативно рендерим FlatCard для бесперебойной отрисовки Skiko на Mac Desktop и Android
  FlatCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 12.dp)
  ) {
    // 1. Шапка: Аватар-иконка + ФИО проживающего
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
      Column(
        modifier = Modifier
          .padding(start = 16.dp)
          .weight(1f)
      ) {
        Text(
          text = "${person.surname} ${person.fistname} ${person.lastname}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          lineHeight = 22.sp
        )
        Text(
          text = person.rodstvo.takeIf { it.isNotEmpty() } ?: "Мешканець",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium
        )
      }
    }

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 8.dp),
      thickness = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (person.born.isNotEmpty()) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 1.dp),
          labelText = stringResource(Res.string.born_text),
          valueText = person.born
        )
      }
      if (person.document.isNotEmpty()) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 1.dp),
          labelText = stringResource(Res.string.doc_text),
          valueText = person.document
        )
      }
      if (person.inn.isNotEmpty()) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 1.dp),
          labelText = stringResource(Res.string.inn_text),
          valueText = person.inn
        )
      }
    }
  }
}


