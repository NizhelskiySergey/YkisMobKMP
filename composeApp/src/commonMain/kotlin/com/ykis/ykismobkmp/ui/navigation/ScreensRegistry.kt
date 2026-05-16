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
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen

private const val className = "ScreensRegistry"

// ====================================================================
// --- МОНОЛИТНЫЕ КМР ЭКРАНЫ VOYAGER (ГРАФИКА + НАВИГАЦИЯ В СТАТИКЕ) ---
// ====================================================================

/**
 * [SignUpScreen] — Экран регистрации нового жильца г. Южный.
 */
// ВНУТРИ ФАЙЛА ScreensRegistry.kt:

/**
 * [SignUpScreen] — Маршрутизатор экрана регистрации нового жильца г. Южный.
 * ИСПРАВЛЕНО: Ложный вызов функции с параметрами удален. Нативно перенаправляет отрисовку на оригинальный КМР-класс.
 */
object SignUpScreen : Screen {
  @Composable
  override fun Content() {
    println("[$className.SignUpScreen]: Маршрутизатор передає управління холсту реєстрації")

    // РЕШЕНИЕ: Напрямую вызываем метод Content() твоего реального графического класса SignUpScreen!
    SignUpScreen().Content()
  }
}

/**
 * [VerifyEmailScreen] — Маршрутизатор экрана подтверждения учетной записи через Email.
 * ИСПРАВЛЕНО: Ложный вызов удален, вызов родительского синглтона приведен к виду SignInScreen без скобок ().
 */
object VerifyEmailScreen : Screen {
  @Composable
  override fun Content() {
    println("[$className.VerifyEmailScreen]: Маршрутизатор передає управління холсту верифікації пошти")

    // РЕШЕНИЕ: Напрямую вызываем метод Content() твоего реального графического класса VerifyEmailScreen!
    VerifyEmailScreen().Content()
  }
}


/** [AddApartmentScreen] — Окно привязки квартиры БТИ по секретному коду. */
object AddApartmentScreen : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Экран добавления квартиры БТИ") } }
}

/** [SendImageScreenDest] — Экран предосмотра и отправки фото счетчика в Gemini AI. */
object SendImageScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Предосмотр ИИ-фотографии счетчика") } }
}

/** [CameraScreenDest] — Кроссплатформенный видоискатель камеры. */
object CameraScreenDest : Screen {
  @Composable override fun Content() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Экран камеры") } }
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

/**
 * [ChatScreenDest] — Универсальный кроссплатформенный экран чат-комнаты ЮКИС.
 * ИСПРАВЛЕНО: Изменен с object на data class. Теперь он нативно принимает и обрабатывает токен пуша chatId!
 */
data class ChatScreenDest(
  val chatId: String? = null
) : Screen {
  @Composable
  override fun Content() {
    println("[$className.ChatScreenDest]: Отрисовка чата для токена: $chatId")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Лента обговорень чату для ID: $chatId")
    }
  }
}

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
