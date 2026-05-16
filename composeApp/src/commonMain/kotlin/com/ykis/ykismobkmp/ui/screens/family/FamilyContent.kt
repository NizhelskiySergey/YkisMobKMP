package com.ykis.ykismobkmp.ui.screens.family

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
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "FamilyContent"

/**
 * [FamilyContent] — Кроссплатформенный Stateless-компонент отображения состава семьи и проживающих жителей.
 * Изолирован от контекстов Android SDK и готов к нативному рендерингу на любой операционной системе.
 */
@Composable
fun FamilyContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState
) {
  // ИСПРАВЛЕНО: Внедряем очищенную KMP-модель через кроссплатформенный koinInject()
  val screenModel = koinInject<FamilyListScreenModel>()
  val state by screenModel.state.collectAsState()

  // Триггер фонового обновления списка жителей при смене активного адреса квартиры
  LaunchedEffect(baseUIState.addressId) {
    // ИСПРАВЛЕНО: addressId передается как Long напрямую в КМР-метод без кастинга типов
    if (baseUIState.addressId != 0L) {
      screenModel.getFamilyList(baseUIState.uid ?: "", baseUIState.addressId)
    }
  }

  Box(
    modifier = modifier
      .padding(horizontal = 8.dp)
      .fillMaxSize()
  ) {
    // Контент списочной ленты
    AnimatedVisibility(
      visible = !state.isLoading,
      enter = fadeIn(tween(300)),
      exit = fadeOut(tween(300))
    ) {
      FamilyList(
        familyList = state.familyList,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Кроссплатформенный круговой индикатор прогресса по центру холста
    if (state.isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}

/**
 * [FamilyList] — Вертикальная Lazy-лента карточек проживающих мешканців г. Южный.
 */
@Composable
fun FamilyList(
  familyList: List<FamilyEntity>, // ИСПРАВЛЕНО: Тип коллекции синхронизирован с KMP-моделью
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(vertical = 12.dp)
  ) {
    // ИСПРАВЛЕНО: items вызван с явным указанием коллекции и уникального Long-ключа recId
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

/**
 * [FamilyListItem] — Визуальная карточка одного зарегистрированного жильца ЮКИС.
 */
@Composable
fun FamilyListItem(
  modifier: Modifier = Modifier,
  person: FamilyEntity,
) {
  // ИСПРАВЛЕНО: cardModifier заменен на стандартный универсальный modifier в соответствии с BaseCard контрактом
  BaseCard(
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

        // Статус родства (например, Власник рахунку / Дитина) под ФИО абонента
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

    // 2. Блок технических данных БТИ: Дата рождения, Паспорт, ИНН налоговой г. Южный
    // ИСПРАВЛЕНО: Все вызовы строк R.string заменены на JetBrains Res.string
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
