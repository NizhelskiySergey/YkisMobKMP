package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.NavigationType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.apartment.BaseUIState
import com.ykis.ykismobkmp.ui.screens.apartment.UserRole
import com.ykis.ykismobkmp.ui.screens.chat.components.UserList
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res
import android.util.Log

private const val className = "UserListScreen"

@Composable
fun UserListScreen(
  modifier: Modifier = Modifier,
  userList: List<UserEntity>,
  baseUIState: BaseUIState,
  onUserClicked: (UserEntity) -> Unit,
  onDrawerClicked: () -> Unit,
  navigationType: NavigationType,
  chatViewModel: ChatViewModel
) {
  // Подписки на состояния ViewModel (Мультиплатформенный collectAsState)
  val isForwardingMode by chatViewModel.isForwardingMode.collectAsState()
  val searchQuery by chatViewModel.searchQuery.collectAsState()
  val selectedService by chatViewModel.selectedService.collectAsState()

  LaunchedEffect(baseUIState.userRole, baseUIState.addressId) {
    Log.d("YkisLog", "[$className.LaunchedEffect]: [ENTER] Role: ${baseUIState.userRole} | Service: ${selectedService?.name}")
  }

  Column(modifier = modifier.fillMaxSize()) {
    // 1. ВЕРХНЯЯ ПАНЕЛЬ
    val appBarTitle = remember(baseUIState.userRole, selectedService) {
      val role = baseUIState.userRole
      val serviceName = selectedService?.name ?: ""

      val result = if (role == UserRole.StandardUser) {
        if (serviceName.contains("ОСББ", ignoreCase = true) || serviceName.isBlank()) {
          "ОСББ чати"
        } else {
          serviceName
        }
      } else {
        "список доступних чатів"
      }
      Log.d("YkisLog", "[$className.AppBar]: [FIXED_TITLE] -> $result")
      result
    }

    DefaultAppBar(
      title = appBarTitle,
      subtitle = if (baseUIState.userRole == UserRole.StandardUser) "Ваші адреси" else "",
      onDrawerClick = onDrawerClicked,
      canNavigateBack = true,
      onBackClick = {
        Log.d("YkisLog", "[$className.onBackClick]: Reset service and exit")
        chatViewModel.setSelectedService(null)
        onDrawerClicked()
      },
      navigationType = navigationType
    )

    // 2. ПОИСК (Универсальный)
    if (baseUIState.userRole != UserRole.StandardUser && !isForwardingMode) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = {
          Log.v("YkisLog", "[$className.Search]: Query -> $it")
          chatViewModel.onSearchQueryChanged(it)
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Пошук...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { chatViewModel.onSearchQueryChanged("") }) {
              Icon(Icons.Default.Close, contentDescription = null)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )
    }

    // 3. ИНДИКАТОР ПЕРЕСЫЛКИ
    AnimatedVisibility(visible = isForwardingMode) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(20.dp))
          Text(
            text = stringResource(Res.string.select_recipient),
            modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
            style = MaterialTheme.typography.labelLarge
          )
          TextButton(onClick = {
            Log.d("YkisLog", "[$className.Forwarding]: Cancelled")
            chatViewModel.cancelForwarding()
          }) {
            Text(stringResource(Res.string.cancel))
          }
        }
      }
    }

    // 4. КОНТЕНТ (Маппинг квартир в чаты для жильца)
    val finalUserList = remember(baseUIState.apartments, baseUIState.uid, baseUIState.userRole, searchQuery, userList) {
      if (baseUIState.userRole == UserRole.StandardUser) {
        Log.d("YkisLog", "[$className.Mapping]: Transforming ${baseUIState.apartments.size} apts to chats")

        baseUIState.apartments.map { apt ->
          UserEntity(
            uid = baseUIState.uid ?: "",
            address = apt.address,
            addressId = apt.addressId,
            osbbId = apt.osmdId ?: 0,
            displayName = apt.address,
            userRole = UserRole.StandardUser,
            nanim = apt.nanim ?: ""
          )
        }.filter {
          it.address.contains(searchQuery, ignoreCase = true)
        }
      } else {
        userList
      }
    }

    UserList(
      userList = finalUserList,
      baseUIState = baseUIState,
      onUserClick = { user ->
        if (isForwardingMode) {
          Log.i("YkisLog", "[$className.onUserClick]: Forwarding message to ${user.address}")
          chatViewModel.confirmForward(user)
        } else {
          Log.d("YkisLog", "[$className.onUserClick]: Opening chat -> ${user.address}")
          onUserClicked(user)
        }
      },
      chatViewModel = chatViewModel
    )
  }
}
