package com.ykis.ykismobkmp.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.getPlatform
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.SingleSelectDialog
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val THEME_KEY = "theme_key"

enum class AppLanguage(val isoCode: String, val displayNameRes: StringResource) {
    UKRAINIAN("uk", Res.string.language_uk),
    RUSSIAN("ru", Res.string.language_ru)
}

val themeOptions = listOf("light", "dark", "system")

class SettingsScreen(
  private val onDrawerClick: () -> Unit
) : Screen {

  override val key: cafe.adriel.voyager.core.screen.ScreenKey = "SettingsScreen_KMP_Instance"

  @Composable
  override fun Content() {
    val screenModel = koinInject<SettingsScreenModel>()
    val appStartModel = koinInject<AppScreenModel>()
    val appCache = koinInject<com.russhwolf.settings.Settings>()
    val loading by screenModel.loading.collectAsState()
    
    // Стейт для темы
    var activeThemeString by remember {
      mutableStateOf(appCache.getString(key = THEME_KEY, defaultValue = "system"))
    }
    val themeIndex = themeOptions.indexOf(activeThemeString).let { if (it == -1) 2 else it }

    // Стейт для языка
    val languages = AppLanguage.entries
    val currentLanguageCode by screenModel.language.collectAsState()
    val languageIndex = languages.indexOfFirst { it.isoCode == currentLanguageCode }.let { if (it == -1) 0 else it }

    var showChangeThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    val restartMessage = stringResource(Res.string.restart_required)

    Crossfade(targetState = loading, label = "SettingsLoadingFade") { isLoading ->
      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(strokeWidth = 3.dp)
        }
      } else {
        SettingsScreenStateless(
          currentLanguage = stringResource(languages[languageIndex].displayNameRes),
          photoUrl = screenModel.photoUrl,
          email = screenModel.email,
          onDrawerClick = onDrawerClick,
          onChangeThemeClick = { showChangeThemeDialog = true },
          onChangeLanguageClick = { showLanguageDialog = true },
          revokeAccess = {
            screenModel.revokeAccess { isSessionExpired ->
              if (isSessionExpired) {
                showReauthDialog = true
              } else {
                appCache.putBoolean("is_terms_accepted", false)
                appStartModel.evaluateStartDestination()
              }
            }
          },
          signOut = {
            screenModel.signOut {
              appStartModel.evaluateStartDestination()
            }
          }
        )
      }
    }

    // ДИАЛОГ ВЫБОРА ТЕМЫ
    if (showChangeThemeDialog) {
        SingleSelectDialog(
            title = stringResource(Res.string.choose_mode),
            icon = Icons.Default.DarkMode,
            optionsList = listOf(
                stringResource(Res.string.lite_mode),
                stringResource(Res.string.dark_mode),
                stringResource(Res.string.system_mode)
            ),
            defaultSelected = themeIndex,
            submitButtonText = stringResource(Res.string.save),
            dismissButtonText = stringResource(Res.string.cancel),
            onSubmitButtonClick = { id ->
                val selectedKey = themeOptions[id]
                appCache.putString(key = THEME_KEY, value = selectedKey)
                activeThemeString = selectedKey
                screenModel.setThemeValue(selectedKey)
                showChangeThemeDialog = false
            },
            onDismissRequest = { showChangeThemeDialog = false }
        )
    }

    // ДИАЛОГ ВЫБОРА ЯЗЫКА
    if (showLanguageDialog) {
        SingleSelectDialog(
            title = stringResource(Res.string.language),
            icon = Icons.Default.Language,
            headerText = restartMessage, // Теперь передаем как заголовок
            optionsList = languages.map { stringResource(it.displayNameRes) },
            defaultSelected = languageIndex,
            submitButtonText = stringResource(Res.string.save),
            dismissButtonText = stringResource(Res.string.cancel),
            onSubmitButtonClick = { id ->
                val selectedLang = languages[id].isoCode
                screenModel.setLanguageValue(selectedLang)
                showLanguageDialog = false
            },
            onDismissRequest = { showLanguageDialog = false }
        )
    }

    // ДИАЛОГ ПЕРЕЗАХОДА (REAUTH)
    if (showReauthDialog) {
      AlertDialog(
        onDismissRequest = { showReauthDialog = false },
        title = { Text("⚠️ " + stringResource(Res.string.sign_out_title), fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(Res.string.revoke_access_message)) },
        dismissButton = {
          TextButton(onClick = { showReauthDialog = false }) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
          Button(onClick = {
              showReauthDialog = false
              screenModel.signOut {
                appCache.putBoolean("is_terms_accepted", false)
                appStartModel.evaluateStartDestination()
              }
          }) { Text(stringResource(Res.string.sign_in_button)) }
        },
        shape = RoundedCornerShape(24.dp)
      )
    }
  }
}

@Composable
fun SettingsScreenStateless(
  modifier: Modifier = Modifier,
  currentLanguage: String,
  photoUrl: String,
  email: String,
  onDrawerClick: () -> Unit,
  onChangeThemeClick: () -> Unit,
  onChangeLanguageClick: () -> Unit,
  revokeAccess: () -> Unit,
  signOut: () -> Unit,
) {
  var showLogOutDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
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
      ProfileCard(photoUrl = photoUrl, email = email)

      // КАРТОЧКА ТЕМЫ
      SettingItemCard(
          icon = Icons.Default.DarkMode,
          title = stringResource(Res.string.theme_mode),
          value = "",
          onClick = onChangeThemeClick
      )

      // КАРТОЧКА ЯЗЫКА
      SettingItemCard(
          icon = Icons.Default.Language,
          title = stringResource(Res.string.language),
          value = currentLanguage,
          onClick = onChangeLanguageClick
      )

      Spacer(modifier = Modifier.weight(1f))

      ActionButtons(
        onLogOutClick = { showLogOutDialog = true },
        onDeleteAccountClick = { showDeleteAccountDialog = true }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showDeleteAccountDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteAccountDialog = false },
      title = { Text(stringResource(Res.string.delete_account_title)) },
      text = { Text(stringResource(Res.string.delete_account_description)) },
      dismissButton = {
        TextButton(onClick = { showDeleteAccountDialog = false }) { Text(stringResource(Res.string.cancel)) }
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
        TextButton(onClick = { showLogOutDialog = false }) { Text(stringResource(Res.string.cancel)) }
      },
      confirmButton = {
        Button(onClick = {
            signOut()
            showLogOutDialog = false
        }) { Text(stringResource(Res.string.sign_out)) }
      }
    )
  }
}

@Composable
fun SettingItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = CardDefaults.shape)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (value.isNotEmpty()) {
                    Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
      AsyncImage(
        model = photoUrl,
        contentDescription = null,
        error = painterResource(Res.drawable.ic_account_circle),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(48.dp).clip(CircleShape)
      )
      Text(text = email, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}

@Composable
fun ActionButtons(
  modifier: Modifier = Modifier,
  onLogOutClick: () -> Unit,
  onDeleteAccountClick: () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), onClick = onLogOutClick) {
      Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
      Text(text = stringResource(Res.string.log_out), style = MaterialTheme.typography.labelLarge)
    }

    TextButton(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      onClick = onDeleteAccountClick,
      colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.error, containerColor = Color.Transparent)
    ) {
      Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
      Text(text = stringResource(Res.string.delete_acc), style = MaterialTheme.typography.labelLarge)
    }

    Text(
      modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
      text = stringResource(
          Res.string.version_info, 
          com.ykis.ykismobkmp.AppConfig.APP_VERSION, 
          stringResource(Res.string.developer_info)
      ),
      style = MaterialTheme.typography.labelMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
