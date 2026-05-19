package com.ykis.ykismobkmp.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ProfileTopBar"

/**
 * [ProfileTopBar] — Кроссплатформенная верхняя панель навигации профиля абонента ЮКИС.
 * ИСПРАВЛЕНО: Убраны зависимости от R.string, выпадающие элементы переведены на DropdownMenuItem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
  modifier: Modifier = Modifier,
  signOut: () -> Unit,
  revokeAccess: () -> Unit,
  navigateBack: () -> Unit
) {
  var openMenu by remember { mutableStateOf(false) }

  TopAppBar(
    modifier = modifier.fillMaxWidth(),
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(Res.string.profile),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    navigationIcon = {
      IconButton(
        onClick = navigateBack,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onSurface
        )
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(Res.string.back_button),
          modifier = Modifier.size(24.dp)
        )
      }
    },
    actions = {
      Box {
        IconButton(onClick = { openMenu = !openMenu }) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(Res.string.more_options_button),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        DropdownMenu(
          expanded = openMenu,
          onDismissRequest = { openMenu = false },
          modifier = Modifier
            .width(200.dp)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          // Элемент Выхода из сессии биллинга г. Южный
          DropdownMenuSignOutItem(
            onSignOutClick = {
              openMenu = false
              signOut()
            }
          )

          // Элемент Безопасного удаления аккаунта
          DropdownMenuDeleteAccountItem(
            onDeleteClick = {
              openMenu = false
              revokeAccess()
            }
          )
        }
      }
    }
  )
}

/**
 * [DropdownMenuSignOutItem] — Кроссплатформенная кнопка выхода с подтверждающим AlertDialog.
 */
@Composable
fun DropdownMenuSignOutItem(onSignOutClick: () -> Unit) {
  var showWarningDialog by remember { mutableStateOf(false) }

  DropdownMenuItem(
    text = { Text(stringResource(Res.string.sign_out)) },
    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
    onClick = { showWarningDialog = true }
  )

  if (showWarningDialog) {
    AlertDialog(
      onDismissRequest = { showWarningDialog = false },
      title = { Text(stringResource(Res.string.sign_out_title)) },
      text = { Text(stringResource(Res.string.sign_out_description)) },
      dismissButton = {
        TextButton(onClick = { showWarningDialog = false }) {
          Text(stringResource(Res.string.cancel))
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showWarningDialog = false
            onSignOutClick()
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(stringResource(Res.string.sign_out))
        }
      }
    )
  }
}

/**
 * [DropdownMenuDeleteAccountItem] — Кнопочный элемент деструктивного удаления профиля.
 */
@Composable
fun DropdownMenuDeleteAccountItem(onDeleteClick: () -> Unit) {
  var showWarningDialog by remember { mutableStateOf(false) }

  DropdownMenuItem(
    text = { Text(stringResource(Res.string.delete_my_account), color = MaterialTheme.colorScheme.error) },
    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
    onClick = { showWarningDialog = true }
  )

  if (showWarningDialog) {
    AlertDialog(
      onDismissRequest = { showWarningDialog = false },
      title = { Text(stringResource(Res.string.delete_account_title)) },
      text = { Text(stringResource(Res.string.delete_account_description)) },
      dismissButton = {
        TextButton(onClick = { showWarningDialog = false }) {
          Text(stringResource(Res.string.cancel))
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showWarningDialog = false
            onDeleteClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(stringResource(Res.string.delete_my_account))
        }
      }
    )
  }
}

