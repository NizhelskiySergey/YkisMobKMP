package com.ykis.ykismobkmp.ui.screens.settings


import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.SingleSelectDialog
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel
import com.ykis.ykismobkmp.ui.screens.settings.ThemeValues.Companion.fromStorageKey
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.choose_mode
import ykismobkmp.composeapp.generated.resources.dark_mode
import ykismobkmp.composeapp.generated.resources.delete_acc
import ykismobkmp.composeapp.generated.resources.delete_account_description
import ykismobkmp.composeapp.generated.resources.delete_account_title
import ykismobkmp.composeapp.generated.resources.delete_my_account
import ykismobkmp.composeapp.generated.resources.ic_account_circle
import ykismobkmp.composeapp.generated.resources.lite_mode
import ykismobkmp.composeapp.generated.resources.log_out
import ykismobkmp.composeapp.generated.resources.revoke_access_message
import ykismobkmp.composeapp.generated.resources.save
import ykismobkmp.composeapp.generated.resources.settings
import ykismobkmp.composeapp.generated.resources.sign_out
import ykismobkmp.composeapp.generated.resources.sign_out_description
import ykismobkmp.composeapp.generated.resources.sign_out_title
import ykismobkmp.composeapp.generated.resources.system_mode
import ykismobkmp.composeapp.generated.resources.theme_mode
import ykismobkmp.composeapp.generated.resources.version

private const val className = "SettingsScreen"

class SettingsScreen(
  private val onDrawerClick: () -> Unit
) : Screen {

  override val key: cafe.adriel.voyager.core.screen.ScreenKey = "SettingsScreen_KMP_Instance"

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = koinInject<SettingsScreenModel>()
    val appStartModel = koinInject<AppScreenModel>()
    val appCache = koinInject<com.russhwolf.settings.Settings>() // Единый синглтон-источник правды
    val coroutineScope = rememberCoroutineScope()
    val loading by screenModel.loading.collectAsState()
    var activeThemeString by remember {
      mutableStateOf(appCache.getString(key = "theme_key", defaultValue = "system"))
    }

    // Вычисляем точный индекс радиокнопки для SingleSelectDialog напрямую из строки кэша диска
    var themeLocation = remember(activeThemeString) {
      val index = themes.indexOf(activeThemeString)
      if (index == -1) 2 else index // Если ключ пуст — по дефолту синяя точка встанет на 2-й индекс ("system")
    }
    var showReauthDialog by remember { mutableStateOf(false) }
    Crossfade(targetState = loading, label = "SettingsLoadingFade") { isLoading ->
      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(strokeWidth = 3.dp)
        }
      } else {
        SettingsScreenStateless(
          // Передаем строку темы напрямую из нашего дискового стейта, минуя фабричный поток вьюмодели!
          theme = activeThemeString,
          themeLocation = themeLocation,
          photoUrl = screenModel.photoUrl ?: "",
          email = screenModel.email ?: "",
          onDrawerClick = onDrawerClick,

          // Метод записи темы: обновляет диск, локальный стейт экрана и сигнализирует вьюмодели
          setThemeValues = { selectedKey ->
            println("[YkisLogKMP.$className.Content]: Каскадна фіксація пресету теми на диск: \"$selectedKey\"")

            // 1. Прошиваем значение на физический диск через синглтон-кэш
            appCache.putString(key = "theme_key", value = selectedKey)

            // 2. Мгновенно обновляем локальный стейт экрана для синхронизации RadioButton!
            activeThemeString = selectedKey

            // 3. Дублируем запись во вьюмодель настроек (для её внутренних процессов)
            screenModel.setThemeValue(selectedKey)
          },

          onThemeChange = {
            // Принудительно перечитываем строку с диска для обновления индексов при открытии окна
            activeThemeString = appCache.getString(key = "theme_key", defaultValue = "system")
            println("[YkisLogKMP.$className.Content.onThemeChange]: Потоковий індекс RadioButton синхронізовано: $themeLocation (\"$activeThemeString\")")
          },

          revokeAccess = {
            println("[YkisLogKMP.$className.Content.revokeAccess]: Пользователь подтвердил удаление аккаунта.")

            screenModel.revokeAccess { isSessionExpired ->
              if (isSessionExpired) {
                // ИСПРАВЛЕНО НАМЕРТВО: Сессия Google/SMS устарела! Включаем триггер диалога-инструкции!
                println("[YkisLogKMP.$className]: [SHOW_ALERT] Сессия устарела. Включение триггера showReauthDialog.")
                showReauthDialog = true
              } else {
                // УСПЕХ: Облако и локальная СУБД чисты, переходим на стартовую оферту
                println("[YkisLogKMP.$className]: Профиль полностью ликвидирован. Уход на оферту.")
                appCache.putBoolean("is_terms_accepted", false)
                appStartModel.evaluateStartDestination()
              }
            }
          },

          // Внутри override fun Content() в классе SettingsScreen:
          signOut = {
            println("[YkisLogKMP.$className.Content.signOut]: Клієнт підтвердив вихід. Запуск процедури...")
            screenModel.signOut {
              println("[YkisLogKMP.$className.Content.signOut]: Локальний логаут завершено. Перерахунок стартової траєкторії ЮКИС.")

              // Принудительно заставляем центральный диспетчер пересчитать граф.
              // Поскольку сессия Firebase теперь false, он бесшовно и плавно развернет SignInScreen!
              appStartModel.evaluateStartDestination()
            }
          }

        )
      }
    }
    // ====================================================================
    // ЖИВАЯ НАВЕДЕННАЯ ИНСТРУКЦИЯ ПЕРЕЗАХОДА ---
    // ====================================================================
    if (showReauthDialog) {
      AlertDialog(
        onDismissRequest = { showReauthDialog = false },
        title = {
          Text(
            text = "⚠️ Безпека облікового запису",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        },
        text = {
          Text(
            text = stringResource(Res.string.revoke_access_message),
            style = MaterialTheme.typography.bodyMedium
          )
        },
        dismissButton = {
          TextButton(onClick = { showReauthDialog = false }) {
            Text("Скасувати", style = MaterialTheme.typography.labelLarge)
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showReauthDialog = false
              println("[YkisLogKMP.$className]: Инструкция принята. Запуск автоматического signOut для перезахода...")

              // Нативно запускаем наш отлаженный метод выхода, который сотрет локальный кэш и переведет на SignInScreen
              screenModel.signOut {
                appCache.putBoolean("is_terms_accepted", false)
                appStartModel.evaluateStartDestination()
              }
            }
          ) {
            Text("Перезайти", style = MaterialTheme.typography.labelLarge)
          }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
      )
    }
  }
}

/**
 * [SettingsScreenStateless] — Чистая верстка карточек и системных диалогов Material 3 настроек.
 */
@Composable
fun SettingsScreenStateless(
  modifier: Modifier = Modifier,
  theme: String?,
  themeLocation: Int,
  photoUrl: String,
  email: String,
  onThemeChange: () -> Unit,
  setThemeValues: (String) -> Unit,
  revokeAccess: () -> Unit,
  signOut: () -> Unit,
  onDrawerClick: () -> Unit
) {
  var showChangeThemeDialog by rememberSaveable { mutableStateOf(false) }
  var showLogOutDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // ИСПРАВЛЕНО: Заменен Android R.string на КМР Res.string
    DefaultAppBar(
      title = stringResource(Res.string.settings),
      canNavigateBack = false,
      onDrawerClick = onDrawerClick
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // ИСПРАВЛЕНО: В дочерние карточки больше не передается один и тот же внешний modifier
      ProfileCard(photoUrl = photoUrl, email = email)

      ThemeSettingCard(onClick = { showChangeThemeDialog = true })

      Spacer(modifier = Modifier.weight(1f))

      ActionButtons(
        onLogOutClick = { showLogOutDialog = true },
        onDeleteAccountClick = { showDeleteAccountDialog = true }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  // --- МОДАЛЬНЫЕ ОКНА БЕЗОПАСНОСТИ И ТЕМ (AlertDialog КМР-стандарт) ---
  if (showDeleteAccountDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteAccountDialog = false },
      title = { Text(stringResource(Res.string.delete_account_title)) },
      text = { Text(stringResource(Res.string.delete_account_description)) },
      dismissButton = {
        TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Скасувати") }
      },
      confirmButton = {
        Button(
          onClick = {
            revokeAccess()
            showDeleteAccountDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text(stringResource(Res.string.delete_my_account))
        }
      }
    )
  }

  if (showLogOutDialog) {
    AlertDialog(
      onDismissRequest = { showLogOutDialog = false },
      title = { Text(stringResource(Res.string.sign_out_title)) },
      text = { Text(stringResource(Res.string.sign_out_description)) },
      dismissButton = {
        TextButton(onClick = { showLogOutDialog = false }) { Text("Скасувати") }
      },
      confirmButton = {
        Button(
          onClick = {
            signOut()
            showLogOutDialog = false
          }
        ) {
          Text(stringResource(Res.string.sign_out))
        }
      }
    )
  }

  // ====================================================================
  // --- БРОНИРОВАННЫЙ ВЫЗОВ ТВОЕГО РОДНОГО SINGLE_SELECT_DIALOG ---
  // ====================================================================
  if (showChangeThemeDialog) {
    onThemeChange()
    println("[YkisLogKMP.$className]: [THEME_DIALOG] Відкриття модального вікна вибору теми ІС ЮКІС. Індекс у СУБД: $themeLocation")

    // Внутренний флаг-предохранитель для защиты от коллизий и двойных срабатываний
    var isSubmitTriggered by remember { mutableStateOf(false) }

    val localizedThemeLabels = listOf(
      stringResource(Res.string.lite_mode),   // Світла тема
      stringResource(Res.string.dark_mode),   // Темна тема
      stringResource(Res.string.system_mode)  // Системна тема
    )

    SingleSelectDialog(
      modifier = modifier,
      title = stringResource(Res.string.choose_mode),       // "Оберіть тему оформлення"
      optionsList = localizedThemeLabels,                   // Локализованные строки КМР в LazyColumn
      defaultSelected = themeLocation,                      // Текущий выбранный индекс из СУБД
      submitButtonText = stringResource(Res.string.save),   // Кнопка "Зберегти"
      dismissButtonText = stringResource(Res.string.cancel), // Кнопка "Скасувати"
      onSubmitButtonClick = { id ->
        // Шаг 1. Взводим предохранитель: сохранение запущено!
        isSubmitTriggered = true

        val selectedStorageKey = themes[id]
        println("[YkisLogKMP.$className.ThemeDialog]: Клік по кнопці 'Зберегти'. Обрано пресет палітри: \"$selectedStorageKey\"")

        // Шаг 2. Записываем значение в кэш DataStore/Settings через вьюмодель
        setThemeValues(selectedStorageKey)
        onThemeChange()

        // Шаг 3. Реактивно закрываем модальное окно диалога
        showChangeThemeDialog = false
      },
      onDismissRequest = {
        // ИСПРАВЛЕНО НАМЕРТВО: Если встроенный confirmButton пытается вызвать onDismissRequest дуплетом
        // после успешного сохранения — предохранитель блокирует ложный вызов отмены и очищает рантайм!
        if (!isSubmitTriggered) {
          println("[YkisLogKMP.$className.ThemeDialog]: Клік по кнопці 'Скасувати'. Зміни відхилено користувачем.")
          showChangeThemeDialog = false
        } else {
          println("[YkisLogKMP.$className.ThemeDialog]: [GUARD] Каскадний Dismiss після успішного збереження успішно заблоковано.")
        }
      }
    )
  }


}

@Composable
fun ProfileCard(
  modifier: Modifier = Modifier,
  photoUrl: String,
  email: String
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ИСПРАВЛЕНО: Кроссплатформенный вызов Coil 3 без передачи контекста Android SDK
      AsyncImage(
        model = photoUrl,
        contentDescription = null,
        // ИСПРАВЛЕНО: Заменен Android R.drawable на КМР генератор Res
        error = painterResource(Res.drawable.ic_account_circle),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
      )
      Text(
        text = email,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * [ThemeSettingCard] — Карточка быстрого вызова диалога смены темы оформления.
 */
@Composable
fun ThemeSettingCard(
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape = CardDefaults.shape)
        .clickable(onClick = onClick)
        .padding(vertical = 16.dp, horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = Icons.Default.DarkMode,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
      // ИСПРАВЛЕНО: Модификатор weight(1f) изолирован, R.string заменен на Res.string
      Text(
        modifier = Modifier.weight(1f),
        text = stringResource(Res.string.theme_mode),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

/**
 * [ActionButtons] — Кроссплатформенный блок деструктивных кнопок безопасности и версии ЮКИС.
 */
@Composable
fun ActionButtons(
  modifier: Modifier = Modifier,
  onLogOutClick: () -> Unit,
  onDeleteAccountClick: () -> Unit
) {
  // ИСПРАВЛЕНО: Внутренние элементы больше не дублируют входящий modifier
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OutlinedButton(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      onClick = onLogOutClick
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Logout,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.bindDp())
      )
      Text(
        text = stringResource(Res.string.log_out),
        style = MaterialTheme.typography.labelLarge
      )
    }

    TextButton(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      onClick = onDeleteAccountClick,
      colors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
        containerColor = Color.Transparent
      )
    ) {
      Icon(
        imageVector = Icons.Default.DeleteForever,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.bindDp())
      )
      Text(
        text = stringResource(Res.string.delete_acc),
        style = MaterialTheme.typography.labelLarge
      )
    }

    // ИСПРАВЛЕНО: Убран Android-зависимый класс BuildConfig. Версия заменена на КМР строковую константу.
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      text = "ЮКІС версія 1.1 KMP\nрозробник ФОП Ніжельський С.О.",
      style = MaterialTheme.typography.labelMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

// Приватный КМР хелпер для безопасного проброса Int-отступов
private fun Int.bindDp() = this.dp

/**
 * [ThemeValues] — Кроссплатформенное перечисление конфигураций тем оформления ЮКИС.
 * Полностью изолировано от жестких строк и интегрировано с локализацией JetBrains Compose.
 */
@Serializable
enum class ThemeValues(
  val storageKey: String,          // Ключ, сохраняемый в DataStore ("light", "dark", "system")
  val displayTitle: StringResource // Локализованный КМР-ресурс строки для UI диалогов
) {
  LIGHT_MODE("light", Res.string.lite_mode),
  DARK_MODE("dark", Res.string.dark_mode),
  SYSTEM_DEFAULT("system", Res.string.system_mode);

  companion object {
    /**
     * [fromStorageKey] — Восстановление роли темы из строкового кэша DataStore.
     */
    fun fromStorageKey(key: String?): ThemeValues {
      return entries.find { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM_DEFAULT
    }
  }
}

/**
 * [themes] — Чистый кроссплатформенный список строковых ключей для совместимости с SingleSelectDialog.
 * Полностью синхронизирован со статическим массивом тем нашей SettingsScreenModel.
 */
val themes: List<String> = listOf(
  ThemeValues.LIGHT_MODE.storageKey,
  ThemeValues.DARK_MODE.storageKey,
  ThemeValues.SYSTEM_DEFAULT.storageKey
)

