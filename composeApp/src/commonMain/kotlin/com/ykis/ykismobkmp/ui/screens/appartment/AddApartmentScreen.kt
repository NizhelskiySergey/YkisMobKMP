package com.ykis.ykismobkmp.ui.screens.appartment

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobPAM / YkisMobKMP
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import org.koin.compose.koinInject

private const val className = "AddApartmentScreen"

/**
 * [AddApartmentScreen] — Кроссплатформенный экран ввода инфо-кодов и секретных слов ОСМД ЮКИС.
 */
class AddApartmentScreen(
  private val onDrawerClicked: () -> Unit = {},
  private val closeContentDetail: () -> Unit = {}
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Инжектируем очищенную KMP-модель через кроссплатформенный koinInject()
    val screenModel = koinInject<ApartmentScreenModel>()

    // Платформозависимый collectAsStateWithLifecycle заменен на КМР collectAsState()
    val secretCode by screenModel.secretCode.collectAsState("")
    val snackbarMessage by SnackbarManager.snackbarMessages.collectAsState(null)

    // КРОСС ПЛАТФОРМЕННЫЙ СЛУШАТЕЛЬ ОШИБОК И СНЭКБАРОВ
    LaunchedEffect(snackbarMessage) {
      snackbarMessage?.let { msg ->
        val text = msg.toString()
        println("[$className.LaunchedEffect]: [DISPLAYING_SNACKBAR] $text")

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
          println("[$className.onCodeChanged]: Пользователь вводит инфо-код: $newValue")
          screenModel.onSecretCodeChange(newValue)
        },
        onAddClick = {
          println("[$className.onAddClick]: Клик по кнопке отправки инфо-кода биллинга: $secretCode")
          keyboard?.hide()

          // Запускаем привязку, передавая КМР лямбду перезапуска графа
          screenModel.addApartment {
            println("[$className.onAddClick]: [SUCCESS] Код успешно верифицирован биллингом г. Южный")
            closeContentDetail()

            // ИСПРАВЛЕНО НАМЕРТВО: Сбрасываем стек до главного экрана MainMeterScreen взамен удаленного MeterListScreen!
            navigator.replaceAll(MainMeterScreen(onDrawerClick = onDrawerClicked))
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



