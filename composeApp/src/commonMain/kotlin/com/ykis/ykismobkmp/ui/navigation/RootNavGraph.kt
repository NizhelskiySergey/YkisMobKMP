package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.TermsAndConditionScreen
import com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val className = "RootNavGraph"

val LocalContentType = compositionLocalOf<ContentType> { ContentType.SINGLE_PANE }
val LocalNavigationType = compositionLocalOf<NavigationType> { NavigationType.BOTTOM_NAVIGATION }

@OptIn(InternalVoyagerApi::class)
@Composable
fun RootNavGraph(
  appState: YkisPamAppState,
  contentType: ContentType,
  navigationType: NavigationType
) {
  val scope = rememberCoroutineScope()
  val appStartModel = koinInject<AppScreenModel>()
  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()
  val firebaseService = apartmentScreenModel.firebaseService

  val currentStartState by appStartModel.startState.collectAsState()
  val baseUIState by apartmentScreenModel.uiState.collectAsState()
  val pendingChatId by chatScreenModel.pendingPushChatId.collectAsState()
  val selectedUser by chatScreenModel.selectedUser.collectAsState()
  val currentFirebaseUid = firebaseService.uid
  println("[YkisLogKMP.$className.RECOMPOSITION]: ======= КАДР ОБНОВЛЕНИЯ ДЕРЕВА COMPOSE =======")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • currentFirebaseUid = \"$currentFirebaseUid\"")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • currentStartState  = $currentStartState")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.mainLoading = ${baseUIState.mainLoading}")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.userRole    = ${baseUIState.userRole}")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.addressId   = ${baseUIState.addressId}L")
  println("[YkisLogKMP.$className.RECOMPOSITION]: ======================================================")

  val chatUid =
    remember(baseUIState.userRole, baseUIState.apartment, selectedUser, baseUIState.uid) {
      val userRole = baseUIState.userRole
      val apartment = baseUIState.apartment
      val myUid = baseUIState.uid
      val resultUid = when (userRole) {
        UserRole.VodokanalUser, UserRole.YtkeUser, UserRole.TboUser -> apartment.uid ?: ""
        UserRole.OsbbUser -> selectedUser?.uid ?: ""
        UserRole.StandardUser -> myUid ?: ""
        else -> ""
      }
      println("[YkisLogKMP.$className.chatUid_Calc]: Сборка токена комнаты чата. Результат: \"$resultUid\"")
      resultUid
    }

//   ИСПРАВЛЕНО НАМЕРТВО: Провайдер геометрии вынесен на самый верхний уровень,
//   полностью исключая рекурсивную инвалидацию холста при холодном старте!
  CompositionLocalProvider(
    LocalContentType provides contentType,
    LocalNavigationType provides navigationType
  )   {

    // ИСПРАВЛЕНО НАМЕРТВО: Слушатель пушей вынесен в корень провайдера!
    // Теперь он "ловит" сигнал даже во время загрузки (заставки) и выполнит 
    // переход сразу после инициализации навигатора.
    LaunchedEffect(pendingChatId) {
      println("[YkisLogKMP.TRAP.RootNav]: LaunchedEffect Triggered. PendingChatId: \"$pendingChatId\"")
      
      if (pendingChatId != null) {
        val id = pendingChatId!!
        println("[YkisLogKMP.TRAP.RootNav]: [1] Поступил сигнал: \"$id\"")
        
        // Ждем пока пропадет лоадер и появится навигатор
        snapshotFlow { currentStartState }.first { it != AppStartState.Loading }
        // Ждем готовности системы
        snapshotFlow { baseUIState }.first { it.userRole != UserRole.Unknown && !it.mainLoading }

        println("[YkisLogKMP.TRAP.RootNav]: [2] Система готова. Обработка ID: $id")

        val parts = id.split("_")
        if (parts.size >= 3) {
          val addrId = parts[parts.size - 2].toLongOrNull() ?: 0L
          
          if (addrId != 0L) {
            val isResident = baseUIState.userRole == UserRole.StandardUser
            val hasThisApartment = baseUIState.apartments.any { it.addressId == addrId }

            // ИСПРАВЛЕНО: Если это житель и у него нет этой квартиры — блокируем прыжок
            if (isResident && !hasThisApartment) {
              println("[YkisLogKMP.TRAP.RootNav]: [BLOCK] Квартира ${addrId}L не найдена. Прыжок отменен.")
              chatScreenModel.setPendingPushChatId(null)
              return@LaunchedEffect
            }

            if (isResident) {
              val servicePrefix = when {
                id.startsWith("WATER_SERVICE") -> "WATER_SERVICE"
                id.startsWith("WARM_SERVICE") -> "WARM_SERVICE"
                id.startsWith("GARBAGE_SERVICE") -> "GARBAGE_SERVICE"
                else -> "OSBB"
              }
              chatScreenModel.onServiceSelectedForResident(servicePrefix)
            }

            // ИСПРАВЛЕНО НАМЕРТВО: Сначала принудительно переключаем квартиру
            apartmentScreenModel.setAddressId(addrId)
            
            // Ждем синхронизации ID в глобальном стейте
            snapshotFlow { baseUIState }.first { it.addressId == addrId }
            
            // ИСПРАВЛЕНО НАМЕРТВО: Теперь ищем комнату чата по адресу, а не по UID!
            // Это решает проблему дублей у жителей с несколькими квартирами.
            chatScreenModel.selectUserByAddressId(addrId)

            delay(500)
            
            println("[YkisLogKMP.TRAP.RootNav]: [3] >>> ПРЫЖОК В ЧАТ! <<<")
          }
        }
      }
    }

    Scaffold(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) { data -> Snackbar(data) } }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

        // Защитный барьер холодного старта. Пока мы не знаем роль пользователя,
        // мы держим стабильный лоадер. Но после инициализации навигатор больше не уничтожается!
        if (currentStartState == AppStartState.Loading) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
          }
        } else {
          // Вычисляем строго один, валидный и стабильный корневой экран Voyager-стека
          val stableStartScreen = remember {
            when (currentStartState) {
              AppStartState.TermsAndConditions -> TermsAndConditionScreen(appStartModel.cachedTermsText)
              AppStartState.SignIn -> SignInScreen
              AppStartState.VerifyEmail -> VerifyEmailScreenDest
              else -> MainApartmentScreen(contentType = contentType, navigationType = navigationType)
            }
          }

          // Запускаем единственный, монолитный навигатор в приложении
          Navigator(screen = stableStartScreen) { navigator ->
            SlideTransition(navigator)

            // Динамический диспетчер состояний: Безопасное переключение глобальных экранов
            LaunchedEffect(currentStartState) {
              val currentRoute = navigator.lastItem
              
              // ИСПРАВЛЕНО: Не сбрасываем стек, если мы уже в режиме детального просмотра чата (Deep Link)
              if (currentRoute is ChatScreenDest) {
                println("[YkisLogKMP.$className.Dispatcher]: [SKIP] Утримання Deep Link екрану чату.")
                return@LaunchedEffect
              }
              
              // ИСПРАВЛЕНО: Если есть ожидающий пуш — блокируем сброс навигации
              if (pendingChatId != null) {
                println("[YkisLogKMP.TRAP.RootNav]: [GUARD] Блокировка сброса стека (ждем пуш-переход)")
                return@LaunchedEffect
              }

              when (currentStartState) {
                AppStartState.TermsAndConditions -> {
                  if (currentRoute !is TermsAndConditionScreen) {
                    println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Оферта не прийнята. Накатываем TermsAndConditionScreen.")
                    val readyText = appStartModel.cachedTermsText
                    navigator.replaceAll(TermsAndConditionScreen(readyText))
                  }
                }
                AppStartState.SignIn -> {
                  if (currentRoute != SignInScreen && currentRoute != SignUpScreenDest && currentRoute != VerifyEmailScreenDest) {
                    println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Сесія відсутня. Перехід на SignInScreen.")
                    navigator.replaceAll(SignInScreen)
                  }
                }
                AppStartState.VerifyEmail -> {
                  if (currentRoute != VerifyEmailScreenDest) {
                    println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Пошта не підтверджена. Перехід на VerifyEmailScreen.")
                    navigator.replaceAll(VerifyEmailScreenDest)
                  }
                }
                AppStartState.AddApartment,
                AppStartState.InfoApartment,
                AppStartState.UserList -> {
                  // ИСПРАВЛЕНО: Если мы УЖЕ внутри хаба MainApartmentScreen, 
                  // не нужно делать replaceAll, иначе все корутины (чаты) упадут с CancellationException.
                  if (currentRoute !is MainApartmentScreen && currentRoute !is ChatScreenDest) {
                    println("[YkisLogKMP.$className.Dispatcher]: [EXECUTE] Запуск хаба MainApartmentScreen")
                    navigator.replaceAll(
                      MainApartmentScreen(
                        contentType = contentType,
                        navigationType = navigationType
                      )
                    )
                  }
                }
                AppStartState.Loading -> {
                  println("[YkisLogKMP.$className.Dispatcher]: [WAIT] Удержание загрузки...")
                }
              }
            }

            // Исполнитель прыжка (уже внутри навигатора)
            LaunchedEffect(pendingChatId) {
              if (pendingChatId != null) {
                // Ждем готовности стейта
                snapshotFlow { baseUIState }.first { it.userRole != UserRole.Unknown && !it.mainLoading }
                // Ждем синхронизации ID
                val parts = pendingChatId!!.split("_")
                val addrId = parts.getOrNull(parts.size - 2)?.toLongOrNull() ?: 0L
                if (addrId != 0L) {
                   snapshotFlow { baseUIState }.first { it.addressId == addrId }
                   delay(300)
                   println("[YkisLogKMP.TRAP.RootNav]: [EXECUTE] Navigator.push -> ChatScreenDest")
                   navigator.push(ChatScreenDest(chatId = pendingChatId))
                   chatScreenModel.setPendingPushChatId(null)
                }
              }
            }
          }
        }
      }
    }
  }
}

fun cleanNavigateTo(navigator: Navigator, screen: cafe.adriel.voyager.core.screen.Screen) {
  println("[YkisLogKMP.Navigation.cleanNavigateTo]: [CLEAN_START] Тотальное замещение стека на экран: $screen")
  navigator.replaceAll(screen)
}



