package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.meter.MeterListScreen
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.koin.compose.koinInject

private const val className = "AddApartmentScreen"

/**
 * [AddApartmentScreen] — Кроссплатформенный экран ввода инфо-кодов и секретных слов ОСМД ЮКИС.
 * Полностью автономен, интегрирован в Voyager и готов к запуску на Mac Desktop, Android и iOS.
 */
class AddApartmentScreen(
  private val onDrawerClicked: () -> Unit,
  private val closeContentDetail: () -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ИСПРАВЛЕНО: Инжектируем очищенную KMP-модель через кроссплатформенный koinInject()
    val screenModel = koinInject<ApartmentScreenModel>()

    // ИСПРАВЛЕНО: Платформозависимый collectAsStateWithLifecycle заменен на КМР collectAsState()
    val secretCode by screenModel.secretCode.collectAsState("")
    val snackbarMessage by SnackbarManager.snackbarMessages.collectAsState(null)

    // КРОСС ПЛАТФОРМЕННЫЙ СЛУШАТЬ ОШИБОК И СНЭКБАРОВ
    LaunchedEffect(snackbarMessage) {
      snackbarMessage?.let { msg ->
        // Встроенный КМР менеджер сообщений отдает чистую строку без вызовов context.resources
        val text = msg.toString()
        println("[$className]: [DISPLAYING_SNACKBAR] $text")

        snackbarHostState.showSnackbar(
          message = text,
          duration = SnackbarDuration.Short
        )
        SnackbarManager.clearMessage()
      }
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
      AddApartmentScreenStateless(
        modifier = Modifier.padding(padding),
        isButtonEnabled = secretCode.trim().isNotEmpty(),
        onDrawerClicked = onDrawerClicked,
        secretCode = secretCode,
        onCodeChanged = { newValue ->
          println("[$className]: [INPUT] Полозователь вводит: $newValue")
          screenModel.onSecretCodeChange(newValue)
        },
        onAddClick = {
          println("[$className]: [CLICK] Клик по кнопке отправки кода: $secretCode")
          keyboard?.hide()

          // Запускаем привязку, передавая КМР лямбду перезапуска графа (restartApp)
          screenModel.addApartment {
            println("[$className]: [SUCCESS] Код успешно верифицирован биллингом г. Южный")
            closeContentDetail()

            // ИСПРАВЛЕНО: Навигация Jetpack заменена на КМР Voyager. Сбрасываем стек до главного экрана
            navigator.replaceAll(MeterListScreen())
          }
        }
      )
    }
  }
}

/**
 * [AddApartmentScreenStateless] — Чистая адаптивная верстка ввода кодов, изолированная от DI и навигации.
 */
@Composable
fun AddApartmentScreenStateless(
  modifier: Modifier = Modifier,
  isButtonEnabled: Boolean,
  onDrawerClicked: () -> Unit,
  onAddClick: () -> Unit,
  secretCode: String,
  onCodeChanged: (String) -> Unit
) {
  Column(modifier = modifier.fillMaxSize()) {
    // ИСПРАВЛЕНО: Вызовы ресурсов R.string полностью заменены строковыми литералами
    DefaultAppBar(
      onDrawerClick = onDrawerClicked,
      title = "Прив'язка особового рахунку",
      canNavigateBack = false
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Карточка ввода с ограничением максимальной ширины для Desktop-мониторов Mac/Планшетов
      Card(
        modifier = Modifier.widthIn(max = 500.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Text(
            text = "Введіть інфо-код з квитанції ГІОЦ для прив'язки вашої квартири або секретне слово доступу адміністратора ОСББ.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedTextField(
              value = secretCode,
              onValueChange = onCodeChanged,
              modifier = Modifier.weight(1f),
              label = { Text("Код або секретне слово") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              )
            )

            Button(
              onClick = onAddClick,
              enabled = isButtonEnabled,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(54.dp) // Выравниваем высоту кнопки по полю ввода
            ) {
              Text(
                text = "Додати",
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun AddApartmentPreview() {
  YkisPAMTheme {
    Box(modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)) {
      AddApartmentScreenStateless(
        isButtonEnabled = true,
        onDrawerClicked = {},
        onAddClick = {},
        secretCode = "55555555",
        onCodeChanged = {}
      )
    }
  }
}
