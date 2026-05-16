package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

// Импорты твоих оригинальных UI-скриптов верстки экранов и ScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreenModel


private const val className = "ScreensRegistry"

// ====================================================================
// --- МОНОЛИТНЫЕ КМР ЭКРАНЫ VOYAGER (ГРАФИКА + НАВИГАЦИЯ В СТАТИКЕ) ---
// ====================================================================

/**
 * [SignInScreen] — Экран авторизации абонента ЮКИС.
 * ИСПРАВЛЕНО: Интегрирован нативный КМР-метод навигации Voyager.
 */
/**
 * [SignInScreen] — Экран авторизации абонента ЮКИС.
 * ИСПРАВЛЕНО: Сигнатура аргументов приведена в точечное соответствие с твоим openScreen.
 */
object SignInScreen : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    println("[$className.SignInScreen]: Отрисовка окна входа")

    com.ykis.ykismobkmp.ui.screens.auth.SignInScreen(
      // ИСПРАВЛЕНО: Вызываем твой родной параметр openScreen.
      // Вместо старого navController мы перенаправляем строковый маршрут на нативные push/replaceAll Voyager!
      openScreen = { targetRoute ->
        println("[$className.SignInScreen]: Сработал триггер openScreen -> $targetRoute")

        when (targetRoute) {
          "SignUpScreen", "signUp" -> navigator.push(SignUpScreen)
          "VerifyEmailScreen" -> navigator.push(VerifyEmailScreen)
          else -> {
            // По умолчанию успешный вход переводит на главный адаптивный хаб биллинга
            navigator.replaceAll(MainApartmentScreen(ContentType.METER, NavigationType.BOTTOM_NAVIGATION))
          }
        }
      }
    )
  }
}


/**
 * [SignUpScreen] — Экран регистрации нового жильца г. Южный.
 */
object SignUpScreen : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = koinInject<SignUpScreenModel>() // Безопасный КМР-инжект Koin
    println("[$className.SignUpScreen]: Отрисовка окна регистрации")

    com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen(
      viewModel = screenModel,
      onNavigateBack = {
        println("[$className.SignUpScreen]: Возврат на вход")
        navigator.pop()
      },
      onRegistrationSuccess = {
        println("[$className.SignUpScreen]: Регистрация успешна, переход на верификацию Email")
        navigator.push(VerifyEmailScreen)
      }
    )
  }
}

/**
 * [VerifyEmailScreen] — Экран подтверждения учетной записи через Email.
 */
object VerifyEmailScreen : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = koinInject<SignUpScreenModel>()
    println("[$className.VerifyEmailScreen]: Отрисовка окна подтверждения почты")

    com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen(
      viewModel = screenModel,
      onRestartApp = {
        println("[$className.VerifyEmailScreen]: Сброс стека и перезапуск на вход")
        navigator.replaceAll(SignInScreen) // Аналог popUpTo(0) { inclusive = true }
      },
      onBackClick = {
        navigator.pop()
      }
    )
  }
}

/** [AddApartmentScreen] — Окно привязки квартиры БТИ по секретному коду. */
object AddApartmentScreen : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Экран добавления квартиры") } }
}

/** [SendImageScreenDest] — Экран предосмотра и отправки фото счетчика в Gemini AI. */
object SendImageScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Предосмотр ИИ-фотографии счетчика") } }
}

/** [CameraScreenDest] — Кроссплатформенный видоискатель камеры. */
object CameraScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Экран камеры") } }
}

/** [ChatScreenDest] — Главный чат обсуждений и заявок ЖЭК / ОСМД г. Южный. */
object ChatScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Лента обсуждений ОСМД") } }
}

/** [ProfileScreenDest] — Окно персональных данных профиля. */
object ProfileScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Профиль пользователя") } }
}

/** [BtiScreenDest] — Панель характеристик жилья БТИ. */
object BtiScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Данные БТИ") } }
}

/** [FamilyScreenDest] — Список зарегистрированных мешканців квартиры. */
object FamilyScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Состав семьи") } }
}

// ====================================================================
// --- ДИНАМИЧЕСКИЕ КЛАССЫ ЭКРАНОВ VOYAGER (ПЕРЕДАЧА АРГУМЕНТОВ) ---
// ====================================================================

data class InfoApartmentScreenDest(
  val addressId: Long = 0L
) : Screen {
  @Composable
  override fun Content() {
    println("[$className.InfoApartmentScreenDest]: Open для ID: $addressId")
  }
}

data class ImageDetailScreenDest(
  val imageUrl: String
) : Screen {
  @Composable override fun Content() { println("[$className.ImageDetailScreenDest]: Open для: $imageUrl") }
}

data class WebViewScreenDest(
  val link: String
) : Screen {
  @Composable
  override fun Content() {
    val formattedLink = remember(link) { link.replace("*", "/") }
    println("[$className.WebViewScreenDest]: Open Xpay Link: $formattedLink")
  }
}

object YkisNavConstants {
  const val APARTMENT_SCREEN = "ApartmentScreen"
  const val WATER_SCREEN = "WaterScreen"
  const val SERVICE_DETAIL_SCREEN = "ServiceDetailScreen"
  const val ADDRESS_ID = "addressId"
  const val ADDRESS_DEFAULT_ID = "0"
  const val HOUSE_ID = "houseId"
  const val HOUSE_DEFAULT_ID = "0"
  const val SERVICE = "service"
  const val SERVICE_DEFAULT = "1"
  const val SERVICE_NAME = "serviceName"
  const val SERVICE_DEFAULT_NAME = "ОСББ"
  const val ADDRESS = "address"
  const val ADDRESS_DEFAULT = "адреса"
}
