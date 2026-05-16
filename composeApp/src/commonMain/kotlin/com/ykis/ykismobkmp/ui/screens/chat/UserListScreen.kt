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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.components.UserList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

// Кроссплатформенные заглушки сущностей для успешной компиляции списков


// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ СТРОК JETBRAINS
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "UserListScreen"

/**
 * [UserListScreen] — Кроссплатформенный экран списка доступных чатов/квартир ЮКИС.
 * ИСПРАВЛЕНО: Расширяет Screen Voyager, типы ИД переведены на сквозной Long стандарт YkisMobKMP.
 */
class UserListScreen(
  private val userList: List<UserEntity> = emptyList(),
  private val onDrawerClicked: () -> Unit = {},
  private val navigationType: NavigationType = NavigationType.BOTTOM_NAVIGATION,
  private val onUserClicked: (UserEntity) -> Unit = {}
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Извлекаем ScreenModels через Koin мост YkisMobKMP
    val chatViewModel = koinInject<ChatScreenModel>()
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()

    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    val isForwardingMode by chatViewModel.isForwardingMode.collectAsState()
    val searchQuery by chatViewModel.searchQuery.collectAsState()
    val selectedService by chatViewModel.selectedService.collectAsState()

    // Логирование рантайма согласно правилу [Класс.Метод]
    LaunchedEffect(baseUIState.userRole, baseUIState.addressId) {
      println("[$className.Content.LaunchedEffect]: [ENTER] Role: ${baseUIState.userRole} | Service: ${selectedService?.name}")
    }

    Column(modifier = Modifier.fillMaxSize()) {
      // 1. ВЕРХНЯЯ ПАНЕЛЬ С АДАПТИВНЫМ ЗАГОЛОВКОМ
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
        println("[$className.Content.AppBar]: [FIXED_TITLE] -> $result")
        result
      }

      DefaultAppBar(
        title = appBarTitle,
        subtitle = if (baseUIState.userRole == UserRole.StandardUser) "Ваші адреси" else "",
        onDrawerClick = onDrawerClicked,
        canNavigateBack = true,
        onBackClick = {
          println("[$className.Content.onBackClick]: Reset service and exit")
          // ИСПРАВЛЕНО: Приведение к String? для исключения Overload Resolution Ambiguity
          chatViewModel.setSelectedService(null as String?)
          onDrawerClicked()
        },
        navigationType = navigationType
      )

      // 2. УНИВЕРСАЛЬНЫЙ СТРОКОВЫЙ ПОИСК
      if (baseUIState.userRole != UserRole.StandardUser && !isForwardingMode) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { query ->
            println("[$className.Content.Search]: Query -> $query")
            chatViewModel.onSearchQueryChanged(query)
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

      // 3. АНИМИРОВАННЫЙ ИНДИКАТОР ПЕРЕСЫЛКИ СООБЩЕНИЙ ЖЭК / ОСМД
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
              println("[$className.Content.Forwarding]: Cancelled")
              chatViewModel.cancelForwarding()
            }) {
              Text(stringResource(Res.string.cancel))
            }
          }
        }
      }

      // 4. КОНТЕНТ: КРОСС ПЛАТФОРМЕННЫЙ МАППИНГ КВАРТИР В ЧАТ-СУЩНОСТИ
      val finalUserList = remember(
        baseUIState.apartments,
        baseUIState.uid,
        baseUIState.userRole,
        searchQuery,
        userList
      ) {
        if (baseUIState.userRole == UserRole.StandardUser) {
          println("[$className.Content.Mapping]: Transforming ${baseUIState.apartments.size} apts to chats")

          baseUIState.apartments.map { apt ->
            UserEntity(
              uid = baseUIState.uid ?: "",
              address = apt.address,
              addressId = apt.addressId, // Сквозной Long стандарт из BaseUIState
              osbbId = apt.osmdId ?: 0L,  // ИСПРАВЛЕНО: Хардкод '0' заменен на КМР-валидный '0L'
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

      // Внутри UserListScreen.kt в самом низу функции Content()
      UserList(
        userList = finalUserList,
        baseUIState = baseUIState,
        onUserClick = { user ->
          if (isForwardingMode) {
            println("[$className.Content.onUserClick]: Пересилання повідомлення до служби...")

            selectedService?.contentDetail?.let { currentService ->
              // Нативно вызываем добавленный в модель метод, передавая все 3 ожидаемых аргумента!
              chatViewModel.confirmForwardToService(
                service = currentService,
                baseState = baseUIState,
                targetUser = user
              )
            }
          } else {
            println("[$className.Content.onUserClick]: Opening chat -> ${user.address}")
            onUserClicked(user)
          }
        },
        chatViewModel = chatViewModel
      )

    }
  }
}

