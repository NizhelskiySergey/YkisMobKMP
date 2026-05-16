package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.ContentType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import org.koin.compose.koinInject

private const val className = "InfoApartmentScreen"

// КМР Перечисление адаптивной сетки (Планшет/Смартфон)
enum class ContentType { SINGLE_PANE, DUAL_PANE }

/**
 * [InfoApartmentScreen] — Кроссплатформенный экран отображения данных БТИ и состава семьи Voyager.
 * Одинаково плавно рендерится в окнах Mac Desktop (JVM), Android и iOS.
 */
class InfoApartmentScreen(
  private val contentType: ContentType,
  private val onDrawerClicked: () -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Внедряем единую очищенную модель экрана через Koin
    val screenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by screenModel.baseUIState.collectAsState()

    // ЗОЛОТОЙ ФОНД ЛОКАЛЬНОГО КЭШИРОВАНИЯ БТИ ДАННЫХ
    // ИСПРАВЛЕНО: Проверки переведены на Long-константы (0L) под стандарты SQLDelight 2.x
    LaunchedEffect(baseUIState.addressId, baseUIState.apartments.size, baseUIState.uid) {
      val currentFirebaseUid = baseUIState.uid

      if (currentFirebaseUid == null || baseUIState.apartments.isEmpty()) {
        println("[$className.LaunchedEffect]: [WAIT] Данные биллинга Южного еще загружаются")
        return@LaunchedEffect
      }

      val targetId: Long = if (baseUIState.addressId != 0L) {
        baseUIState.addressId
      } else {
        baseUIState.apartments.firstOrNull()?.addressId ?: 0L
      }

      if (targetId != 0L && targetId != screenModel.lastLoadedAddressId) {
        println("[$className.LaunchedEffect]: [LOAD] Запрос характеристик квартиры ID: $targetId")
        screenModel.getApartment(targetId)
      }
    }

    InfoApartmentScreenStateless(
      contentType = contentType,
      baseUIState = baseUIState,
      onDrawerClicked = onDrawerClicked,
      deleteApartment = {
        screenModel.deleteApartment { route ->
          println("[$className]: Квартира удалена, перенаправление на добавление")
          // navigator.replaceAll(AddApartmentScreen())
        }
      }
    )
  }
}

/**
 * [InfoApartmentScreenStateless] — Адаптивная декларативная верстка экрана БТИ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoApartmentScreenStateless(
  modifier: Modifier = Modifier,
  contentType: ContentType,
  baseUIState: BaseUIState,
  onDrawerClicked: () -> Unit,
  deleteApartment: () -> Unit
) {
  var selectedTab by rememberSaveable { mutableStateOf(0) }
  var showWarningDialog by remember { mutableStateOf(false) }

  // 1. Модальное КМР-окно подтверждения удаления привязки квартиры
  if (showWarningDialog) {
    AlertDialog(
      onDismissRequest = { showWarningDialog = false },
      icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.createErrorColor()) },
      title = { Text("Вилучити особовий рахунок?") },
      text = { Text("Ви дійсно хочете видалити цю квартиру з памяти устройства? Дані нарахувань міста Южне не будуть порушені.") },
      dismissButton = {
        TextButton(onClick = { showWarningDialog = false }) { Text("Скасувати") }
      },
      confirmButton = {
        Button(
          onClick = {
            println("[$className.delete]: Confirmation received")
            deleteApartment()
            showWarningDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Видалити")
        }
      }
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      DefaultAppBar(
        title = baseUIState.address.takeIf { it.isNotEmpty() } ?: "Прилади БТІ ЮКІС",
        subtitle = " о/р ${baseUIState.addressId}",
        canNavigateBack = false,
        onDrawerClick = onDrawerClicked,
        actionButton = {
          // Кнопка удаления доступна только стандартному жильцу, админы служб защищены от случайной порчи кэша
          if (baseUIState.userRole == UserRole.StandardUser) {
            IconButton(onClick = { showWarningDialog = true }) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Видалити",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
    ) {
      if (contentType == ContentType.DUAL_PANE) {
        // Широкоформатная верстка для Mac Desktop (Две панели рядом)
        InfoScreenDualPanelContent(baseUIState = baseUIState)
      } else {
        // Мобильный режим / Узкое окно Mac (Переключение по табам)
        PrimaryTabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surface,
          divider = { HorizontalDivider(thickness = 0.5.dp) }
        ) {
          val tabTitles = listOf("Технічний паспорт", "Склад сім'ї")
          tabTitles.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = {
                println("[$className.Tab]: Switch to $index")
                selectedTab = index
              },
              text = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                )
              },
              icon = {
                Icon(
                  imageVector = if (index == 0) Icons.Default.Home else Icons.Default.People,
                  contentDescription = null
                )
              }
            )
          }
        }

        // Плавная КМР-анимация скольжения контента по табам
        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            if (targetState > initialState) {
              (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
              (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }.using(SizeTransform(clip = false))
          },
          label = "TabContentAnimation"
        ) { targetIndex ->
          Box(modifier = Modifier.fillMaxSize()) {
            when (targetIndex) {
              0 -> BtiPanelContent(baseUIState = baseUIState) // ИСПРАВЛЕНО: viewModel удалена из параметров
              else -> FamilyContent(baseUIState = baseUIState)
            }
          }
        }
      }
    }
  }
}

/**
 * [InfoScreenDualPanelContent] — Двухпанельная верстка БТИ под большие мониторы Mac.
 */
@Composable
fun InfoScreenDualPanelContent(
  baseUIState: BaseUIState
) {
  Row(modifier = Modifier.fillMaxSize()) {
    // Левая панель: БТИ (45% ширины экрана)
    Surface(
      modifier = Modifier.weight(0.45f).fillMaxHeight(),
      // ИСПРАВЛЕНО: surfaceColorAtElevation заменен официальным Material 3 контейнером
      color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
      Column {
        DualPaneHeader(Icons.Default.Home, "Технічний паспорт БТІ")
        BtiPanelContent(baseUIState = baseUIState)
      }
    }

    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

    // Правая панель: Состав семьи (55% ширины экрана)
    Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
      DualPaneHeader(Icons.Default.People, "Склад сім'ї")
      FamilyContent(baseUIState = baseUIState)
    }
  }
}

@Composable
private fun DualPaneHeader(icon: ImageVector, title: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )
  }
  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp)
}

// Заглушка расширения цвета, гарантирующая стабильный КМР вывод цвета ошибок
private fun MaterialTheme.createErrorColor() = androidx.compose.ui.graphics.Color(0xFFD32F2F)

// Временные заглушки панелей (Пришли их оригинальные файлы следующим шагом, и мы причешем их под КМР)
@Composable fun BtiPanelContent(baseUIState: BaseUIState) { Box(Modifier.fillMaxSize()) { Text("Характеристики БТІ") } }
@Composable fun FamilyContent(baseUIState: BaseUIState) { Box(Modifier.fillMaxSize()) { Text("Список мешканців") } }
