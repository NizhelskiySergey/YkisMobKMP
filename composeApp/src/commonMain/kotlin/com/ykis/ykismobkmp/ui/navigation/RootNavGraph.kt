package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen
import com.ykis.ykismobkmp.ui.screens.auth.TermsAndConditionScreen
import com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val className = "RootNavGraph"

// Провайдеры статического контекста для адаптивной геометрии экранов (Смартфон vs Планшет Mac)
val LocalContentType = compositionLocalOf<ContentType> { ContentType.SINGLE_PANE }
val LocalNavigationType = compositionLocalOf<NavigationType> { NavigationType.BOTTOM_NAVIGATION }

/**
 * [RootNavGraph] — Кросплатформова стейт-машина навігації KMP Voyager.
 * ИСПРАВЛЕНО НАМЕРТВО: Дублирование условий полностью вырезано! Роутингом управляет
 * исключительно AppScreenModel, а данный граф является чистым реактивным исполнителем её команд.
 */
@OptIn(InternalVoyagerApi::class)
@Composable
fun RootNavGraph(
  appState: YkisPamAppState,
  contentType: ContentType,
  navigationType: NavigationType,
  initialChatId: String? = null
) {
  val scope = rememberCoroutineScope()

  // ИНЖЕКЦИЯ КМР-МОДЕЛЕЙ И ПЛАТФОРМЕННЫХ СЕРВИСОВ ЧЕРЕЗ KOIN
  val appStartModel = koinInject<AppScreenModel>()
  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()
  val firebaseService = apartmentScreenModel.firebaseService

  // РЕАКТИВНЫЙ СБОР ПОТОКОВ СОСТОЯНИЙ ИЗ ОПЕРАТИВНОЙ ПАМЯТИ СМАРТФОНА
  val currentStartState by appStartModel.startState.collectAsState()
  val baseUIState by apartmentScreenModel.baseUIState.collectAsState()
  val pendingChatId by chatScreenModel.pendingPushChatId.collectAsState()
  val selectedUser by chatScreenModel.selectedUser.collectAsState()

  val currentFirebaseUid = firebaseService.uid

  // ТОТАЛЬНЫЙ СИНХРОННЫЙ ДАМП КАЖДОГО КАДРА РЕКОМПОЗИЦИИ
  println("[YkisLogKMP.$className.RECOMPOSITION]: ======= КАДР ОБНОВЛЕНИЯ ДЕРЕВА COMPOSE =======")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • currentFirebaseUid = \"$currentFirebaseUid\"")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • currentStartState  = $currentStartState")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.mainLoading = ${baseUIState.mainLoading}")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.userRole    = ${baseUIState.userRole}")
  println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.addressId   = ${baseUIState.addressId}L")
  println("[YkisLogKMP.$className.RECOMPOSITION]: ======================================================")

  // ИСПРАВЛЕНО НАМЕРТВО: Явно переопределяем уникальный строковый ключ key!
  // Это полностью ликвидирует IllegalStateException и ошибку генерации Default ScreenKey.
  val initialScreen = remember {
    object : cafe.adriel.voyager.core.screen.Screen {

      // Явный уникальный ключ экрана для рантайма Voyager
      override val key: cafe.adriel.voyager.core.screen.ScreenKey = "RootNavGraph_BufferLoadingScreen"

      @Composable
      override fun Content() {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(strokeWidth = 3.dp)
        }
      }
    }
  }



  // СЛУЖЕБНЫЙ КАЛЬКУЛЯТОР ДИНАМИЧНЫХ ТОКЕНОВ ХАБУ ЧАТІВ ЖКГ МІСТА ЮЖНОГО
  val chatUid = remember(baseUIState.userRole, baseUIState.apartment, selectedUser, baseUIState.uid) {
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

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) { data -> Snackbar(data) } }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

      CompositionLocalProvider(
        LocalContentType provides contentType,
        LocalNavigationType provides navigationType
      ) {
        Navigator(screen = initialScreen) { navigator ->
          SlideTransition(navigator)

          // ====================================================================
          // --- [ЗОЛОТОЙ ФОНД] ЕДИНЫЙ РЕАКТИВНЫЙ ДИСПЕТЧЕР ПЕРЕХОДОВ VOYAGER ---
          // ====================================================================
          // ИСПРАВЛЕНО НАМЕРТВО: Текст оферты теперь извлекается из кэша стейт-машины
          // и пробрасывается в конструктор класса TermsAndConditionScreen.
          // Проверка типа экрана переведена на оператор 'is' для исключения графических петель.
          LaunchedEffect(currentStartState) {
            val currentRoute = navigator.lastItem

            println("[YkisLogKMP.$className.Dispatcher]: ====== РЕАКТИВНЫЙ ПЕРЕКЛЮЧАТЕЛЬ ЭКРАНОВ ======")
            println("[YkisLogKMP.$className.Dispatcher]: • Текущий активный экран в стеке: $currentRoute")
            println("[YkisLogKMP.$className.Dispatcher]: • Получена команда от AppScreenModel: $currentStartState")
            println("[YkisLogKMP.$className.Dispatcher]: ===================================================")

            when (currentStartState) {
              AppStartState.TermsAndConditions -> {
                // ИСПРАВЛЕНО: Проверяем тип класса через оператор 'is'
                if (currentRoute !is TermsAndConditionScreen) {
                  println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Оферта не прийнята. Прокидання готового тексту на TermsAndConditionScreen.")

                  // Извлекаем предварительно скачанный из Remote Config текст договора
                  val readyText = appStartModel.cachedTermsText

                  // Нативно замещаем корень стека экземпляром класса экрана оферты
                  navigator.replaceAll(TermsAndConditionScreen(readyText))
                }
              }

              AppStartState.SignIn -> {
                if (currentRoute != SignInScreen && currentRoute != SignUpScreen && currentRoute != VerifyEmailScreen) {
                  println("[YkisLogKMP.$className.Dispatcher]: [NAV_ACTION] Сесія відсутня. Випалювання стеку та перехід на SignInScreen.")
                  navigator.replaceAll(SignInScreen)
                }
              }

              AppStartState.AddApartment -> {
                println("[YkisLogKMP.$className.Dispatcher]: [EXECUTE] Переход на AddApartmentScreen (Ввод инфо-кода БТИ)")
                navigator.replaceAll(AddApartmentScreen)
              }

              AppStartState.InfoApartment, AppStartState.UserList -> {
                println("[YkisLogKMP.$className.Dispatcher]: [EXECUTE] Переход в адаптивный хаб MainApartmentScreen")
                navigator.replaceAll(
                  MainApartmentScreen(
                    contentType = contentType,
                    navigationType = navigationType
                  )
                )
              }

              AppStartState.Loading -> {
                println("[YkisLogKMP.$className.Dispatcher]: [WAIT] Ожидание вычисления траектории стейт-машиной...")
              }
            }
          }
          // ====================================================================

          // ОБРАБОТКА ХОЛОДНОГО СТАРТА / РАЗБОР ГЛУБОКИХ ПОСИЛАНЬ DEEPLINK ПУШ-СПОВІЩЕНЬ
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

                  delay(400) // Пауза для завершения транзакций SQLDelight в фоновом пуле

                  println("[YkisLogKMP.$className.PushAction]: [NAVIGATING] Накат экрана чата поверх стека.")
                  navigator.push(ChatScreenDest(chatId = initialChatId))
                }
              }
            }
          }

          // СЛУХАЧ ГАРЯЧИХ ПУШ-СИГНАЛІВ ДЛЯ МИТТЄВОГО ВХОДУ В ДІАЛОГИ ЖЕК / ОСМД
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

/**
 * [cleanNavigateTo] — Кросплатформений КМР-аналог утилиты полной очистки истории переходов.
 */
fun cleanNavigateTo(navigator: Navigator, screen: cafe.adriel.voyager.core.screen.Screen) {
  println("[YkisLogKMP.Navigation.cleanNavigateTo]: [CLEAN_START] Тотальное замещение стека на экран: $screen")
  navigator.replaceAll(screen)
}

