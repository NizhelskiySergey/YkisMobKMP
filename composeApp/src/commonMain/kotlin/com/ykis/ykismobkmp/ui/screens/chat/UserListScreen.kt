package com.ykis.ykismobkmp.ui.screens.chat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.list.TotalServiceDebt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val className = "UserListScreen"
class UserListScreen(
  private val onDrawerClicked: () -> Unit = {},
  private val onUserClicked: (UserEntity) -> Unit = {}
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Кроссплатформенная инжекция Koin ScreenModel финансового и чат хаба ЮКІС
    val chatScreenModel = koinInject<ChatScreenModel>()
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()

    // Исправлено: Ссылка приведена к легитимному имени потока uiState, убирая Unresolved reference!
    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    // Реактивно подписываемся на динамические сокет-потоки запечатанной вьюмодели чата
    val liveUserList by chatScreenModel.userList.collectAsState()
    val isForwardingMode by chatScreenModel.isForwardingMode.collectAsState()
    val searchQuery by chatScreenModel.searchQuery.collectAsState()
    val selectedService by chatScreenModel.selectedService.collectAsState()

    LaunchedEffect(baseUIState.userRole, baseUIState.addressId) {
      println("[YkisLogKMP.$className.Content.LaunchedEffect]: [ENTER] Роль сесії: ${baseUIState.userRole} | Активна служба: ${selectedService?.name}")
    }

    Column(modifier = Modifier.fillMaxSize()) {
      val vodokanalTitle = stringResource(Res.string.vodokanal)
      val ytkeTitle = stringResource(Res.string.ytke)
      val garbageTitle = stringResource(Res.string.yzhtrans)

      val appBarTitle = remember(baseUIState.userRole, selectedService, baseUIState.osbb, vodokanalTitle, ytkeTitle, garbageTitle) {
        val role = baseUIState.userRole
        val serviceName = selectedService?.name ?: ""
        if (role == UserRole.StandardUser) {
          if (serviceName.contains("ОСББ", ignoreCase = true) || serviceName.isBlank()) {
            "ОСББ чати"
          } else {
            serviceName
          }
        } else {
          when (role) {
            UserRole.VodokanalUser -> vodokanalTitle
            UserRole.YtkeUser -> ytkeTitle
            UserRole.TboUser -> garbageTitle
            else -> baseUIState.osbb
          }
        }
      }

      // Динамічне визначення можливості повернення назад (тільки для мешканців)
      val canNavigateBack = remember(baseUIState.userRole) {
        baseUIState.userRole == UserRole.StandardUser
      }

      DefaultAppBar(
        title = appBarTitle,
        subtitle = if (baseUIState.userRole == UserRole.StandardUser) "Ваші адреси" else "Список чатів",
        onDrawerClick = onDrawerClicked,
        canNavigateBack = canNavigateBack,
        navigationType = com.ykis.ykismobkmp.ui.navigation.LocalNavigationType.current,
        onBackClick = {
          if (canNavigateBack) {
            println("[YkisLogKMP.$className.Content.onBackClick]: Повернення до вибору служби.")
            chatScreenModel.setSelectedService(null as TotalServiceDebt?)
            onDrawerClicked()
          }
        }
      )

      if (baseUIState.userRole != UserRole.StandardUser && !isForwardingMode) {
        // ФІКС: Локальний стейт для запобігання перемішуванню цифр при швидкому введенні
        var localSearchQuery by remember { mutableStateOf(searchQuery) }

        OutlinedTextField(
          value = localSearchQuery,
          onValueChange = { query ->
            localSearchQuery = query
            chatScreenModel.onSearchQueryChanged(query)
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          placeholder = { Text("Пошук за адресою або о/р...", fontSize = 14.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (localSearchQuery.isNotEmpty()) {
              IconButton(onClick = { 
                localSearchQuery = ""
                chatScreenModel.onSearchQueryChanged("") 
              }) {
                Icon(Icons.Default.Close, contentDescription = null)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )
      }

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
              println("[YkisLogKMP.$className.Content.Forwarding]: Режим пересилання повідомлення скасовано користувачем.")
              chatScreenModel.cancelForwarding()
            }) {
              Text(stringResource(Res.string.cancel))
            }
          }
        }
      }

      // Нативный КМР-фильтр и маппинг квартир. Все типы данных жестко удерживаются в Long!
      val finalUserList = remember(
        baseUIState.apartments,
        baseUIState.uid,
        baseUIState.userRole,
        searchQuery,
        liveUserList
      ) {
        if (baseUIState.userRole == UserRole.StandardUser) {
          println("[YkisLogKMP.$className.Content.Mapping]: Мапінг ${baseUIState.apartments.size} адрес БТІ у доменні об'єкти кімнат чату.")
          baseUIState.apartments.map { apt ->
            UserEntity(
              uid = baseUIState.uid ?: "",
              address = apt.address,
              addressId = apt.addressId, // Чистый Long тип
              osbbId = apt.osmdId,  // Чистый Long тип
              displayName = apt.address,
              userRole = UserRole.StandardUser,
              nanim = apt.nanim,
              fio = apt.nanim
            )
          }.filter {
            it.address.contains(searchQuery, ignoreCase = true)
          }
        } else {
          // Исправлено: Диспетчеры теперь реактивно читают живой liveUserList сокетов Firebase вместо пустоты!
          liveUserList
        }
      }

      UserList(
        modifier = Modifier.weight(1f),
        userList = finalUserList,
        baseUIState = baseUIState,
        onUserClick = { user ->
          if (isForwardingMode) {
            println("[YkisLogKMP.$className.Content.onUserClick]: Виконання пересилання повідомлення до обраного абонента...")
            selectedService?.contentDetail?.let { currentService ->
              // Вызываем метод с передачей трех легитимных аргументов-контекстов КМР-модели чата
              chatScreenModel.confirmForwardToService(
                service = currentService,
                baseState = baseUIState,
                targetUser = user
              )
            }
          } else {
            println("[YkisLogKMP.$className.Content.onUserClick]: Перехід до кімнати обговорення -> ${user.address}")
            onUserClicked(user)
          }
        },
        chatScreenModel = chatScreenModel // Намертво стыкуем контекст для корректного вывода бейджей
      )
    }
  }
}
