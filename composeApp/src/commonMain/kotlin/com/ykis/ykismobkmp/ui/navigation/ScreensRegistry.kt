package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.InfoApartmentScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.MainServiceScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreen
import org.koin.compose.koinInject

private const val className = "ScreensRegistry"
object SignUpScreen : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.SignUpScreen]: Маршрутизатор передає управління холсту реєстрації")
    com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen.Content()
  }
}
object VerifyEmailScreen : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.VerifyEmailScreen]: Маршрутизатор передає управління холсту верифікації пошти")
    com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen.Content()
  }
}
object AddApartmentScreen : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.AddApartmentScreen]: Маршрутизатор передає управління живому холсту прив'язки квартири")
    AddApartmentScreen(
      onDrawerClicked = { /* Открытие боковой панели шторки */ },
      closeContentDetail = { /* Закрытие подмодуля привязки */ }
    )
  }
}
/** [SendImageScreenDest] — Экран предосмотра и отправки фото счетчика в Gemini AI. */
object SendImageScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Перегляд ІИ-фотографії лічильника") }
  }
}
/**
 * [CameraScreenDest] — Кроссплатформенный видоискатель камеры.
 * */
object CameraScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Екран камери") }
  }
}
/** [ProfileScreenDest] — Окно персональных данных профиля. */
object ProfileScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Профіль користувача") }
  }
}
/**
 * [BtiScreenDest] — Панель характеристик жилья БТИ.
 * */
object BtiScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Дані БТІ") }
  }
}
/**
 * [FamilyScreenDest] — Список зарегистрированных мешканців квартиры.
 * */
object FamilyScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Склад сім'ї") }
  }
}
/**
  [ChatScreenDest] — Универсальный кроссплатформенный экран чат-комнаты ЮКИС.
 **/
data class ChatScreenDest(
  val chatId: String? = null
) : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.ChatScreenDest]: Отрисовка чату для токена: $chatId")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Стрічка обговорень чату для ID: $chatId")
    }
  }
}

data class InfoApartmentScreenDest(
  val addressId: Long = 0L
) : Screen {

  // Явное переопределение KMP-ключа Voyager для защиты от крашей при повороте планшета
  override val key: cafe.adriel.voyager.core.screen.ScreenKey
    get() = "InfoApartmentScreenDest_${addressId}"

  @Composable
  override fun Content() {
    val classNameRegistry = "ScreensRegistry"
    println("[YkisLogKMP.$classNameRegistry.InfoApartmentScreenDest]: Маршрутизатор передає управління ЖИВОМУ холсту БТІ для о/р: $addressId")

    // ДОБАВЛЕНО НАМЕРТВО: Достаем контекст верхнего навигатора Voyager
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow

    InfoApartmentScreen(
      onDrawerClicked = {
        println("[YkisLogKMP.$classNameRegistry.InfoApartmentScreenDest]: Клік по бургер-кнопці на екрані БТІ.")
      }
    )
  }
}
data class ImageDetailScreenDest(
  val imageUrl: String
) : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.ImageDetailScreenDest]: Open для: $imageUrl")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Деталізація зображення: $imageUrl")
    }
  }
}
data class WebViewScreenDest(
  val link: String
) : Screen {
  @Composable
  override fun Content() {
    val formattedLink = remember(link) { link.replace("*", "/") }
    println("[YkisLogKMP.$className.WebViewScreenDest]: Open Xpay Link: $formattedLink")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Платіжний шлюз Xpay: $formattedLink")
    }
  }
}
object MainMeterScreenDest : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.MainMeterScreenDest]: Маршрутизатор передає управління головному екрану лічильників")
    // Вызов реального холста из вашего feature-модуля приборов учета
    MainMeterScreen()
  }
}



data class MainServiceScreenDest(
  val addressId: Long = 0L
) : Screen {

  // ИСПРАВЛЕНО НАМЕРТВО: Явное переопределение KMP-ключа Voyager для защиты от крашей при повороте планшета
  override val key: cafe.adriel.voyager.core.screen.ScreenKey
    get() = "MainServiceScreenDest_${addressId}"

  @Composable
  override fun Content() {
    val classNameRegistry = "ScreensRegistry"
    println("[YkisLogKMP.$classNameRegistry.MainServiceScreenDest]: Маршрутизатор передає управління ЖИВОМУ фінансовому хабу для о/р: $addressId")

    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by apartmentScreenModel.apartmentUiState.collectAsState()

    // Вызываем оригинальный полноценный экран коммунальных начислений ЮКІС
    MainServiceScreen(
      baseUIState = baseUIState,
      navigationType = LocalNavigationType.current, // Берем тип навигации из стабильного CompositionLocal
      onDrawerClick = {
        println("[YkisLogKMP.$classNameRegistry.MainServiceScreenDest]: Клік по бургер-кнопці на екрані фінансів.")
      }
    )
  }
}

object SettingsScreenDest : cafe.adriel.voyager.core.screen.Screen {
  @Composable
  override fun Content() {
    val classNameRegistry = "ScreensRegistry"
    println("[YkisLogKMP.$classNameRegistry.SettingsScreenDest]: Маршрутизатор передає управління холсту системних налаштувань")

    // Извлекаем навигатор Voyager из текущего контекста KMP-холста
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow

    // Вызываем оригинальный компонент интерфейса настроек
    SettingsScreen(
      onDrawerClick = {
        // Так как в чистом Voyager шторкой Drawer управляет родительский навигатор,
        // при клике на бургер-кнопку внутри настроек мы можем нативно возвращать пользователя назад
        println("[YkisLogKMP.$classNameRegistry.SettingsScreenDest]: Запрос закрытия экрана настроек, возврат по стеку.")
        navigator.pop()
      }
    )
  }
}

