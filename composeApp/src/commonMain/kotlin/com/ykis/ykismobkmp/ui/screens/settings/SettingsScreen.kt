package com.ykis.ykismobkmp.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import coil3.compose.AsyncImage


import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val className = "SettingsScreen"

// Глобальная КМР константа тем оформления для диалогов

/**
 * [SettingsScreen] — Нативный кроссплатформенный экран конфигурации профиля и безопасности Voyager.
 */
class SettingsScreen(
  private val onDrawerClick: () -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Инжектируем очищенную KMP-модель настроек через Koin
    val screenModel = koinInject<SettingsScreenModel>()

    // ИСПРАВЛЕНО: collectAsStateWithLifecycle заменен универсальным КМР collectAsState()
    val theme by screenModel.theme.collectAsState()
    val loading by screenModel.loading.collectAsState()

    var themeLocation by remember { mutableStateOf(0) }

    // Плавное КМР переключение лоадера удаления аккаунта / выхода
    Crossfade(targetState = loading, label = "SettingsLoadingFade") { isLoading ->
      if (isLoading) {
        CenteredProgressIndicator()
      } else {
        SettingsScreenStateless(
          theme = theme,
          themeLocation = themeLocation,
          photoUrl = screenModel.photoUrl ?: "",
          email = screenModel.email ?: "",
          onDrawerClick = onDrawerClick,
          setThemeValues = screenModel::setThemeValue,
          onThemeChange = {
            themeLocation = if (theme.isNullOrEmpty()) 2 else themes.indexOf(theme)
          },
          revokeAccess = {
            // Каскадный КМР-выход с полным стиранием облака и SQLite SQLDelight
            screenModel.revokeAccess {
              println("[$className]: Аккаунт успешно удален")
              // navigator.replaceAll(AuthScreen()) // Раскомментируй при подключении AuthScreen
            }
          },
          signOut = {
            screenModel.signOut {
              println("[$className.signOut]: Сессия успешно закрыта, уход на авторизацию")
              // navigator.replaceAll(AuthScreen())
            }
          }
        )
      }
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

  if (showChangeThemeDialog) {
    // Примени свой кастомный SingleSelectDialog, вычитывая строки через КМР Res
    println("[$className]: Открытие окна смены темы. Текущий индекс: $themeLocation")
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
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
      text = "${stringResource(Res.string.version)} 2.1.0 KMP",
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

