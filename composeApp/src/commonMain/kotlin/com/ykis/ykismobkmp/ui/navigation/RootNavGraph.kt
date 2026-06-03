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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen
import com.ykis.ykismobkmp.ui.screens.auth.TermsAndConditionScreen
import com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
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
  navigationType: NavigationType,
  initialChatId: String? = null
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

    Scaffold(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) { data -> Snackbar(data) } }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

        // Защитный барьер холодного старта. Пока СУБД ЮКІС инициализируется,
        // мы держим стабильный лоадер и не пускаем граф рекомпозиции в транзакции навигаторов.
        if (currentStartState == AppStartState.Loading || baseUIState.mainLoading) {
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
              else -> MainApartmentScreen(contentType = contentType, navigationType = navigationType)
            }
          }

          // Запускаем единственный, монолитный навигатор в приложении
          Navigator(screen = stableStartScreen) { navigator ->
            SlideTransition(navigator)

            // Динамический диспетчер состояний: Безопасное переключение глобальных экранов
            LaunchedEffect(currentStartState) {
              val currentRoute = navigator.lastItem
              when (currentStartState) {
                AppStartState.TermsAndConditions -> {
                  if (currentRoute !is TermsAndConditionScreen) {
                    println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Оферта не прийнята. Накатываем TermsAndConditionScreen.")
                    val readyText = appStartModel.cachedTermsText
                    navigator.replaceAll(TermsAndConditionScreen(readyText))
                  }
                }
                AppStartState.SignIn -> {
                  if (currentRoute != SignInScreen && currentRoute != SignUpScreen && currentRoute != VerifyEmailScreen) {
                    println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Сесія відсутня. Перехід на SignInScreen.")
                    navigator.replaceAll(SignInScreen)
                  }
                }
                AppStartState.AddApartment,
                AppStartState.InfoApartment,
                AppStartState.UserList -> {
                  if (currentRoute !is MainApartmentScreen) {
                    println("[YkisLogKMP.$className.Dispatcher]: [EXECUTE] Запуск монолитного хаба MainApartmentScreen")
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

            // Нативная КМР обработка диплинков и пуш-маршрутов ГИОЦ г. Южного
            LaunchedEffect(baseUIState.userRole, initialChatId) {
              if (baseUIState.userRole != UserRole.Unknown && !initialChatId.isNullOrEmpty()) {
                val parts = initialChatId.split("_")
                println("[YkisLogKMP.$className.PushAction]: [PROCESSING] Сегментів у пуш-шляху: ${parts.size}")
                if (parts.size >= 3) {
                  val addrId = parts[parts.size - 2].toLongOrNull() ?: 0L
                  val targetUid = parts.last()
                  println("[YkisLogKMP.$className.PushAction]: [PARSED] Вытянато: AddressID = ${addrId}L, TargetUID = \"$targetUid\"")
                  if (addrId != 0L) {
                    println("[YkisLogKMP.$className.PushAction]: [EXECUTE] Синхронізація адреси в СУБД...")
                    apartmentScreenModel.setAddressId(addrId)
                    chatScreenModel.selectUserByUid(targetUid)
                    delay(400) // Пауза для фиксации транзакций SQLDelight в фоновом пуле
                    println("[YkisLogKMP.$className.PushAction]: [NAVIGATING] Накат экрана чата поверх стека.")
                    navigator.push(ChatScreenDest(chatId = initialChatId))
                  }
                }
              }
            }

            // Слушатель горячих сигналов из Firebase Cloud Messaging (Пуши)
            LaunchedEffect(pendingChatId) {
              pendingChatId?.let { id ->
                println("[YkisLogKMP.$className.LaunchedEffect_Push]: [HOT_SIGNAL] Отримано гарячий пуш: \"$id\".")
                navigator.push(ChatScreenDest(chatId = id))
                chatScreenModel.setPendingPushChatId(null)
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



